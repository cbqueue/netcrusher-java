package org.netcrusher.datagram.linux.socat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectDatagramSocat4Test extends AbstractDatagramSocatTestLinux {

    @Test
    void loop() {
        Assertions.assertDoesNotThrow(() -> loop(SOCAT4_PROCESSOR, SOCAT4_REFLECTOR_DIRECT, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

    @Test
    void direct() {
        Assertions.assertDoesNotThrow(() -> direct(SOCAT4_PRODUCER, SOCAT4_CONSUMER_DIRECT, DEFAULT_BYTES, DEFAULT_THROUGHPUT_KBPERSEC));
    }

}
