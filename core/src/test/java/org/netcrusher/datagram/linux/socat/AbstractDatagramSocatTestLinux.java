package org.netcrusher.datagram.linux.socat;

import org.junit.jupiter.api.Assertions;
import org.netcrusher.test.AbstractTestLinux;
import org.netcrusher.test.process.ProcessResult;
import org.netcrusher.test.process.ProcessWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public abstract class AbstractDatagramSocatTestLinux extends AbstractTestLinux {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatagramSocatTestLinux.class);

    protected static final int DEFAULT_BYTES = 8 * 1024 * 1024;

    protected static final int DEFAULT_THROUGHPUT_KBPERSEC = 500;

    protected static final int PORT_DIRECT = 50100;

    protected static final int PORT_PROXY = 50101;

    protected static final int SOCAT_TIMEOUT_SEC = 5;

    /* IP4 */

    protected static final String SOCAT4 =
        String.format("socat -T%d -4 -d", SOCAT_TIMEOUT_SEC);

    protected static final String SOCAT4_PROCESSOR =
        SOCAT4 + " - udp4-sendto:127.0.0.1:50100,ignoreeof";

    protected static final String SOCAT4_REFLECTOR_DIRECT =
        SOCAT4 + " -b 16384 PIPE udp4-listen:50100,bind=127.0.0.1,reuseaddr";

    protected static final String SOCAT4_REFLECTOR_PROXIED =
        SOCAT4 + " -b 16384 PIPE udp4-listen:50101,bind=127.0.0.1,reuseaddr";

    protected static final String SOCAT4_PRODUCER =
        SOCAT4 + " - udp4-sendto:127.0.0.1:50100";

    protected static final String SOCAT4_CONSUMER_DIRECT =
        SOCAT4 + " - udp4-listen:50100,bind=127.0.0.1,reuseaddr";

    protected static final String SOCAT4_CONSUMER_PROXIED =
        SOCAT4 + " - udp4-listen:50101,bind=127.0.0.1,reuseaddr";

    /* IP6 */

    protected static final String SOCAT6 =
        String.format("socat -T%d -6 -d", SOCAT_TIMEOUT_SEC);

    protected static final String SOCAT6_PROCESSOR =
        SOCAT6 + " - udp6-sendto:[::1]:50100,ignoreeof";

    protected static final String SOCAT6_REFLECTOR_DIRECT =
        SOCAT6 + " -b 16384 PIPE udp6-listen:50100,bind=[::1],reuseaddr";

    protected static final String SOCAT6_REFLECTOR_PROXIED =
        SOCAT6 + " -b 16384 PIPE udp6-listen:50101,bind=[::1],reuseaddr";

    protected static final String SOCAT6_PRODUCER =
        SOCAT6 + " - udp6-sendto:[::1]:50100";

    protected static final String SOCAT6_CONSUMER_DIRECT =
        SOCAT6 + " - udp6-listen:50100,bind=[::1],reuseaddr";

    protected static final String SOCAT6_CONSUMER_PROXIED =
        SOCAT6 + " - udp6-listen:50101,bind=[::1],reuseaddr";

    protected ProcessResult loop(String processorCmd, String reflectorCmd, int bytes, int throughputKbSec) throws Exception {
        ProcessWrapper processor = new ProcessWrapper(Arrays.asList(
            "bash",
            "-o", "pipefail",
            "-c", "openssl rand " + bytes
                + " | tee >(openssl md5 >&2)"
                + " | pv -q -L " + throughputKbSec + "K"
                + " | dd bs=1024"
                + " | " + processorCmd
                + " | dd bs=1024"
                + " | openssl md5 >&2"
        ));

        ProcessWrapper reflector = new ProcessWrapper(Arrays.asList(
            "bash",
            "-o", "pipefail",
            "-c", reflectorCmd
        ));

        Future<ProcessResult> reflectorFuture = reflector.run();
        Future<ProcessResult> processorFuture = processor.run();

        ProcessResult processorResult = processorFuture.get();
        ProcessResult reflectorResult = reflectorFuture.get();

        output(LOGGER, "Processor", processorResult.getOutputText());
        output(LOGGER, "Reflector", reflectorResult.getOutputText());

        Assertions.assertEquals(0, processorResult.getExitCode());
        Assertions.assertEquals(0, reflectorResult.getExitCode());

        Assertions.assertEquals(2, processorResult.getOutput().stream()
            .filter(s -> s.startsWith(String.format("%d bytes", bytes)))
            .count()
        );

        List<String> hashes = extractMd5(processorResult.getOutput())
            .collect(Collectors.toList());

        Assertions.assertEquals(2, hashes.size());
        Assertions.assertEquals(hashes.get(0), hashes.get(1));

        return processorResult;
    }

    protected ProcessResult direct(String producerCmd, String consumerCmd, int bytes, int throughputKbSec) throws Exception {
        ProcessWrapper producer = new ProcessWrapper(Arrays.asList(
            "bash",
            "-o", "pipefail",
            "-c", "openssl rand " + bytes
                + " | tee >(openssl md5 >&2)"
                + " | pv -q -L " + throughputKbSec + "K"
                + " | dd bs=1024"
                + " | " + producerCmd
        ));

        ProcessWrapper consumer = new ProcessWrapper(Arrays.asList(
            "bash",
            "-o", "pipefail",
            "-c", consumerCmd
                + " | dd bs=1024"
                + " | openssl md5 >&2"
        ));

        Future<ProcessResult> consumerFuture = consumer.run();
        Future<ProcessResult> producerFuture = producer.run();

        ProcessResult producerResult = producerFuture.get();
        ProcessResult consumerResult = consumerFuture.get();

        output(LOGGER, "Producer", producerResult.getOutputText());
        output(LOGGER, "Consumer", consumerResult.getOutputText());

        Assertions.assertEquals(0, producerResult.getExitCode());
        Assertions.assertEquals(0, consumerResult.getExitCode());

        Assertions.assertEquals(1, producerResult.getOutput().stream()
            .filter(s -> s.startsWith(String.format("%d bytes", bytes)))
            .count()
        );
        Assertions.assertEquals(1, consumerResult.getOutput().stream()
            .filter(s -> s.startsWith(String.format("%d bytes", bytes)))
            .count()
        );

        String producerHash = extractMd5(producerResult.getOutput())
            .findFirst()
            .orElse("no-producer-hash");
        String consumerHash = extractMd5(consumerResult.getOutput())
            .findFirst()
            .orElse("no-consumer-hash");

        Assertions.assertEquals(producerHash, consumerHash);

        return consumerResult;
    }

}
