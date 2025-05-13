package org.netcrusher.tcp.main;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class TcpCrusherMainTest {

    @Test
    void test() {
        String[] arguments = { "127.0.0.1:12345", "google.com:80" };

        Assertions.assertDoesNotThrow(() -> TcpCrusherMain.main(arguments));
    }

}
