package org.netcrusher.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.netcrusher.test.process.ProcessResult;
import org.netcrusher.test.process.ProcessWrapper;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

class CheckLinuxTest extends AbstractTestLinux {

    @Test
    void check() throws Exception {
        Assertions.assertTrue(ensureCommand(Arrays.asList("socat", "-V")), "<socat> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("iperf3", "-v")), "<iperf3> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("openssl", "version")), "<openssl> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("pv", "-V")), "<pv> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("tee", "--version")), "<tee> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("dd", "--version")), "<dd> is not found");
        Assertions.assertTrue(ensureCommand(Arrays.asList("bash", "--version")), "<bash> is not found");
    }

    private static boolean ensureCommand(List<String> commands) throws Exception {
        ProcessWrapper wrapper = new ProcessWrapper(commands);

        Future<ProcessResult> future = wrapper.run();

        return future.get().getExitCode() == 0;
    }

    @Test
    void checkMd5Extraction() {
        List<String> hashes = extractMd5(Arrays.asList(
            "rwgrw g w 10f8941b7e6239f4e2af05fa916037fd rwgr",
            "10f8941b7e6239f4e2af05fa916038fd",
            "10f8941b7e6239f4e2af05fa916039fd rwgr"
        )).collect(Collectors.toList());

        Assertions.assertEquals(3, hashes.size());
        Assertions.assertEquals("10f8941b7e6239f4e2af05fa916037fd", hashes.get(0));
        Assertions.assertEquals("10f8941b7e6239f4e2af05fa916038fd", hashes.get(1));
        Assertions.assertEquals("10f8941b7e6239f4e2af05fa916039fd", hashes.get(2));
    }
}
