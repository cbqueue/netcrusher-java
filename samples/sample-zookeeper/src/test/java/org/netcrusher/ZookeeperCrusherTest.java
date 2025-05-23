package org.netcrusher;

import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.instance.ZookeeperInstance;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ZookeeperCrusherTest {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ZookeeperCrusherTest.class);

    private static final File TMP_FOLDER = FileUtils.getTempDirectory();

    private static final File WORK_FOLDER = new File(TMP_FOLDER, "ZooKeeperTest");

    private Instance instance1;

    private Instance instance2;

    private Instance instance3;

    private NioReactor reactor;

    private String connection;

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("zookeeper.jmx.log4j.disable", "true");

        if (WORK_FOLDER.exists()) {
            LOGGER.warn("Work folder '{}' exists", WORK_FOLDER);
            FileUtils.deleteDirectory(WORK_FOLDER);
        }

        FileUtils.forceMkdir(WORK_FOLDER);

        reactor = new NioReactor();

        instance1 = new Instance(reactor, 1);
        instance2 = new Instance(reactor, 2);
        instance3 = new Instance(reactor, 3);

        Collection<String> collections = new ArrayList<>();
        for (int i = 1; i <= ZookeeperInstance.CLUSTER_SIZE; i++) {
            int port = ZookeeperInstance.PORT_BASE_CLIENT + ZookeeperInstance.PORT_CRUSHER_BASE + i;
            collections.add("127.0.0.1:" + port);
        }
        connection = Joiner.on(",").join(collections);

        LOGGER.info("==========================================================================================");
        LOGGER.info("Prepared");
        LOGGER.info("==========================================================================================");
    }

    @AfterEach
    public void tearDown() throws Exception {
        LOGGER.info("==========================================================================================");
        LOGGER.info("Shutdowning...");
        LOGGER.info("==========================================================================================");

        IOUtils.closeQuietly(instance1);
        IOUtils.closeQuietly(instance2);
        IOUtils.closeQuietly(instance3);
        IOUtils.closeQuietly(reactor);

        FileUtils.deleteDirectory(WORK_FOLDER);
    }

    @Test
    public void testOneZooIsDown() throws Exception {
        LOGGER.info("zoo3 goes down");
        instance3.clientCrusher.close();
        instance3.electionCrusher.close();
        instance3.leaderCrusher.close();
        Thread.sleep(3000);

        LOGGER.info("writing data");
        byte[] data1 = { 1, 2, 3};
        write(connection, "/my/path", data1);

        LOGGER.info("zoo3 is back again");
        instance3.clientCrusher.open();
        instance3.electionCrusher.open();
        instance3.leaderCrusher.open();
        Thread.sleep(3000);

        LOGGER.info("zoo1 and zoo2 are disconnected from the client (but still available for zoo3)");
        instance1.clientCrusher.close();
        instance2.clientCrusher.close();
        Thread.sleep(3000);

        LOGGER.info("reading data");
        byte[] data2 = read(connection, "/my/path");

        Assertions.assertArrayEquals(data1, data2);
    }

    private void write(String connection, String path, byte[] data) throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(connection, retryPolicy);

	    try (client) {
		    client.start();
		    client.create().creatingParentContainersIfNeeded().forPath(path, data);
	    } finally {
		    LOGGER.info("Closing client");
	    }
    }

    private byte[] read(String connection, String path) throws Exception {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(connection, retryPolicy);

	    try (client) {
		    client.start();
		    return client.getData().forPath(path);
	    } finally {
		    LOGGER.info("Closing client");
	    }
    }

    private static class Instance implements Closeable {

        private final ZookeeperInstance zoo;

        private final TcpCrusher clientCrusher;

        private final TcpCrusher leaderCrusher;

        private final TcpCrusher electionCrusher;

        public Instance(NioReactor reactor, int instance) throws Exception {
            this.clientCrusher = TcpCrusherBuilder.builder()
                    .withReactor(reactor)
                    .withBindAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_CLIENT + ZookeeperInstance.PORT_CRUSHER_BASE + instance)
                    .withConnectAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_CLIENT + instance)
                    .buildAndOpen();

            this.leaderCrusher = TcpCrusherBuilder.builder()
                    .withReactor(reactor)
                    .withBindAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_LEADER + ZookeeperInstance.PORT_CRUSHER_BASE + instance)
                    .withConnectAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_LEADER + instance)
                    .buildAndOpen();

            this.electionCrusher = TcpCrusherBuilder.builder()
                    .withReactor(reactor)
                    .withBindAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_ELECTION + ZookeeperInstance.PORT_CRUSHER_BASE + instance)
                    .withConnectAddress("127.0.0.1", ZookeeperInstance.PORT_BASE_ELECTION + instance)
                    .buildAndOpen();

            this.zoo = new ZookeeperInstance(WORK_FOLDER, true, instance);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(zoo);
            IOUtils.closeQuietly(electionCrusher);
            IOUtils.closeQuietly(leaderCrusher);
            IOUtils.closeQuietly(clientCrusher);
        }
    }

}
