package jmm.analysis;

import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public abstract class PreorderSemanticAnalyser extends PreorderJmmVisitor<Integer, Integer> implements SemanticAnalyser {
    private final List<Report> reports;

    public PreorderSemanticAnalyser() {
        reports = new ArrayList<>();
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }

    protected void addReport(Report report) {
        reports.add(report);
    }
}
