package jmm.astToJasmin;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class AstToJasminParam {

    public static String getCode(Type type) {
        StringBuilder code = new StringBuilder();

        if (type.isArray()) {
            code.append("[");
        }

        code.append(getJasminType(type.getName()));

        return code.toString();
    }

    public static String getJasminType(String jmmType) {
        switch (jmmType) {
            case "integer array":
            case "int":
                return "I";
            case "string array":
                return "Ljava/lang/String;";
            case "boolean":
                return "Z";
            default:
                return jmmType;
        }
    }

}
