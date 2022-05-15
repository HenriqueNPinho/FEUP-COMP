package pt.up.fe.comp.jmm.ollir;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {

    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MethodDeclaration", this::methodDeclVisit);
    }

    public String getCode() {
        return code.toString();
    }

    private Integer programVisit(JmmNode program, Integer dummy) {
        for (var importString : symbolTable.getImports()) {
            code.append("import ").append(importString).append(";\n");
        }

        for (var child : program.getChildren()) {
            visit(child);
        }

        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy) {
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if (superClass != null) {
            code.append(" extends ").append(superClass);
        }
        code.append(" {\n");

        if (classDecl.getJmmChild(1).getKind().equals("VarDeclaration")) {
            for (var field : symbolTable.getFields()) {
                code.append(".field ").append(OllirUtils.getCode(field)).append(";\n");
            }
        }

        for (var child : classDecl.getChildren()) {
            visit(child);
        }

        code.append("}\n");

        return 0;
    }

    private Integer methodDeclVisit(JmmNode methodDecl, Integer dummy) {
        var methodSignature = methodDecl.get("name");
        var isStatic = Boolean.valueOf(methodDecl.getOptional("static").toString());

        code.append(".method public ");
        if (isStatic) {
            code.append("static ");
        }
        code.append(methodSignature).append("(");

        var params = symbolTable.getParameters(methodSignature);

        var paramCode = params.stream().map(symbol -> OllirUtils.getCode(symbol)).collect(Collectors.joining(", "));

        code.append(paramCode);
        code.append(").");

        code.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));
        code.append(" {\n");
        code.append("}\n");

        return 0;
    }
}
