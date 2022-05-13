package pt.up.fe.comp.jmm.analysis.analysers;

import pt.up.fe.comp.jmm.analysis.SemanticAnalyser;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleMainMethodCheckV2 implements SemanticAnalyser {

    private final SymbolTable symbolTable;

    public SingleMainMethodCheckV2(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public List<Report> getReports() {
        // TODO this is just an example
        if (!symbolTable.getMethods().contains("main")) {
            return Arrays.asList(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, -1, "Class '" + symbolTable.getClassName() + "' does not contain main method"));
        }

        return Collections.emptyList();
    }
}
