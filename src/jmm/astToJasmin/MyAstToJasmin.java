package jmm.astToJasmin;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MyAstToJasmin extends AJmmVisitor<Integer, Integer> implements AstToJasmin {
    StringBuilder jasminCode;
    private final SymbolTable symbolTable;
    List<Report> reports;

    public MyAstToJasmin(SymbolTable symbolTable){
        this.jasminCode = new StringBuilder();
        this.symbolTable = symbolTable;
        reports = new ArrayList<>();

        addVisit("ClassDeclaration", this::classDeclVisit);
    }

    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();

        return new JasminResult(rootNode.getClass().getName(), jasminCode.toString(), reports);
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy) {
        jasminCode.append(".class public ").append(symbolTable.getClassName()).append("\n");
        var superClass = symbolTable.getSuper();
        if (superClass != null) {
            jasminCode.append(".super ").append(superClass);
        } else {
            jasminCode.append(".super java/lang/Object");
        }
        jasminCode.append("\n");

        if (classDecl.getJmmChild(1).getKind().equals("VarDeclaration")) {
            for (var field : symbolTable.getFields()) {
                jasminCode.append(".field public ").append(AstToJasminField.getCode(field)).append("\n");
            }
        }

        for (var child : classDecl.getChildren()) {
            visit(child);
        }

        return 0;
    }



}
