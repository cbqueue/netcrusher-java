package org.netcrusher.core.meter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class RateMeterPeriodTest {

    @Test
    void test() {
        RateMeterPeriod p = new RateMeterPeriod(5000, 1000);

        Assertions.assertEquals(5000, p.getCount());
        Assertions.assertEquals(1000, p.getElapsedMs());

        Assertions.assertEquals(5000, p.getRatePerSec(), 0.1);
        Assertions.assertEquals(5000, p.getRatePer(1, TimeUnit.SECONDS), 0.1);

        Assertions.assertEquals(0.005, p.getRatePer(1, TimeUnit.MICROSECONDS), 0.0001);
        Assertions.assertEquals(5, p.getRatePer(1, TimeUnit.MILLISECONDS), 0.1);
        Assertions.assertEquals(300000, p.getRatePer(1, TimeUnit.MINUTES), 0.1);
    }
}
