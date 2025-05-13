package org.netcrusher.datagram.linux.socat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectDatagramSocat6Test extends AbstractDatagramSocatTestLinux {

    @Test
    void loop() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT6_PROCESSOR, SOCAT6_REFLECTOR_DIRECT, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT6_PRODUCER, SOCAT6_CONSUMER_DIRECT, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

}
