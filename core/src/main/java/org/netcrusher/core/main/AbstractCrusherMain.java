package org.netcrusher.core.main;

import org.netcrusher.NetCrusher;
import org.netcrusher.core.nio.NioUtils;
import org.netcrusher.core.reactor.NioReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

@SuppressWarnings("PMD.SystemPrintln")
public abstract class AbstractCrusherMain<T extends NetCrusher> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrusherMain.class);

    private static final Charset PLATFORM_CHARSET = Charset.defaultCharset();

    private static final int ERR_EXIT_CODE_NO_ARGUMENT = 1;
    private static final int ERR_EXIT_CODE_INVALID_ADDRESS = 2;
    private static final int ERR_EXIT_INITIALIZATION = 3;

    private static final String CMD_OPEN = "OPEN";
    private static final String CMD_CLOSE = "CLOSE";
    private static final String CMD_REOPEN = "REOPEN";
    private static final String CMD_FREEZE = "FREEZE";
    private static final String CMD_UNFREEZE = "UNFREEZE";
    private static final String CMD_STATUS = "STATUS";
    private static final String CMD_HELP = "HELP";
    private static final String CMD_QUIT = "QUIT";

    private static final String CMD_CLIENT_CLOSE = "CLIENT-CLOSE";
    private static final String CMD_CLIENT_STATUS = "CLIENT-STATUS";

    protected int run(String[] arguments) {
        if (arguments == null || arguments.length != 2) {
            printUsage();
            return ERR_EXIT_CODE_NO_ARGUMENT;
        }

        final InetSocketAddress bindAddress;
        try {
            bindAddress = NioUtils.parseInetSocketAddress(arguments[0]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Fail to parse listening address: {}", arguments[0], e);
            return ERR_EXIT_CODE_INVALID_ADDRESS;
        }

        final InetSocketAddress connectAddress;
        try {
            connectAddress = NioUtils.parseInetSocketAddress(arguments[1]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Fail to parse connect address: {}", arguments[1], e);
            return ERR_EXIT_CODE_INVALID_ADDRESS;
        }

        final long tickMs = Integer.getInteger("crusher.tick", 10);
        LOGGER.debug("Reactor tick = {} ms", tickMs);

        return run(bindAddress, connectAddress, tickMs);
    }

    @SuppressWarnings("PMD.CloseResource")
    protected int run(InetSocketAddress bindAddress, InetSocketAddress connectAddress, long tickMs) {
        final NioReactor reactor;
        try {
            reactor = new NioReactor(tickMs);
        } catch (Exception e) {
            LOGGER.error("Fail to create reactor", e);
            return ERR_EXIT_INITIALIZATION;
        }

        final T crusher;
        try {
            crusher = create(reactor, bindAddress, connectAddress);
        } catch (Exception e) {
            LOGGER.error("Fail to create crusher", e);
            reactor.close();
            return ERR_EXIT_INITIALIZATION;
        }

        Runnable closer = () -> {
            try {
                if (crusher.isOpen()) {
                    close(crusher);
                }
            } catch (Exception e) {
                LOGGER.error("Fail to close the crusher", e);
            }

            try {
                if (reactor.isOpen()) {
                    reactor.close();
                }
            } catch (Exception e) {
                LOGGER.error("Fail to close the reactor", e);
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(closer));

        final String crusherVersion = getClass().getPackage().getImplementationVersion();
        if (crusherVersion != null && !crusherVersion.isEmpty()) {
            System.out.printf("# Version: %s%n", crusherVersion);
        }

        final String javaVersion = System.getProperty("java.version");
        if (javaVersion != null && !javaVersion.isEmpty()) {
            System.out.printf("# Java: %s%n", javaVersion);
        }

        System.out.println("# Print `HELP` for the list of the commands");
        repl(crusher);

        closer.run();

        LOGGER.info("Exiting..");

        return 0;
    }

    protected void repl(T crusher) {
        try {
            try (Scanner scanner = new Scanner(System.in, PLATFORM_CHARSET)) {
                while (true) {
                    System.out.printf("# enter the command in the next line (crusher is %s)%n",
                        crusher.isOpen() ? "OPEN" : "CLOSED");

                    String line = scanner.nextLine();

                    if (line == null || CMD_QUIT.equals(line)) {
                        break;
                    } else if (line.isEmpty()) {
                        LOGGER.warn("Command is empty");
                    } else {
                        try {
                            command(crusher, line);
                        } catch (Exception e) {
                            LOGGER.error("Command failed: '{}'", line, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("REPL error", e);
        }
    }

    protected void command(T crusher, String command) {
        if (command.equals(CMD_OPEN)) {
            open(crusher);
        } else if (command.equals(CMD_CLOSE)) {
            close(crusher);
        } else if (command.equals(CMD_REOPEN)) {
            reopen(crusher);
        } else if (command.equals(CMD_STATUS)) {
            status(crusher);
        } else if (command.equals(CMD_FREEZE)) {
            freeze(crusher);
        } else if (command.equals(CMD_UNFREEZE)) {
            unfreeze(crusher);
        } else if (command.equals(CMD_HELP)) {
            printHelp();
        } else if (command.startsWith(CMD_CLIENT_CLOSE)) {
            closeClient(crusher, command);
        } else if (command.startsWith(CMD_CLIENT_STATUS)) {
            statusClient(crusher, command);
        } else {
            LOGGER.warn("Command is unknown: '{}'", command);
        }
    }

    protected void open(T crusher) {
        if (crusher.isOpen()) {
            LOGGER.warn("Crusher is already open");
        } else {
            crusher.open();
            LOGGER.info("Crusher is open");
        }
    }

    protected void close(T crusher) {
        if (crusher.isOpen()) {
            crusher.close();
            LOGGER.info("Crusher is closed");
        } else {
            LOGGER.warn("Crusher is not open");
        }
    }

    protected void reopen(T crusher) {
        if (crusher.isOpen()) {
            crusher.reopen();
            LOGGER.info("Crusher is reopen");
        } else {
            LOGGER.warn("Crusher is not open");
        }
    }

    protected void freeze(T crusher) {
        if (crusher.isOpen()) {
            crusher.freeze();
            LOGGER.info("Crusher is frozen");
        } else {
            LOGGER.warn("Crusher is not open");
        }
    }

    protected void unfreeze(T crusher) {
        if (crusher.isOpen()) {
            crusher.unfreeze();
            LOGGER.info("Crusher is unfrozen");
        } else {
            LOGGER.warn("Crusher is not open");
        }
    }

    protected void status(T crusher) {
        LOGGER.info(
            "{} crusher for <{}>-<{}>", crusher.getClass().getSimpleName(),
            crusher.getBindAddress(), crusher.getConnectAddress()
        );

        if (crusher.isOpen()) {
            LOGGER.info("Crusher is open");

            if (crusher.isFrozen()) {
                LOGGER.info("Crusher is frozen");
            }

            LOGGER.info("Total number of registered clients: {}", crusher.getClientTotalCount());
            LOGGER.info("Total number of active clients: {}", crusher.getClientAddresses().size());

            for (InetSocketAddress clientAddress : crusher.getClientAddresses()) {
                statusClient(crusher, clientAddress);
            }
        } else {
            LOGGER.info("Crusher is closed");
        }
    }

    protected void closeClient(T crusher, String command) {
        InetSocketAddress address = parseAddress(command);
        boolean closed = crusher.closeClient(address);
        if (closed) {
            LOGGER.info("Client for <{}> is closed", address);
        } else {
            LOGGER.warn("Client for <{}> is not found", address);
        }
    }

    protected void statusClient(T crusher, String command) {
        InetSocketAddress address = parseAddress(command);
        statusClient(crusher, address);
    }

    protected void statusClient(T crusher, InetSocketAddress address)  {
        LOGGER.info("Client address <{}>", address);
    }

    protected void printUsage() {
        LOGGER.warn("Execution: {} <bind-socket-address:port> <connect-socket-address:port>",
            this.getClass().getSimpleName());
    }

    protected void printHelp() {
        LOGGER.info("Commands:");
        LOGGER.info("\t" + CMD_OPEN + "     - opens the crusher");
        LOGGER.info("\t" + CMD_CLOSE + "    - closes the crusher (sockets will be closed)");
        LOGGER.info("\t" + CMD_REOPEN + "   - closes and opens the crusher again");
        LOGGER.info("\t" + CMD_FREEZE + "   - freezes the crusher (socket are open but data is not transferred)");
        LOGGER.info("\t" + CMD_UNFREEZE + " - unfreezes the crusher");
        LOGGER.info("\t" + CMD_STATUS + "   - prints the status of the connection");
        LOGGER.info("\t" + CMD_HELP + "     - prints this help");
        LOGGER.info("\t" + CMD_QUIT + "     - quits the program");

        LOGGER.info("Commands for clients:");
        LOGGER.info("\t" + CMD_CLIENT_CLOSE + " <addr>    - closes the client");
        LOGGER.info("\t" + CMD_CLIENT_STATUS + " <addr>   - prints status of the client");
    }

    protected static InetSocketAddress parseAddress(String command) {
        final String[] items = command.split(" ", 2);
        if (items.length == 2) {
            return NioUtils.parseInetSocketAddress(items[1]);
        } else {
            throw new IllegalArgumentException("Fail to parse address from command: " + command);
        }
    }

    protected abstract T create(NioReactor reactor,
        InetSocketAddress bindAddress, InetSocketAddress connectAddress);

    protected static void withLongProperty(String name, LongConsumer consumer) {
        final String text = System.getProperty(name);
        if (text != null && !text.isEmpty()) {
            try {
                consumer.accept(Long.decode(text));
                LOGGER.info("Property '{}={}' is loaded", name, text);
            } catch (NumberFormatException e) {
                LOGGER.error("Fail to load long property {}={}", name, text);
            }
        }
    }

    protected static void withIntProperty(String name, IntConsumer consumer) {
        final String text = System.getProperty(name);
        if (text != null && !text.isEmpty()) {
            try {
                consumer.accept(Integer.decode(text));
                LOGGER.info("Property '{}={}' is loaded", name, text);
            } catch (NumberFormatException e) {
                LOGGER.error("Fail to load integer property {}={}", name, text);
            }
        }
    }

    protected static void withBoolProperty(String name, Consumer<Boolean> consumer) {
        final String text = System.getProperty(name);
        if (text != null && !text.isEmpty()) {
            consumer.accept(Boolean.parseBoolean(text));
            LOGGER.info("Property '{}={}' is loaded", name, text);
        }
    }

    protected static void withStrProperty(String name, Consumer<String> consumer) {
        final String text = System.getProperty(name);
        if (text != null && !text.isEmpty()) {
            consumer.accept(text);
            LOGGER.info("Property '{}={}' is loaded", name, text);
        }
    }

}

