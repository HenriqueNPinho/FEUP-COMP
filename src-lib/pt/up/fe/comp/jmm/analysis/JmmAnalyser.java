package pt.up.fe.comp.jmm.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.analysers.SingleMainMethodCheck;
import pt.up.fe.comp.jmm.analysis.analysers.SingleMainMethodCheckV2;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();

        var symbolTable = new SymbolTableBuilder();

        var symbolTableFiller = new SymbolTableFiller();
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);
        reports.addAll(symbolTableFiller.getReports());

        List<SemanticAnalyser> analysers = Arrays.asList(new SingleMainMethodCheck(symbolTable), new SingleMainMethodCheckV2(symbolTable));

        for (var analyser : analysers) {
            reports.addAll(analyser.getReports());
        }

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
