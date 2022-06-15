package jmm.astToJasmin;

import jmm.analysis.SymbolTableBuilder;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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
    private SymbolTable symbolTable;
    List<Report> reports;
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
        addVisit("VarDeclaration", this::varDeclVisit);
        addVisit("Assignment", this::assignVisit);
        addVisit("StatementExpression", this::stmtExpVisit);
        addVisit("MethodCall", this::methodCallVisit);
        addVisit("IfStatement", this::ifStmtVisit);
        addVisit("IfTrue", this::ifTrueVisit);
        addVisit("IfFalse", this::ifFalseVisit);
    }

    public String getVariableType(String varName, String methodName) {
        if (symbolTable.getLocalVariables(methodName).isEmpty() && symbolTable.getParameters(methodName).isEmpty() && symbolTable.getFields().isEmpty())
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
        if (!symbolTable.getFields().isEmpty()) {
            for (var symbol3 : symbolTable.getFields()) {
                if (symbol3.getName().equals(varName)) {
                    return symbol3.getType().getName();
                }
            }
        }
        return "";
    }

    public boolean methodHasParam(String method, String param) {
        for (var parameter : symbolTable.getParameters(method)) {
            if (parameter.getName().equals(param)) {
                return true;
            }
        }
        return false;
    }

    public boolean methodHasVar(String method, String variable) {
        for (var parameter : symbolTable.getLocalVariables(method)) {
            if (parameter.getName().equals(variable)) {
                return true;
            }
        }
        return false;
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
        boolean isStatic = methodDecl.getOptional("static").isPresent();

        jasminCode.append(".method public ");
        if (isStatic) {
            jasminCode.append("static ");
        }
        jasminCode.append(methodSignature).append("(");

        var params = symbolTable.getParameters(methodSignature);
        var localVars = symbolTable.getLocalVariables(methodSignature);

        // ASSIGN REGISTERS
        Type thisType = new Type(symbolTable.getClassName(), false);
        Symbol thisSymbol = new Symbol(thisType, "this");
        varRegisters.add(thisSymbol);
        varRegisters.addAll(params);
        varRegisters.addAll(localVars);

        var paramCode = params.stream().map(symbol -> AstToJasminParam.getCode(symbol.getType())).collect(Collectors.joining(""));

        jasminCode.append(paramCode);
        jasminCode.append(")");

        jasminCode.append(AstToJasminReturn.getJasminType(symbolTable.getReturnType(methodSignature).getName())).append("\n");

        int localVarsSize = varRegisters.size();

        jasminCode.append(".limit stack 50\n").append(".limit locals ").append(localVarsSize).append("\n");

        for (var child : methodDecl.getChildren()) {
            visit(child);
        }

        if (methodSignature.equals("main")) {
            jasminCode.append("return").append("\n");
        }

        jasminCode.append(".end method").append("\n");

        varRegisters.clear();

        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, Integer dummy) {
        var op = binOp.get("op");

        if (binOp.getJmmChild(0).getKind().equals("BinOp") && binOp.getJmmChild(1).getKind().equals("BinOp")) {
            visit(binOp.getJmmChild(0));
            visit(binOp.getJmmChild(1));
        }

        else if (binOp.getJmmChild(0).getKind().equals("IntLiteral") && binOp.getJmmChild(1).getKind().equals("BinOp")) {
            visit(binOp.getJmmChild(1));
            if (Integer.parseInt(binOp.getJmmChild(0).get("value")) > 5 || Integer.parseInt(binOp.getJmmChild(0).get("value")) < -1){
                jasminCode.append("bipush ").append(binOp.getJmmChild(0).get("value")).append("\n");
            } else {
                jasminCode.append("iconst_").append(binOp.getJmmChild(0).get("value")).append("\n");
            }
        }

        else if (binOp.getJmmChild(1).getKind().equals("IntLiteral") && binOp.getJmmChild(0).getKind().equals("BinOp")) {
            visit(binOp.getJmmChild(0));
            if (Integer.parseInt(binOp.getJmmChild(1).get("value")) > 5 || Integer.parseInt(binOp.getJmmChild(1).get("value")) < -1){
                jasminCode.append("bipush ").append(binOp.getJmmChild(1).get("value")).append("\n");
            } else {
                jasminCode.append("iconst_").append(binOp.getJmmChild(1).get("value")).append("\n");
            }
        }

        else if (binOp.getJmmChild(0).getKind().equals("Id") && binOp.getJmmChild(1).getKind().equals("Id")) {
            int register1 = -1;
            int register2 = -1;
            for (int i = 0; i < this.varRegisters.size(); ++i) {
                if (varRegisters.get(i).getName().equals(binOp.getJmmChild(0).get("name"))) {
                    register1 = i;
                }
                if (varRegisters.get(i).getName().equals(binOp.getJmmChild(1).get("name"))) {
                    register2 = i;
                }
            }
            jasminCode.append("iload_").append(Integer.toString(register1)).append("\n");
            jasminCode.append("iload_").append(Integer.toString(register2)).append("\n");
        }

        else if (binOp.getJmmChild(0).getKind().equals("IntLiteral") && binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
            if (Integer.parseInt(binOp.getJmmChild(0).get("value")) > 5 || Integer.parseInt(binOp.getJmmChild(0).get("value")) < -1){
                jasminCode.append("bipush ").append(binOp.getJmmChild(0).get("value")).append("\n");
            } else {
                jasminCode.append("iconst_").append(binOp.getJmmChild(0).get("value")).append("\n");
            }

            if (Integer.parseInt(binOp.getJmmChild(1).get("value")) > 5 || Integer.parseInt(binOp.getJmmChild(1).get("value")) < -1){
                jasminCode.append("bipush ").append(binOp.getJmmChild(1).get("value")).append("\n");
            } else {
                jasminCode.append("iconst_").append(binOp.getJmmChild(1).get("value")).append("\n");
            }
        }

        else if (binOp.getJmmChild(0).getKind().equals("Bool") && binOp.getJmmChild(1).getKind().equals("Bool")) {
            if (binOp.getJmmChild(0).get("value").equals("true")) {
                jasminCode.append("iconst_1\n");
            } else {
                jasminCode.append("iconst_0\n");
            }

            if (binOp.getJmmChild(1).get("value").equals("true")) {
                jasminCode.append("iconst_1\n");
            } else {
                jasminCode.append("iconst_0\n");
            }
        }

        switch (op) {
            case "add":
                jasminCode.append("iadd\n");
                break;
            case "sub":
            case "lower":
                jasminCode.append("isub\n");
                break;
            case "mult":
                jasminCode.append("imul\n");
                break;
            case "div":
                jasminCode.append("idiv\n");
                break;
            case "and":
                jasminCode.append("iand\n");
                break;

        }
        return 0;
    }

    private Integer returnExpVisit(JmmNode returnExp, Integer dummy) {
        var methodDecl = returnExp.getAncestor("MethodDeclaration").get().get("name");
        if (returnExp.getJmmChild(0).getKind().equals("IntLiteral")) {
            if (Integer.parseInt(returnExp.getJmmChild(0).get("value")) > 5 || Integer.parseInt(returnExp.getJmmChild(0).get("value")) < -1){
                jasminCode.append("bipush ").append(returnExp.getJmmChild(0).get("value")).append("\n");
            } else {
                jasminCode.append("iconst_").append(returnExp.getJmmChild(0).get("value")).append("\n");
            }
            jasminCode.append("ireturn\n");
            return 0;
        }

        if (returnExp.getJmmChild(0).getKind().equals("Id")) {
            var type = this.getVariableType(returnExp.getJmmChild(0).get("name"), methodDecl);
            int register = -1;
            for (int i = 0; i < this.varRegisters.size(); ++i) {
                if (varRegisters.get(i).getName().equals(returnExp.getJmmChild(0).get("name"))) {
                    register = i;
                }
            }
            switch (type) {
                case "boolean":
                case "int":
                    jasminCode.append("iload_").append(register).append("\n");
                    jasminCode.append("ireturn\n");
            }
            return 0;
        }

        visit(returnExp.getJmmChild(0));

        if (returnExp.getJmmChild(0).getKind().equals("BinOp")) {
            jasminCode.append("ireturn\n");
        }
        return 0;
    }

    private Integer assignVisit(JmmNode assign, Integer dummy){
        var method = assign.getAncestor("MethodDeclaration").get().get("name");
        var name = assign.get("name");
        var type = this.getVariableType(name, method);
        for (var field : symbolTable.getFields()) {
            if (field.getName().equals(name) && !methodHasParam(method, name) && !methodHasVar(method, name)) {
                if (assign.getJmmChild(0).getKind().equals("IntLiteral")) {
                    jasminCode.append("bipush ").append(assign.getJmmChild(0).get("value")).append("\n");
                    Type type1 = new Type(type, type.equals("string array") || type.equals("int array"));
                    Symbol symbol = new Symbol(type1, name);
                    jasminCode.append("putpublic fields/").append(AstToJasminField.getCode(symbol));
                }
            }
        }
        int register = -1;
        for (int i = 0; i < this.varRegisters.size(); ++i) {
            if (varRegisters.get(i).getName().equals(name)) {
                register = i;
            }
        }

        switch (type) {
            case "boolean":
            case "int":
                if (assign.getJmmChild(0).getKind().equals("IntLiteral")) {
                    if (Integer.parseInt(assign.getJmmChild(0).get("value")) > 5 || Integer.parseInt(assign.getJmmChild(0).get("value")) < -1){
                        jasminCode.append("bipush ").append(assign.getJmmChild(0).get("value")).append("\n");
                    }
                    else {
                        jasminCode.append("iconst_").append(assign.getJmmChild(0).get("value")).append("\n");
                    }
                }
                else if (assign.getJmmChild(0).getKind().equals("Id")) {
                    int aux=0;
                    for (int i = 0; i < this.varRegisters.size(); ++i) {
                        if (varRegisters.get(i).getName().equals(assign.getJmmChild(0).get("name"))) {
                           aux = i;
                        }
                    }
                    jasminCode.append("iload_").append(aux).append("\n");
                }
                for (var child : assign.getChildren()) {
                    if (child.getKind().equals("BinOp")) {
                        visit(child);
                    }
                }
                jasminCode.append("istore_").append(register).append("\n");
        }
        return 0;
    }

    private Integer stmtExpVisit(JmmNode stmtExp, Integer dummy){
        for (var child : stmtExp.getChildren()) {
            visit(child);
        }
        return 0;
    }

    private Integer methodCallVisit(JmmNode methodCall, Integer dummy) {
        var method = methodCall.getAncestor("MethodDeclaration").get().get("name");
        var caller = methodCall.getJmmChild(0).get("name");
        var callee = methodCall.getJmmChild(1).get("name");
        var arguments = methodCall.getJmmChild(2);

        for (var argument : arguments.getChildren()) {
            if (argument.getKind().equals("Id")) {
                var register = -1;
                for (int i = 0; i < this.varRegisters.size(); ++i) {
                    if (varRegisters.get(i).getName().equals(argument.get("name"))) {
                        register = i;
                    }
                }
                var type = getVariableType(argument.get("name"), method);
                switch (type) {
                    case "int":
                    case "boolean":
                        jasminCode.append("iload_").append(register).append("\n");
                        break;
                    case "integer array":
                    case "string array":
                        jasminCode.append("aload_").append(register).append("\n");
                        break;
                }
            }

            else if (argument.getKind().equals("IntLiteral")) {
                if (Integer.parseInt(argument.get("value")) > 5 || Integer.parseInt(argument.get("value")) < -1){
                    jasminCode.append("bipush ").append(argument.get("value")).append("\n");
                }
                else {
                    jasminCode.append("iconst_").append(argument.get("value")).append("\n");
                }
            }

            else if (argument.getKind().equals("BinOp")) {
                visit(argument);
            }
        }

        jasminCode.append("invokestatic ").append(caller).append("/").append(callee).append("(");
        for (var argument : arguments.getChildren()) {
            if (argument.getKind().equals("Id")) {
                var type = getVariableType(argument.get("name"), method);
                switch (type) {
                    case "int":
                        jasminCode.append("I");
                        break;
                    case "boolean":
                        jasminCode.append("Z");
                        break;
                    case "integer array":
                        jasminCode.append("[I");
                        break;
                    case "string array":
                        jasminCode.append("[Ljava/lang/String;");
                        break;
                }
            }

            else if (argument.getKind().equals("IntLiteral") || argument.getKind().equals("BinOp")) {
                jasminCode.append("I");
            }
        }
        jasminCode.append(")V").append("\n");
        return 0;
    }

    private Integer ifStmtVisit(JmmNode ifStmt, Integer dummy) {
        var condition = ifStmt.getJmmChild(0).getJmmChild(0);
        if (condition.getKind().equals("Id")) {
            var register = -1;
            for (int i = 0; i < this.varRegisters.size(); ++i) {
                if (varRegisters.get(i).getName().equals(condition.get("name"))) {
                    register = i;
                    jasminCode.append("iload_").append(register).append("\n");
                }
            }
        }
        var children = ifStmt.getAncestor("MethodDeclaration").get().getChildren();
        for (var child : children) {
            if (child.getKind().equals("Assignment") && child.get("name").equals(ifStmt.getJmmChild(0).getJmmChild(0).get("name"))) {
                if (child.getJmmChild(0).getKind().equals("BinOp")) {
                    if (child.getJmmChild(0).get("op").equals("lower")) {
                        jasminCode.append("ifgt Label1\n");
                    } else {
                        jasminCode.append("ifeq Label1\n");
                    }
                }
            }
        }
        for (var child : ifStmt.getChildren()) {
            if (!child.getKind().equals("Condition")) {
                visit(child);
            }
        }
        return 0;
    }

    private Integer ifTrueVisit(JmmNode ifTrue, Integer dummy){
        for (var child : ifTrue.getChildren()) {
            for (var child2 : child.getChildren()) {
                visit(child2);
            }
        }
        jasminCode.append("goto Label2\n");
        return 0;
    }

    private Integer ifFalseVisit(JmmNode ifFalse, Integer dummy){
        jasminCode.append("Label1:\n");
        for (var child : ifFalse.getChildren()) {
            for (var child2 : child.getChildren()) {
                visit(child2);
            }
        }
        jasminCode.append("Label2:\n");
        return 0;
    }

    private Integer varDeclVisit(JmmNode varDecl, Integer dummy){
        return 0;
    }
}
