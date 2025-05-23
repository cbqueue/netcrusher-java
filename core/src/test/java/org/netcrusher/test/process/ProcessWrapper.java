package org.netcrusher.test.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ProcessWrapper {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProcessWrapper.class);

    private final static long PROCESS_INIT_MS = 1000;

    private final static int DEFAULT_OUTPUT_LINES = 100;

    private final List<String> arguments;

    private final Map<String, String> environments;

    public ProcessWrapper(List<String> arguments, Map<String, String> environments) {
        this.arguments = arguments;
        this.environments = environments;
    }

    public ProcessWrapper(List<String> arguments) {
        this(arguments, Collections.emptyMap());
    }

    public Future<ProcessResult> run() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.environment().putAll(environments);
        builder.redirectErrorStream(true);

        Watcher watcher = new Watcher(builder);
        return watcher.open();
    }

    private static final class Watcher extends Thread {

        private final Process process;

        private final CompletableFuture<ProcessResult> future;

        private Watcher(ProcessBuilder builder) throws IOException {
            this.process = builder.start();

            LOGGER.info("Process {} started: {}", process.hashCode(), builder.command());

            this.future = new CompletableFuture<>() {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    if (process.isAlive() && mayInterruptIfRunning) {
                        process.destroyForcibly();
                        return true;
                    } else {
                        return false;
                    }
                }
            };

            this.setName("Watcher thread");
            this.setDaemon(true);
            this.setPriority(Thread.MIN_PRIORITY);
        }

        private CompletableFuture<ProcessResult> open() {
            start();

            // poor man's guarantee that process started and sockets are opened
            try {
                Thread.sleep(PROCESS_INIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return future;
        }

        @Override
        public void run() {
            List<String> output = new ArrayList<>(DEFAULT_OUTPUT_LINES);

            try {
                try (InputStream is = process.getInputStream();
                     InputStreamReader isr = new InputStreamReader(is);
                     BufferedReader reader = new BufferedReader(isr)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LOGGER.debug("process {} output: {}", process.hashCode(), line);
                        output.add(line);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Dumper exception", e);
                future.completeExceptionally(e);
                return;
            }

            int exitCode;
            try {
                LOGGER.debug("Waiting for process to complete...");
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                LOGGER.error("Unexpected interruption", e);
                future.completeExceptionally(e);
                return;
            }

            future.complete(new ProcessResult(exitCode, output));
        }
    }
}
