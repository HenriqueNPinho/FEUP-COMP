package jmm.astToJasmin;

public class AstToJasminReturn {

    public static String getJasminType(String jmmType) {
        switch (jmmType) {
            case "integer array":
            case "int":
                return "I";
            case "boolean":
                return "Z";
            case "void":
                return "V";
            default:
                return jmmType;
        }
    }
}
