package jmm.astToJasmin;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class AstToJasminParam {

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("[");
        }

        code.append(getJasminType(type.getName()));

        if (type.isArray()) {
            code.append(";");
        }

        return code.toString();
    }

    public static String getJasminType(String jmmType) {
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
