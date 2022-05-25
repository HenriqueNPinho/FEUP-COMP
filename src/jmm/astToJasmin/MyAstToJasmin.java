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
        addVisit("BinOp", this::binOpVisit);
        addVisit("ReturnExp", this::returnExpVisit);
        addVisit("VarDeclaration", this::varDeclVisit);
        addVisit("Assignment", this::AssignVisit);
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
        boolean isStatic = Boolean.parseBoolean(methodDecl.getOptional("static").toString());

        jasminCode.append(".method public ");
        if (isStatic) {
            jasminCode.append("static ");
        }
        jasminCode.append(methodSignature).append("(");

        var params = symbolTable.getParameters(methodSignature);

        var paramCode = params.stream().map(symbol -> AstToJasminParam.getCode(symbol.getType())).collect(Collectors.joining(""));

        jasminCode.append(paramCode);
        jasminCode.append(")");

        jasminCode.append(AstToJasminReturn.getJasminType(symbolTable.getReturnType(methodSignature).getName())).append("\n");

        jasminCode.append(".limit stack 99\n").append(".limit locals 2\n");

        for (var child : methodDecl.getChildren()) {
            visit(child);
        }

        jasminCode.append(".end method");

        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy) {
        var op = binOp.get("op");
        if (binOp.getJmmChild(0).getKind().equals("Id") && binOp.getJmmChild(1).getKind().equals("Id")) {
            jasminCode.append("iload_0\n").append("iload_0\n");
            switch (op) {
                case "add":
                    jasminCode.append("iadd\n");
                case "sub":
                    jasminCode.append("isub\n");
                case "mul":
                    jasminCode.append("imul\n");
                case "div":
                    jasminCode.append("idiv\n");
            }
        }
        return 0;
    }

    private Integer returnExpVisit(JmmNode returnExp, Integer dummy) {
        jasminCode.append("ireturn\n");
        return 0;
    }

    private Integer varDeclVisit(JmmNode varDecl, Integer dummy){

    }

    private Integer AssignVisit(JmmNode assing, Integer dummy){

    }
}
