package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizationTest {

    @Test
    public void test() {
        var ollirResult = TestUtils.optimize(SpecsIo.getResource("fixtures/public/CompleteTest.jmm"));
        TestUtils.noErrors(ollirResult);
    }
}
