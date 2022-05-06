package pt.up.fe.comp.jmm.jasmin;

import pt.up.fe.comp.jmm.ollir.OllirResult;

/**
 * This Stage converts the OLLIR to Jasmin Bytecodes with optimizations performed at the AST level and at the OLLIR
 * level.<br>
 * Note that this step also for Checkpoint 2 (CP2), but only for code structures defined in the project description.
 */
public interface JasminBackend {

    /**
     * * Converts the OLLIR to Jasmin Bytecodes with optimizations performed at the AST level and at the OLLIR
     * level.<br>
     * Note that this step also for Checkpoint 2 (CP2), but only for code structures defined in the project description.
     * 
     * @param ollirResult
     * @return
     */
    JasminResult toJasmin(OllirResult ollirResult){
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class OLLIR TOOLS
            ollirClass.checkMethodLabels(); 
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs();
            ollirClass.buildVarTables(); // build the table of variables for each method
            ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = ""; 

            // Reports from this stage
            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode, reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }


}
