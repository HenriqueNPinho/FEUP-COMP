package jmm.astToJasmin;

public class AstToJasminReturn {

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
            case "void":
                return "V";
            default:
                return jmmType;
        }
    }
}
