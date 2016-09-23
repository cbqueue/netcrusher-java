package org.netcrusher.datagram;

import org.netcrusher.common.NioReactor;
import org.netcrusher.common.NioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class DatagramInner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatagramInner.class);

    private static final int PENDING_LIMIT = 64 * 1024;

    private final NioReactor reactor;

    private final DatagramCrusherSocketOptions socketOptions;

    private final InetSocketAddress localAddress;

    private final InetSocketAddress remoteAddress;

    private final DatagramChannel channel;

    private final SelectionKey selectionKey;

    private final ByteBuffer bb;

    private final Map<InetSocketAddress, DatagramOuter> outers;

    private final Queue<DatagramMessage> incoming;

    private final long maxIdleDurationMs;

    private volatile boolean frozen;

    public DatagramInner(NioReactor reactor,
                         InetSocketAddress localAddress,
                         InetSocketAddress remoteAddress,
                         DatagramCrusherSocketOptions socketOptions,
                         long maxIdleDurationMs) throws IOException {
        this.reactor = reactor;
        this.socketOptions = socketOptions;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.outers = new HashMap<>(32);
        this.incoming = new LinkedList<>();
        this.maxIdleDurationMs = maxIdleDurationMs;
        this.frozen = true;

        this.channel = DatagramChannel.open(socketOptions.getProtocolFamily());
        this.channel.configureBlocking(true);
        socketOptions.setupSocketChannel(this.channel);
        this.channel.bind(localAddress);
        this.channel.configureBlocking(false);

        this.bb = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());

        this.selectionKey = reactor.register(channel, 0, this::callback);

        LOGGER.debug("Inner on <{}> is started", localAddress);
    }

    protected synchronized void unfreeze() throws IOException {
        if (frozen) {
            reactor.executeReactorOp(() -> {
                int ops = incoming.size() > 0 ?
                    SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ;
                selectionKey.interestOps(ops);

                outers.values().forEach(DatagramOuter::unfreeze);

                return null;
            });

            frozen = false;
        }
    }

    protected synchronized void freeze() throws IOException {
        if (!frozen) {
            reactor.executeReactorOp(() -> {
                if (selectionKey.isValid()) {
                    selectionKey.interestOps(0);
                }

                outers.values().forEach(DatagramOuter::freeze);

                return null;
            });

            frozen = true;
        }
    }

    protected boolean isFrozen() {
        return frozen;
    }

    protected synchronized void close() throws IOException {
        freeze();

        outers.values().forEach(DatagramOuter::close);
        outers.clear();

        NioUtils.closeChannel(channel);

        LOGGER.debug("Inner on <{}> is closed", localAddress);
    }

    private void callback(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isReadable()) {
            handleReadable(selectionKey);
        }

        if (selectionKey.isWritable()) {
            handleWritable(selectionKey);
        }
    }

    private void handleWritable(SelectionKey selectionKey) throws IOException {
        DatagramChannel channel = (DatagramChannel) selectionKey.channel();

        DatagramMessage dm = incoming.peek();
        if (dm != null) {
            int written = channel.send(dm.getBuffer(), dm.getAddress());
            LOGGER.trace("Send {} bytes from inner <{}>", written, dm.getAddress());

            if (!dm.getBuffer().hasRemaining()) {
                incoming.poll();
            } else {
                LOGGER.warn("Datagram is splitted");
            }
        }

        if (incoming.isEmpty()) {
            NioUtils.clearInterestOps(selectionKey, SelectionKey.OP_WRITE);
        }
    }

    private void handleReadable(SelectionKey selectionKey) throws IOException {
        DatagramChannel channel = (DatagramChannel) selectionKey.channel();

        bb.clear();
        InetSocketAddress address = (InetSocketAddress) channel.receive(bb);

        if (address != null) {
            DatagramOuter outer = requestOuter(address);

            ByteBuffer data = ByteBuffer.allocate(bb.limit());
            bb.flip();
            data.put(bb);
            data.flip();

            outer.enqueue(data);

            LOGGER.trace("Received {} bytes from inner <{}>", data.limit(), address);
        }
    }

    private DatagramOuter requestOuter(InetSocketAddress address) throws IOException {
        DatagramOuter outer = outers.get(address);

        if (outer == null) {
            if (maxIdleDurationMs > 0) {
                clearOuters(maxIdleDurationMs);
            }

            outer = new DatagramOuter(this, address, remoteAddress, socketOptions);
            outer.unfreeze();

            outers.put(address, outer);
        }

        return outer;
    }

    private void clearOuters(long maxIdleDurationMs) {
        int countBefore = outers.size();
        if (countBefore > 0) {
            Iterator<DatagramOuter> outerIterator = outers.values().iterator();

            while (outerIterator.hasNext()) {
                DatagramOuter outer = outerIterator.next();

                if (outer.getIdleDurationMs() > maxIdleDurationMs) {
                    outer.close();
                    outerIterator.remove();
                }
            }

            int countAfter = outers.size();
            LOGGER.debug("Outer connections are cleared ({} -> {})", countBefore, countAfter);
        }
    }

    protected void enqueue(DatagramMessage message) {
        if (incoming.size() < PENDING_LIMIT) {
            incoming.add(message);
            NioUtils.setupInterestOps(selectionKey, SelectionKey.OP_WRITE);
        } else {
            LOGGER.debug("Pending limit is exceeded. Packet is dropped");
        }
    }

    protected NioReactor getReactor() {
        return reactor;
    }

}
