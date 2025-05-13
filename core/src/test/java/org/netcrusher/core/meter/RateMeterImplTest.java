package org.netcrusher.core.meter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netcrusher.core.chronometer.MockChronometer;

import java.util.concurrent.TimeUnit;

class RateMeterImplTest {

    @Test
    void test() {
        MockChronometer mockChronometer = new MockChronometer();

        RateMeterImpl rateMeter = new RateMeterImpl(mockChronometer);

        rateMeter.update(100);

        mockChronometer.add(1, TimeUnit.SECONDS);

        Assertions.assertEquals(100, rateMeter.getTotalCount());
        Assertions.assertEquals(1000, rateMeter.getTotalElapsedMs());

        RateMeterPeriod total = rateMeter.getTotal();
        Assertions.assertEquals(100, total.getCount());
        Assertions.assertEquals(1000, total.getElapsedMs());
        Assertions.assertEquals(100, total.getRatePerSec(), 0.1);
        Assertions.assertEquals(0.1, total.getRatePer(1, TimeUnit.MILLISECONDS), 0.01);

        RateMeterPeriod period = rateMeter.getPeriod(true);
        Assertions.assertEquals(100, period.getCount());
        Assertions.assertEquals(1000, period.getElapsedMs());
        Assertions.assertEquals(100, period.getRatePerSec(), 0.1);
        Assertions.assertEquals(0.1, period.getRatePer(1, TimeUnit.MILLISECONDS), 0.01);

        period = rateMeter.getPeriod(true);
        Assertions.assertEquals(0, period.getCount());
        Assertions.assertEquals(0, period.getElapsedMs());
        Assertions.assertEquals(Double.NaN, period.getRatePerSec(), 0.1);
    }
}
