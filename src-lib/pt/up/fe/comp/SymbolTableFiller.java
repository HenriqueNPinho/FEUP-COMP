package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.stream.Collectors;

public class SymbolTableFiller extends PreorderJmmVisitor<SymbolTableBuilder, Integer> {

    SymbolTableFiller() {
        addVisit("ImportDeclaration", this::importDeclVisit);
    }

    private Integer importDeclVisit(JmmNode importDecl, SymbolTableBuilder symbolTable) {
        var importString = importDecl.getChildren().stream().map(id -> id.get("name")).collect(Collectors.joining("."));
        System.out.println("IMPORT: " + importString);
        return 0;
    }
}
