package pt.up.fe.comp;

import java.util.Collections;
import java.util.Map;

import jmm.ast.LineColAnnotator;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParser;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;

/**
 * Copyright 2022 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class SimpleParser implements JmmParser {

    @Override
    public JmmParserResult parse(String jmmCode, Map<String, String> config) {

        try {

            JmmGrammarParser parser = new JmmGrammarParser(SpecsIo.toInputStream(jmmCode));
            parser.Start();

            var root = ((JmmNode) parser.rootNode());
            new LineColAnnotator().visit(root);
            System.out.println(root.sanitize().toTree());

            if (!(root instanceof JmmNode)) {
                return JmmParserResult.newError(new Report(ReportType.WARNING, Stage.SYNTATIC, -1,
                        "JmmNode interface not yet implemented, returning null root node"));
            }

            return new JmmParserResult((JmmNode) root, Collections.emptyList(), config);

        } catch (ParseException e) {
            // ... test if 'e' is null and handle 'ex' in a more generic way
            Token t = e.getToken();
            int line = t.getBeginLine();
            int column = t.getBeginColumn();
            String message = "Exception during parsing: " + t + " at line " + line + " and column " + column;
            Report report = Report.newError(Stage.SYNTATIC, line, column, message, e);
            return JmmParserResult.newError(report);
        } catch (Exception e) {
            return JmmParserResult.newError(Report.newError(Stage.SYNTATIC, -1, -1, "Exception during parsing", e));
        }
    }
}
