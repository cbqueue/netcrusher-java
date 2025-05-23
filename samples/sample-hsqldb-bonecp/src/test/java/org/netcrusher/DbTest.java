package org.netcrusher;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.hsqldb.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.concurrent.TimeUnit;

public class DbTest {

    private static final int DB_PORT = 10777;

    private static final int CRUSHER_PORT = 10778;

    private static final String SQL_CHECK = "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";

    private Server hsqlServer;

    private BoneCP connectionPool;

    private NioReactor reactor;

    private TcpCrusher crusher;

    @BeforeEach
    public void setUp() throws Exception {
        reactor = new NioReactor();

        crusher = TcpCrusherBuilder.builder()
                .withReactor(reactor)
                .withBindAddress("127.0.0.1", CRUSHER_PORT)
                .withConnectAddress("127.0.0.1", DB_PORT)
                .buildAndOpen();

        hsqlServer = new Server();
        hsqlServer.setAddress("127.0.0.1");
        hsqlServer.setPort(DB_PORT);
        hsqlServer.setDaemon(true);
        hsqlServer.setErrWriter(new PrintWriter(System.err));
        hsqlServer.setLogWriter(new PrintWriter(System.out));
        hsqlServer.setNoSystemExit(true);
        hsqlServer.setDatabasePath(0, "mem:testdb");
        hsqlServer.setDatabaseName(0, "testdb");
        hsqlServer.start();

        Class.forName("org.hsqldb.jdbc.JDBCDriver");

        connectionPool = new BoneCP(getBoneCPConfig());
    }

    private static BoneCPConfig getBoneCPConfig() {
        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(String.format("jdbc:hsqldb:hsql://127.0.0.1:%d/testdb", CRUSHER_PORT));
        config.setUsername("sa");
        config.setPassword("");
        config.setInitSQL(SQL_CHECK);
        config.setConnectionTestStatement(SQL_CHECK);
        config.setAcquireIncrement(1);
        config.setAcquireRetryAttempts(1);
        config.setAcquireRetryDelayInMs(1000);
        config.setConnectionTimeoutInMs(1000);
        config.setQueryExecuteTimeLimitInMs(1000);
        config.setDefaultAutoCommit(false);
        config.setDefaultReadOnly(true);
        config.setDefaultTransactionIsolation("NONE");
        config.setPartitionCount(1);
        config.setMinConnectionsPerPartition(1);
        config.setMaxConnectionsPerPartition(1);
        config.setLazyInit(true);
        config.setDetectUnclosedStatements(true);
        return config;
    }

    @AfterEach
    public void tearDown() {
        connectionPool.close();
        hsqlServer.stop();
        crusher.close();
        reactor.close();
    }

    @Test
    public void testDisconnect() throws Exception {
        // create a connection
        Connection connection = connectionPool.getConnection();

        Thread.sleep(1000);
        Assertions.assertEquals(1, crusher.getClientAddresses().size());

        // query some data
        connection.createStatement().executeQuery(SQL_CHECK);

        // check the pool has only one connection
        try {
            connectionPool.getConnection();
            Assertions.fail("Exception is expected");
        } catch (SQLException e) {
            // exception is expected;
        }

        // disconnect
        crusher.reopen();

        // the query should fail
        try {
            connection.createStatement().executeQuery(SQL_CHECK);
            Assertions.fail("Exception is expected");
        } catch (SQLTransientConnectionException e) {
            // exception is expected;
        }

        // close the connection as it is useless
        try {
            connection.close();
        } catch (SQLException e) {
            // possible exception when the dead connection is being closed
        }

        // get a new fresh one from the pool
        connection = connectionPool.getConnection();
        Assertions.assertEquals(1, crusher.getClientAddresses().size());

        // query some data
        connection.createStatement().executeQuery(SQL_CHECK);

        // close
        connection.close();
    }

    @Test
    public void testFreeze() throws Exception {
        // create a connection
        Connection connection = connectionPool.getConnection();

        Thread.sleep(1000);
        Assertions.assertEquals(1, crusher.getClientAddresses().size());

        // query some data
        connection.createStatement().executeQuery(SQL_CHECK);

        // disconnect
        crusher.freezeAllPairs();

        reactor.getScheduler().schedule(() -> {
            crusher.unfreezeAllPairs();
            return true;
        }, 3000, TimeUnit.MILLISECONDS);

        // the query should fail
        connection.createStatement().executeQuery(SQL_CHECK);

        connection.close();
    }
}


