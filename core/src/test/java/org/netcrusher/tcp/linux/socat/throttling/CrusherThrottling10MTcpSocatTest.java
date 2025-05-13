package org.netcrusher.tcp.linux.socat.throttling;

public class CrusherThrottling10MTcpSocatTest extends AbstractThrottlingTcpSocatTestLinux {

    private static final int BYTES_PER_SEC = 10_000_000;

    private static final int DURATION_SEC = 20;

    public CrusherThrottling10MTcpSocatTest() {
        super(BYTES_PER_SEC, DURATION_SEC);
    }
}
