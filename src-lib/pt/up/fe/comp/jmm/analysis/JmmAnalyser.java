package pt.up.fe.comp.jmm.analysis;

import java.util.ArrayList;
import java.util.List;

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

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
