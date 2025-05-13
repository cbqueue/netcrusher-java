package org.netcrusher.test;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class AbstractTestLinux {

    protected static final Pattern MD5_PATTERN = Pattern.compile("[0-9abcdef]{32}");

    protected static final String ADDR_LOOPBACK4 = "127.0.0.1";

    protected static final String ADDR_LOOPBACK6 = "::1";

    @BeforeAll
    public static void checkLinux() {
        String os = System.getProperty("os.name");
        Assumptions.assumeTrue("linux".equalsIgnoreCase(os), "This test runs only on Linux");
        Assumptions.assumeFalse(Boolean.getBoolean("skipLinuxTests"), "Linux tests are disabled");
    }

    protected static void output(Logger logger, String name, String lines) {
        logger.info(
                "{}:\n" +
                "--------------------------------------------------------\n" +
                "{}\n" +
                "--------------------------------------------------------",
            name, lines);
    }

    protected static Stream<String> extractMd5(Collection<String> lines) {
        return lines.stream()
            .map(s -> {
                Matcher matcher = MD5_PATTERN.matcher(s);
                if (matcher.find()) {
                    return matcher.group();
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull);
    }

}
