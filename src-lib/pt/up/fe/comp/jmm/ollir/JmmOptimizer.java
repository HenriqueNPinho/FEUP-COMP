package pt.up.fe.comp.jmm.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR CODE:\n" + ollirCode);

        /*String ollirCode = "import io.a.b;\n" +
                "import c;\n" +
                "\n" +
                "public Fac extends Uni {\n" +
                "    .field number.i32;\n" +
                "    .field anotherNumber.i32;\n" +
                "    .field numArray.array.int;\n" +
                "    .method public compFac(num.i32).i32 {\n" +
                "        if ($1.num.i32 >=.bool 1.i32) goto else;\n" +
                "            num_aux.i32 :=.i32 1.i32;\n" +
                "            goto endif;\n" +
                "        else:\n" +
                "            aux1.i32 :=.i32 $1.num.i32 -.i32 1.i32;\n" +
                "            aux2.i32 :=.i32 invokevirtual(this, \"compFac\", aux1.i32).i32;\n" +
                "            num_aux.i32 :=.i32 $1.num.i32 *.i32 aux2.i32;\n" +
                "        endif:\n" +
                "            ret.i32 num_aux.i32;\n" +
                "    }\n" +
                "    .method public static main(args.array.String).V {\n" +
                "        aux1.Fac :=.Fac new(Fac).Fac;\n" +
                "        invokespecial(aux1.Fac,\"<init>\").V;\n" +
                "        aux2.i32 :=.i32 invokevirtual(aux1.Fac,\"compFac\",10.i32).i32;\n" +
                "        invokestatic(io, \"println\", aux2.i3).V;\n" +
                "    }\n" +
                "}\n";*/

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }
}
