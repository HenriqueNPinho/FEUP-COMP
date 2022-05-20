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
import java.util.stream.Collectors;

public class MyAstToJasmin extends AJmmVisitor<Integer, Integer> implements AstToJasmin {
    StringBuilder jasminCode;
    private final SymbolTable symbolTable;
    List<Report> reports;

    public MyAstToJasmin(SymbolTable symbolTable){
        this.jasminCode = new StringBuilder();
        this.symbolTable = symbolTable;
        reports = new ArrayList<>();

        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MethodDeclaration", this::methodDeclVisit);
    }

    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();

        return new JasminResult(rootNode.getClass().getName(), jasminCode.toString(), reports);
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy) {
        // CLASS
        jasminCode.append(".class public ").append(symbolTable.getClassName()).append("\n");
        var superClass = symbolTable.getSuper();
        if (superClass != null) {
            jasminCode.append(".super ").append(superClass);
        } else {
            jasminCode.append(".super java/lang/Object");
        }
        jasminCode.append("\n");

        // FIELDS
        if (classDecl.getJmmChild(1).getKind().equals("VarDeclaration")) {
            for (var field : symbolTable.getFields()) {
                jasminCode.append(".field public ").append(AstToJasminField.getCode(field)).append("\n");
            }
        }

        // CONSTUCTOR
        jasminCode.append(".method public <init>()V").append("\n");
        jasminCode.append("aload_0").append("\n");
        jasminCode.append("invokenonvirtual ");
        if (superClass != null) {
            jasminCode.append(superClass);
        } else {
            jasminCode.append("java/lang/Object");
        }
        jasminCode.append("/<init>()V").append("\n");
        jasminCode.append("return").append("\n");
        jasminCode.append(".end method");

        for (var child : classDecl.getChildren()) {
            visit(child);
        }

        return 0;
    }

    private Integer methodDeclVisit(JmmNode methodDecl, Integer dummy) {
        var methodSignature = methodDecl.get("name");
        var isStatic = Boolean.valueOf(methodDecl.getOptional("static").toString());

        jasminCode.append(".method public ");
        if (isStatic) {
            jasminCode.append("static ");
        }
        jasminCode.append(methodSignature).append("(");

        var params = symbolTable.getParameters(methodSignature);

        var paramCode = params.stream().map(symbol -> AstToJasminParam.getCode(symbol.getType())).collect(Collectors.joining(" "));

        jasminCode.append(paramCode);
        jasminCode.append(")");

        jasminCode.append(AstToJasminReturn.getJasminType(symbolTable.getReturnType(methodSignature).getName()));

        return 0;
    }


}
