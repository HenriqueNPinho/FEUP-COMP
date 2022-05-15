package pt.up.fe.comp.jmm.ollir;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {

    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable) {
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit("Program", this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
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
        code.append("}\n");

        return 0;
    }

        private String getOllirType(Type type) {
        String name = type.getName();

        String ollirType;

        switch (name) {
            case "int":
                ollirType = "i32";
                break;
            case "void":
                ollirType = "V";
                break;
            case "boolean":
                ollirType = "bool";
                break;
            default:
                ollirType = name;
                break;
        }

        return (type.isArray() ? "array." : "") + ollirType;
    }
}
