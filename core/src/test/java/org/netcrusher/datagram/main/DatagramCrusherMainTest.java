package org.netcrusher.datagram.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class DatagramCrusherMainTest {

    @Test
    void test() {
        String[] arguments = { "127.0.0.1:12345", "google.com:80" };

        Assertions.assertDoesNotThrow(() -> DatagramCrusherMain.main(arguments));
    }
}
