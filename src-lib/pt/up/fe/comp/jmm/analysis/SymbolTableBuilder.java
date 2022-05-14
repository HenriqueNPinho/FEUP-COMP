package pt.up.fe.comp.jmm.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;

public class SymbolTableBuilder implements SymbolTable {

    private final List<String> imports;
    private String className;
    private String superClass;
    private final List<Symbol> fields;
    private final List<String> methods;
    private final Map<String, Type> methodReturnTypes;
    private final Map<String, List<Symbol>> methodParams;
    private final Map<String, List<Symbol>> localVariables;

    public SymbolTableBuilder() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.fields =  new ArrayList<>();
        this.methods = new ArrayList<>();
        this.methodReturnTypes = new HashMap<>();
        this.methodParams = new HashMap<>();
        this.localVariables = new HashMap<>();
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importString) {
        imports.add(importString);
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String newClassName) {
        this.className = newClassName;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    public void setSuper(String newSuperClass) {
        this.superClass = newSuperClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void addField(Symbol field) {
        fields.add(field);
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    public boolean hasMethod(String methodString) {
        return methods.contains(methodString);
    }

    public void addMethod(String methodString) {
        methods.add(methodString);
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnTypes.get(methodSignature);
    }

    public void addReturnType(String name, Type type) {
        methodReturnTypes.put(name, type);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methodParams.getOrDefault(methodSignature, new ArrayList<>());
    }

    public void addParameters(String method, List<Symbol> symbols) {
        methodParams.put(method, symbols);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return localVariables.getOrDefault(methodSignature, new ArrayList<>());
    }

    public void addLocalVariables(String method, List<Symbol> variables) {
        localVariables.put(method, variables);
    }

    public boolean methodHasVar(String methodString, String varName) {
        List<Symbol> locarVars = localVariables.get(methodString);
        for (var symbol : locarVars) {
            if (symbol.getName().equals(varName)) {
                return true;
            }
        }
        return false;
    }
}
