package jmm.astToJasmin;

import jmm.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyAstToJasmin extends AJmmVisitor<Integer, Integer> implements AstToJasmin {
    StringBuilder jasminCode;
    private SymbolTable symbolTable;
    List<Report> reports;
    int currentStackSize = 0;
    int maxStackSize = 0;
    List<Symbol> varRegisters;

    public MyAstToJasmin(){
        this.jasminCode = new StringBuilder();
        reports = new ArrayList<>();
        varRegisters = new ArrayList<>();

        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MethodDeclaration", this::methodDeclVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("ReturnExp", this::returnExpVisit);
        addVisit("Assignment", this::AssignVisit);
    }

    public String getVariableType(String varName, String methodName) {
        if (symbolTable.getLocalVariables(methodName).isEmpty() && symbolTable.getParameters(methodName).isEmpty())
            return "";
        if (!symbolTable.getLocalVariables(methodName).isEmpty()) {
            for (var symbol : symbolTable.getLocalVariables(methodName)) {
                if (symbol.getName().equals(varName)) {
                    return symbol.getType().getName();
                }
            }
        }
        if (!symbolTable.getParameters(methodName).isEmpty()) {
            for (var symbol2 : symbolTable.getParameters(methodName)) {
                if (symbol2.getName().equals(varName)) {
                    return symbol2.getType().getName();
                }
            }
        }
        return "";
    }

    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();
        System.out.println(jasminCode.toString());
        this.symbolTable = semanticsResult.getSymbolTable();
        this.visit(rootNode);
        return new JasminResult(rootNode.getClass().getName(), jasminCode.toString(), reports);
    }

    private Integer programVisit(JmmNode program, Integer dummy) {
        for (var child : program.getChildren()) {
            visit(child);
        }
        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy) {
        // CLASS
        System.out.println(symbolTable);
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

        // CONSTRUCTOR
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
        jasminCode.append(".end method").append("\n");

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
        var localVars = symbolTable.getLocalVariables(methodSignature);

        // ASSIGN REGISTERS
        varRegisters.addAll(params);
        varRegisters.addAll(localVars);


        var paramCode = params.stream().map(symbol -> AstToJasminParam.getCode(symbol.getType())).collect(Collectors.joining(""));

        jasminCode.append(paramCode);
        jasminCode.append(")");

        jasminCode.append(AstToJasminReturn.getJasminType(symbolTable.getReturnType(methodSignature).getName())).append("\n");

        int localVarsSize = symbolTable.getLocalVariables(methodSignature).size();

        jasminCode.append(".limit stack 99\n").append(".limit locals ").append(localVarsSize).append("\n"); // TODO: Stack size

        for (var child : methodDecl.getChildren()) {
            visit(child);
        }

        if (methodSignature.equals("main")) {
            jasminCode.append("return").append("\n");
        }

        jasminCode.append(".end method").append("\n");

        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy) {
        var op = binOp.get("op");
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
        return 0;
    }

    private Integer returnExpVisit(JmmNode returnExp, Integer dummy) {
        var methodDecl = returnExp.getAncestor("MethodDeclaration").get().get("name");
        if (returnExp.getJmmChild(0).getKind().equals("IntLiteral")) {
            jasminCode.append("iconst_").append(returnExp.getJmmChild(0).get("value")).append("\n");
            jasminCode.append("ireturn\n");
            return 0;
        }

        if (returnExp.getJmmChild(0).getKind().equals("Id")) {
            var type = this.getVariableType(returnExp.getJmmChild(0).get("name"), methodDecl);
            int register = 1000000;
            for (int i = 0; i < this.varRegisters.size(); ++i) {
                if (varRegisters.get(i).getName().equals(returnExp.getJmmChild(0).get("name"))) {
                    register = i;
                }
            }
            switch (type) {
                case "boolean":
                    jasminCode.append("bload_").append(Integer.toString(register)).append("\n");
                    jasminCode.append("breturn\n");
            }
        }
        return 0;
    }

    private Integer AssignVisit(JmmNode assign, Integer dummy){
        return 0;
    }
}
