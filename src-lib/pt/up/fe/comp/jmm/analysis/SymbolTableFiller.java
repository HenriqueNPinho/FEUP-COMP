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
            symbols.add(symbol);
        }
        symbolTable.addLocalVariables(varDecl.getAncestor("MethodDeclaration").get().get("name"), symbols);
        return 0;
    }
}
