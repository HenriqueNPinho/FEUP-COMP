package jmm.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableFiller extends PreorderJmmVisitor<SymbolTableBuilder, Integer> {

    private final List<Report> reports;

    public SymbolTableFiller() {
        this.reports = new ArrayList<>();

        addVisit("ImportDeclaration", this::importDeclVisit);
        addVisit("ClassDeclaration", this::ClassDeclVisit);
        addVisit("MethodDeclaration", this::MethodDeclVisit);
        addVisit("Parameters", this::ParametersVisit);
        addVisit("VarDeclaration", this::VarDeclVisit);
        addVisit("ReturnExp", this::returnExpVisit);
        addVisit("BinOp", this::binOpVisit);
        addVisit("MethodCall", this::methodCallVisit);
        addVisit("ArrayAccess", this::arrayAccessVisit);
        addVisit("Assignment", this::assignmentVisit);
        addVisit("Condition", this::conditionVisit);
        addVisit("Arguments", this::argumentsVisit);
    }

    public List<Report> getReports() {
        return reports;
    }

    private Integer importDeclVisit(JmmNode importDecl, SymbolTableBuilder symbolTable) {
        var importString = importDecl.getChildren().stream().map(id -> id.get("name")).collect(Collectors.joining("."));
        symbolTable.addImport(importString);
        return 0;
    }

    private Integer ClassDeclVisit(JmmNode classDecl, SymbolTableBuilder symbolTable) {
        symbolTable.setClassName(classDecl.getJmmChild(0).get("name"));
        classDecl.getJmmChild(0).getOptional("extends").ifPresent(symbolTable::setSuper);

        boolean has = false;
        if (classDecl.getJmmChild(0).getOptional("extends").isPresent()) {
            if (!symbolTable.getImports().contains(symbolTable.getSuper())) {
                for (var imp : symbolTable.getImports()) {
                    var splitImport = imp.split("\\.");
                    var lastImport = splitImport[splitImport.length - 1];
                    if (lastImport.equals(symbolTable.getSuper())) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(classDecl.get("line")), Integer.parseInt(classDecl.get("col")), "You need to import the class '" + symbolTable.getSuper() + "' to extend it", null));
                    return -1;
                }
            }
        }
        if(classDecl.getNumChildren()<=1){
            return 0;
        }
        if (classDecl.getJmmChild(1).getKind().equals("VarDeclaration")) {
            var fieldNames = classDecl.getJmmChild(1).getChildren().stream().map(id -> id.get("name")).collect(Collectors.toList());
            var fieldTypes = classDecl.getJmmChild(1).getChildren().stream().map(id -> id.get("type")).collect(Collectors.toList());
            List<Symbol> symbols = new ArrayList<>();
            for (int i = 0; i < fieldNames.size(); ++i) {
                Type type = new Type(fieldTypes.get(i), fieldTypes.get(i).equals("integer array") || fieldTypes.get(i).equals("string array"));
                Symbol symbol = new Symbol(type, fieldNames.get(i));
                symbolTable.addField(symbol);
            }
        }

        return 0;
    }

    private Integer MethodDeclVisit(JmmNode methodDecl, SymbolTableBuilder symbolTable) {
        var methodString =  methodDecl.get("name");

        if (symbolTable.hasMethod(methodString)) {
            reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(methodDecl.get("line")), Integer.parseInt(methodDecl.get("col")), "Found duplicated methods with signature '" + methodString + "'", null));
            return -1;
        }

        var returnType = methodDecl.get("return type");
        Type type = new Type(returnType, returnType.equals("integer array"));

        symbolTable.addReturnType(methodString, type);
        symbolTable.addMethod(methodString);

        return 0;
    }

    private Integer ParametersVisit(JmmNode parameters, SymbolTableBuilder symbolTable) {
        var paramName = parameters.getChildren().stream().map(id -> id.get("name")).collect(Collectors.toList());
        var paramType = parameters.getChildren().stream().map(id -> id.get("type")).collect(Collectors.toList());
        List<Symbol> symbols = new ArrayList<>();
        for (int i = 0; i < paramName.size(); ++i) {
            Type type = new Type(paramType.get(i), paramType.get(i).equals("integer array") || paramType.get(i).equals("string array"));
            Symbol symbol = new Symbol(type, paramName.get(i));
            symbols.add(symbol);
        }
        symbolTable.addParameters(parameters.getJmmParent().get("name"), symbols);
        return 0;
    }

    private Integer VarDeclVisit(JmmNode varDecl, SymbolTableBuilder symbolTable) {
        if (varDecl.getJmmParent().getKind().equals("ClassDeclaration")) {
            return -1;
        }
        var varName = varDecl.getChildren().stream().map(id -> id.get("name")).collect(Collectors.toList());
        var varType = varDecl.getChildren().stream().map(id -> id.get("type")).collect(Collectors.toList());
        List<Symbol> symbols = new ArrayList<>();
        for (int i = 0; i < varName.size(); ++i) {
            for (var variable : symbols) {
                if (variable.getName().equals(varName.get(i))) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(varDecl.get("line")), Integer.parseInt(varDecl.get("col")), "Found two variables with name '" + varName.get(i) + "'", null));
                    return -1;
                }
            }
            Type type = new Type(varType.get(i), varType.get(i).equals("integer array"));
            Symbol symbol = new Symbol(type, varName.get(i));
            boolean has = false;
            if (!type.getName().equals("int") && !type.getName().equals("boolean") && !type.getName().equals(symbolTable.getClassName()) && !type.isArray()) {
                if (!symbolTable.getImports().contains(type.getName())) {
                    for (var imp : symbolTable.getImports()) {
                        var splitImport = imp.split("\\.");
                        var lastImport = splitImport[splitImport.length - 1];
                        if (lastImport.equals(symbolTable.getSuper())) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) {
                        reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(varDecl.get("line")), Integer.parseInt(varDecl.get("col")), "Found var of type '" + type.getName() + "' but is not imported", null));
                        return -1;
                    }
                }
            }
            symbols.add(symbol);
        }
        symbolTable.addLocalVariables(varDecl.getAncestor("MethodDeclaration").get().get("name"), symbols);
        return 0;
    }

    private Integer returnExpVisit(JmmNode returnExp, SymbolTableBuilder symbolTable) {
        // TODO: needs more cases
        var method = returnExp.getAncestor("MethodDeclaration").get().get("name");
        var child = returnExp.getJmmChild(0).getKind();
        if (child.equals("Id")) {
            var value = returnExp.getJmmChild(0).get("name");
            for (var variable : symbolTable.getLocalVariables(method)) {
                if (variable.getName().equals(value)) {
                    if (!variable.getType().getName().equals(returnExp.getAncestor("MethodDeclaration").get().get("return type"))) {
                        reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Return type does not match function return type", null));
                        return -1;
                    }
                    return 0;
                }
            }
            for (var variable : symbolTable.getParameters(method)) {
                if (variable.getName().equals(value)) {
                    if (!variable.getType().getName().equals(returnExp.getAncestor("MethodDeclaration").get().get("return type"))) {
                        reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Return type does not match function return type", null));
                        return -1;
                    }
                    return 0;
                }
            }
            for (var variable : symbolTable.getFields()) {
                if (variable.getName().equals(value)) {
                    if (!variable.getType().getName().equals(returnExp.getAncestor("MethodDeclaration").get().get("return type"))) {
                        reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Return type does not match function return type", null));
                        return -1;
                    }
                    return 0;
                }
            }
            reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Variable '" + value + "' has not been declared", null));
            return -1;
        }
        if (child.equals("MethodCall")) {
            if (symbolTable.getMethods().contains(returnExp.getJmmChild(0).getJmmChild(1).get("name"))) {
                if (!symbolTable.getReturnType(returnExp.getJmmChild(0).getJmmChild(1).get("name")).getName().equals(returnExp.getAncestor("MethodDeclaration").get().get("return type"))) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Return type does not match function return type", null));
                    return -1;
                }
            }
        }
        return 0;
    }

    private Integer binOpVisit(JmmNode binOp, SymbolTableBuilder symbolTable) {
        var op = binOp.get("op");
        var method = binOp.getAncestor("MethodDeclaration").get().get("name");
        switch (op) {
            case "and":
                if (binOp.getJmmChild(0).getKind().equals("Bool") && binOp.getJmmChild(1).getKind().equals("Bool")) {
                    return 0;
                }
                if (binOp.getJmmChild(0).getKind().equals("Id")) {
                    if (binOp.getJmmChild(1).getKind().equals("Bool") && symbolTable.getVariableType(binOp.getJmmChild(0).get("name"), method).equals("boolean")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("Bool")) {
                    if (binOp.getJmmChild(1).getKind().equals("Id") && symbolTable.getVariableType(binOp.getJmmChild(1).get("name"), method).equals("boolean")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("Id") && binOp.getJmmChild(1).getKind().equals("Id")) {
                    if (symbolTable.getVariableType(binOp.getJmmChild(0).get("name"), method).equals("boolean") && symbolTable.getVariableType(binOp.getJmmChild(1).get("name"), method).equals("boolean")) {
                        return 0;
                    }
                }
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(binOp.get("line")), Integer.parseInt(binOp.get("col")), "You can only use and on boolean values", null));
                return -1;
            case "add":
            case "sub":
            case "mult":
            case "div":
            case "lower":
                if ((binOp.getJmmChild(0).getKind().equals("IntLiteral") && binOp.getJmmChild(1).getKind().equals("IntLiteral")) || (binOp.getJmmChild(0).getKind().equals("BinOp") && binOp.getJmmChild(1).getKind().equals("BinOp"))) {
                    return 0;
                }
                if (binOp.getJmmChild(0).getKind().equals("Id") && binOp.getJmmChild(1).getKind().equals("Id")) {
                    if (symbolTable.getVariableType(binOp.getJmmChild(0).get("name"), method).equals("int")
                    && symbolTable.getVariableType(binOp.getJmmChild(1).get("name"), method).equals("int")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("MethodCall") && binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                    if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("name")).getName().equals("int")
                    && symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("BinOp")) {
                    if (binOp.getJmmChild(1).getKind().equals("Id")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(1).get("name"), method).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("IntLiteral")) {
                    if (binOp.getJmmChild(1).getKind().equals("Id")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(1).get("name"), method).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("BinOp")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("Id")) {
                    if (binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(0).get("name"), method).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(0).get("name"), method).equals("int") && symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("BinOp")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("MethodCall")) {
                    if (binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("Id")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("name")).getName().equals("int") && symbolTable.getVariableType(binOp.getJmmChild(1).get("value"), method).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("BinOp")) {
                        return 0;
                    }
                }
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(binOp.get("line")), Integer.parseInt(binOp.get("col")), "You can only add/sub/div/mult/lower int values", null));
                return -1;
        }
        return 0;
    }

    private Integer methodCallVisit(JmmNode methodCall, SymbolTableBuilder symbolTable) {
        String method = methodCall.getAncestor("MethodDeclaration").get().get("name");
        if (methodCall.getJmmChild(0).getKind().equals("Id")) {
            var call = methodCall.getJmmChild(0);
            if (call.get("name").equals("this")) {
                if (methodCall.getJmmChild(1).getKind().equals("Id")) { // CHECK IF METHOD AFTER THIS IS IN CLASS
                    var callee = methodCall.getJmmChild(1).get("name");
                    if (symbolTable.getMethods().contains(callee)) {
                        return 0;
                    }
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(methodCall.get("line")), Integer.parseInt(methodCall.get("col")), "method '" + callee + "' does not belong to class", null));
                    return -1;

                }
                return 0;
            }
            boolean has = false;
            if (!symbolTable.getImports().contains(call.get("name"))) {
                for (var imp : symbolTable.getImports()) {
                    var splitImport = imp.split("\\.");
                    var lastImport = splitImport[splitImport.length - 1];
                    if (lastImport.equals(symbolTable.getSuper())) {
                        has = true;
                        break;
                    }
                }
                for (var variable : symbolTable.getLocalVariables(method)) {
                    if (call.get("name").equals(variable.getName())) {
                        has = true;
                        break;
                    }
                }
                if (!has) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(methodCall.get("line")), Integer.parseInt(methodCall.get("col")), "import is missing for '" + call.get("name") + "'", null));
                    return -1;
                }
            }
        }

        if (methodCall.getJmmChild(0).getKind().equals("Id") && methodCall.getJmmChild(1).getKind().equals("Id")) {
            String name1 = methodCall.getJmmChild(0).get("name");
            String name2 = methodCall.getJmmChild(1).get("name");

            for (var imp : symbolTable.getImports()) {
                var splitImport = imp.split("\\.");
                var lastImport = splitImport[splitImport.length - 1];
                if (lastImport.equals(name1)) {
                    return 0;
                }
            }

            if (symbolTable.getVariableType(name1, method).equals(symbolTable.getClassName())) {
                if (symbolTable.getSuper() != null) {
                    return 0;
                }
                if (!symbolTable.getMethods().contains(name2)) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(methodCall.get("line")), Integer.parseInt(methodCall.get("col")), "method '" + name2 + "' is not declared", null));
                    return -1;
                }
            }
        }
        return 0;
    }

    private Integer arrayAccessVisit(JmmNode arrayAccess, SymbolTableBuilder symbolTable) {
        // TODO: probably needs more cases
        var array = arrayAccess.getJmmChild(0);
        var access = arrayAccess.getJmmChild(1);
        var method = arrayAccess.getAncestor("MethodDeclaration").get().get("name");
        if (!symbolTable.getVariableType(array.get("name"), method).equals("integer array") &&
                !symbolTable.getVariableType(array.get("name"), method).equals("string array")) {
            reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(arrayAccess.get("line")), Integer.parseInt(arrayAccess.get("col")), "'" + array.get("name") + "' is not an array", null));
            return -1;
        }
        if (access.getKind().equals("IntLiteral")) {
            return 0;
        }
        if (access.getKind().equals("Id")) {
            if (!symbolTable.getVariableType(access.get("name"), method).equals("int")) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(arrayAccess.get("line")), Integer.parseInt(arrayAccess.get("col")), "'" + access.get("name") + "' is not an integer", null));
                return -1;
            }
        }
        return 0;
    }

    private Integer assignmentVisit(JmmNode assignment, SymbolTableBuilder symbolTable) {
        var name = assignment.get("name");
        var method = assignment.getAncestor("MethodDeclaration").get().get("name");
        if (symbolTable.getVariableType(name, method).equals("")) {
            reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(assignment.get("line")), Integer.parseInt(assignment.get("col")), "variable '" + name + "' has not been declared", null));
            return -1;
        }
        if (assignment.getJmmChild(0).getKind().equals("IntLiteral")) {
            if (!symbolTable.getVariableType(name, method).equals("int")) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(assignment.get("line")), Integer.parseInt(assignment.get("col")), "can not assign '" + symbolTable.getVariableType(name, method) + "' to int", null));
                return -1;
            }
        }
        if (assignment.getJmmChild(0).getKind().equals("Id")) {
            var name2 = assignment.getJmmChild(0).get("name");
            if (symbolTable.getVariableType(name2, method).equals("")) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(assignment.get("line")), Integer.parseInt(assignment.get("col")), "variable '" + name2 + "' has not been declared", null));
                return -1;
            }
            if (symbolTable.getVariableType(name2, method).equals(symbolTable.getClassName()) &&
                    symbolTable.getVariableType(name, method).equals(symbolTable.getSuper())) {
                return 0;
            }
            if (symbolTable.getImports().contains(symbolTable.getVariableType(name, method)) &&
            symbolTable.getImports().contains(symbolTable.getVariableType(name2, method))) {
                return 0;
            }
            if (!symbolTable.getVariableType(name, method).equals(symbolTable.getVariableType(name2, method))) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(assignment.get("line")), Integer.parseInt(assignment.get("col")), "types '" + symbolTable.getVariableType(name, method) + "' and '" + symbolTable.getVariableType(name2, method) + "' are not compatible", null));
                return -1;
            }
        }
        return 0;
    }

    private Integer conditionVisit(JmmNode condition, SymbolTableBuilder symbolTable) {
        String conditionType = condition.getJmmChild(0).getKind();
        String method = condition.getAncestor("MethodDeclaration").get().get("name");
        if (conditionType.equals("BinOp")) {
            if (condition.getJmmChild(0).get("op").equals("lower") || condition.getJmmChild(0).get("op").equals("and")) {
                return 0;
            }
            else {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(condition.get("line")), Integer.parseInt(condition.get("col")), "condition can not be of type '" + condition.getJmmChild(0).get("op") + "'", null));
                return -1;
            }
        }
        if (conditionType.equals("Id")) {
            if (!symbolTable.getVariableType(condition.getJmmChild(0).get("name"), method).equals("boolean")) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(condition.get("line")), Integer.parseInt(condition.get("col")), "condition can not be of type '" + symbolTable.getVariableType(condition.getJmmChild(0).get("name"), method) + "'", null));
                return -1;
            }
        }
        return 0;
    }

    private Integer argumentsVisit(JmmNode arguments, SymbolTableBuilder symbolTable) {
        var method = arguments.getAncestor("MethodDeclaration").get().get("name");
        if (symbolTable.getParameters(arguments.getAncestor("MethodCall").get().getJmmChild(1).get("name")).isEmpty()) {
            return 0;
        }
        for (int i = 0; i < arguments.getNumChildren(); ++i) {
            if (arguments.getJmmChild(i).getKind().equals("Id")) {
                if (!symbolTable.getVariableType(arguments.getJmmChild(i).get("name"), method).equals(symbolTable.getParameters(arguments.getAncestor("MethodCall").get().getJmmChild(1).get("name")).get(i).getType().getName())) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(arguments.get("line")), Integer.parseInt(arguments.get("col")), "argument of type '" + symbolTable.getVariableType(arguments.getJmmChild(i).get("name"), method) + "' does not match with param", null));
                    return -1;
                }
            }
            if (arguments.getJmmChild(i).getKind().equals("IntLiteral")) {
                if (!symbolTable.getParameters(arguments.getAncestor("MethodCall").get().getJmmChild(1).get("name")).get(i).getType().getName().equals("int")) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(arguments.get("line")), Integer.parseInt(arguments.get("col")), "argument of type int does not match with param", null));
                    return -1;
                }
            }
        }
        return 0;
    }
}
