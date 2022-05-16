package pt.up.fe.comp.jmm.analysis;

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
        //addVisit("MethodCall", this::methodCallVisit);
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

        if (classDecl.getJmmChild(0).getOptional("extends").isPresent()) {
            if (!symbolTable.getImports().contains(symbolTable.getSuper())) {
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(classDecl.get("line")), Integer.parseInt(classDecl.get("col")), "You need to import the class '" + symbolTable.getSuper() + "' to extend it", null));
                return -1;
            }
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
            Type type = new Type(varType.get(i), varType.get(i).equals("integer array") || varType.get(i).equals("string array"));
            Symbol symbol = new Symbol(type, varName.get(i));
            if (!type.getName().equals("int") && !type.getName().equals("String") && !type.getName().equals("boolean") && !type.isArray()) {
                if (!symbolTable.getImports().contains(type.getName())) {
                    reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(varDecl.get("line")), Integer.parseInt(varDecl.get("col")), "Found var of type '" + type.getName() + "' but is not imported", null));
                    return -1;
                }
            }
            symbols.add(symbol);
        }
        symbolTable.addLocalVariables(varDecl.getAncestor("MethodDeclaration").get().get("name"), symbols);
        return 0;
    }

    private Integer returnExpVisit(JmmNode returnExp, SymbolTableBuilder symbolTable) {
        // TODO: needs more cases
        if (returnExp.getJmmChild(0).getKind().equals("Id")) {
            var value = returnExp.getJmmChild(0).get("value");
            for (var variable : symbolTable.getLocalVariables(returnExp.getAncestor("MethodDeclaration").get().get("name"))) {
                if (variable.getName().equals(value)) {
                    return 0;
                }
            }
            reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(returnExp.get("line")), Integer.parseInt(returnExp.get("col")), "Variable '" + value + "' has not been declared", null));
        }
        return -1;
    }

    private Integer binOpVisit(JmmNode binOp, SymbolTableBuilder symbolTable) {
        var op = binOp.get("op");
        switch (op) {
            case "and":
                return 0;
            case "add":
            case "sub":
            case "mult":
            case "div":
            case "lower":
                if (binOp.getJmmChild(0).getKind().equals("IntLiteral") && binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                    return 0;
                }
                if (binOp.getJmmChild(0).getKind().equals("Id") && binOp.getJmmChild(1).getKind().equals("Id")) {
                    if (symbolTable.getVariableType(binOp.getJmmChild(0).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int")
                    && symbolTable.getVariableType(binOp.getJmmChild(1).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("MethodCall") && binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                    if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("name")).getName().equals("int")
                    && symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                        return 0;
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("IntLiteral")) {
                    if (binOp.getJmmChild(1).getKind().equals("Id")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(1).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("Id")) {
                    if (binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(0).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("MethodCall")) {
                        if (symbolTable.getVariableType(binOp.getJmmChild(0).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int") && symbolTable.getReturnType(binOp.getJmmChild(1).getJmmChild(1).get("name")).getName().equals("int")) {
                            return 0;
                        }
                    }
                }
                if (binOp.getJmmChild(0).getKind().equals("MethodCall")) {
                    if (binOp.getJmmChild(1).getKind().equals("IntLiteral")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("value")).getName().equals("int")) {
                            return 0;
                        }
                    }
                    if (binOp.getJmmChild(1).getKind().equals("Id")) {
                        if (symbolTable.getReturnType(binOp.getJmmChild(0).getJmmChild(1).get("value")).getName().equals("int") && symbolTable.getVariableType(binOp.getJmmChild(1).get("value"), binOp.getAncestor("MethodDeclaration").get().get("name")).equals("int")) {
                            return 0;
                        }
                    }
                }
                reports.add(Report.newError(Stage.SEMANTIC, Integer.parseInt(binOp.get("line")), Integer.parseInt(binOp.get("col")), "You can only add/sub/div/mult/lower int values", null));
                return -1;
        }
        return 0;
    }

    private Integer methodCallVisit(JmmNode methodCall, SymbolTableBuilder symbolTable) {
        return 0;
    }
}
