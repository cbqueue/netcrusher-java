package org.netcrusher.core.reactor;

import org.netcrusher.NetCrusherException;
import org.netcrusher.core.nio.SelectionKeyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

public class NioSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(NioSelector.class);

    private static final long THREAD_TERMINATION_TIMEOUT_MS = 5000;

    private static final Comparator<NioSelectorScheduledOp> SCHEDULE_COMPARATOR =
        Comparator.comparingLong(NioSelectorScheduledOp::getScheduledNs);

    private final Thread thread;

    private final Selector selector;

    private final Queue<NioSelectorPostOp<?>> postOperationQueue;

    private final Queue<NioSelectorScheduledOp> scheduledOperationQueue;

    private final long tickMs;

    private volatile boolean open;

    NioSelector(long tickMs) throws IOException {
        if (tickMs <= 0) {
            throw new IllegalArgumentException("Tick period must be positive");
        }

        this.selector = Selector.open();
        this.postOperationQueue = new ConcurrentLinkedQueue<>();
        this.scheduledOperationQueue = new PriorityQueue<>(SCHEDULE_COMPARATOR);

        this.thread = new Thread(this::loop);
        this.thread.setName("NetCrusher selector event loop");
        this.thread.setDaemon(false);
        this.thread.start();

        this.tickMs = tickMs;
        this.open = true;
    }

    synchronized void close() {
        if (open) {
            LOGGER.debug("Selector is closing");
            boolean interrupted = false;

            postOperationQueue.clear();

            wakeup();

            if (thread.isAlive()) {
                thread.interrupt();

                try {
                    thread.join(THREAD_TERMINATION_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }

                if (thread.isAlive()) {
                    LOGGER.error("NetCrusher selector thread is still alive");
                }
            }

            int activeSelectionKeys = selector.keys().size();
            if (activeSelectionKeys > 0) {
                LOGGER.warn("Selector still has {} selection keys. Have you closed all linked crushers before?",
                    activeSelectionKeys);
            }

            try {
                selector.close();
            } catch (IOException e) {
                LOGGER.error("Fail to close selector", e);
            }

            open = false;
            LOGGER.debug("Selector is closed");

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Internal method
    public SelectionKey register(SelectableChannel channel, int options, SelectionKeyCallback callback) {
        return execute(() -> channel.register(selector, options, callback));
    }

    // Internal method
    public int wakeup() {
        // fixes some strange behaviour on Windows: http://stackoverflow.com/a/39657002/827139
        return execute(selector::selectNow);
    }

    // Internal method
    public <T> T execute(Callable<T> callable) throws NetCrusherException {
        if (open) {
            if (Thread.currentThread().equals(thread)) {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw new NetCrusherException("Fail to execute selector op", e);
                }
            } else {
                NioSelectorPostOp<T> postOperation = new NioSelectorPostOp<>(callable);
                postOperationQueue.add(postOperation);

                selector.wakeup();

                try {
                    return postOperation.await();
                } catch (InterruptedException e) {
                    throw new NetCrusherException("Reactor operation was interrupted", e);
                } catch (ExecutionException e) {
                    throw new NetCrusherException("Selector operation has failed", e);
                }
            }
        } else {
            throw new IllegalStateException("Selector is closed");
        }
    }

    // Internal method
    public void schedule(Runnable runnable, long delayNs) {
        if (tickMs == 0) {
            throw new IllegalStateException("Tick value should be set on selector");
        }
        if (!Thread.currentThread().equals(thread)) {
            throw new IllegalStateException("Scheduling only should be made from selector's thread");
        }

        long nowNs = System.nanoTime();

        scheduledOperationQueue.add(new NioSelectorScheduledOp(nowNs + delayNs, runnable));
    }

    private void loop() {
        LOGGER.debug("Selector event loop started");

        while (!Thread.currentThread().isInterrupted()) {
            // block on getting selection keys ready to act
            int count;
            try {
                count = selector.select(tickMs);
            } catch (ClosedSelectorException e) {
                break;
            } catch (Exception e) {
                LOGGER.error("Error on select()", e);
                break;
            }

            // execute all selection key callbacks
            if (count > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();

                    if (selectionKey.isValid()) {
                        try {
                            SelectionKeyCallback callback = (SelectionKeyCallback) selectionKey.attachment();
                            callback.execute(selectionKey);
                        } catch (Exception e) {
                            LOGGER.error("Error while executing selection key callback", e);
                        }
                    } else {
                        LOGGER.debug("Selection key is invalid: {}", selectionKey);
                    }

                    keyIterator.remove();
                }
            }

            runScheduledOperations();

            runPostOperations();
        }

        LOGGER.debug("Selector event loop has finished");
    }

    private void runScheduledOperations() {
        while (true) {
            NioSelectorScheduledOp scheduledOperation = scheduledOperationQueue.peek();
            if (scheduledOperation != null && scheduledOperation.isReady()) {
                scheduledOperation = scheduledOperationQueue.poll();
                if (scheduledOperation != null) {
                    scheduledOperation.run();
                }
            } else {
                break;
            }
        }
    }

    private void runPostOperations() {
        while (true) {
            NioSelectorPostOp<?> postOperation = postOperationQueue.poll();
            if (postOperation != null) {
                postOperation.run();
            } else {
                break;
            }
        }
    }

}
