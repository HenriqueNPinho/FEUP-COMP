package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisTest {
    @Test
    public void test() {
        var results = TestUtils.analyse(SpecsIo.getResource("fixtures/public/CompleteTest.jmm"));
        System.out.println("Symbol Table: " + results.getSymbolTable().print());
        TestUtils.noErrors(results);
    }
}
