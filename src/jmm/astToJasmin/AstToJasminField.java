package jmm.astToJasmin;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class AstToJasminField {

    public static String getCode(Symbol symbol) {
        return symbol.getName() + " " + getCode(symbol.getType());
    }

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("[");
        }

        code.append(getOllirType(type.getName()));

        return code.toString();
    }

    public static String getOllirType(String jmmType) {
        switch (jmmType) {
            case "integer array":
            case "int":
                return "I";
            case "string array":
            case "string":
                return "Ljava/lang/String";
            case "boolean":
                return "B";
            default:
                return jmmType;
        }
    }
}
