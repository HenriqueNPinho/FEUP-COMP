package jmm.astToJasmin;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MyAstToJasmin implements AstToJasmin {
    StringBuilder jasminCode;
    List<Report> reports;
    public MyAstToJasmin(){
        this.jasminCode = new StringBuilder();
        reports = new ArrayList<>();
    }
    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();

        return new JasminResult(rootNode.getClass().getName(), jasminCode.toString(), reports );
    }
}
