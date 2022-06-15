# Design and Implementation of the Java--language Compiler:From Java Source to Bytecodes

## GROUP: 5D

| Name        | Number    | E-Mail               | GRADE | CONTRIBUTION |
|-------------|-----------|----------------------|-------|--------------|
| João Pinho  | 201805000 | up201805000@fe.up.pt | 13    | 45%          |
| Vasco Alves | 201808031 | up201808031@fe.up.pt | 16    | 55%          |

GLOBAL Grade of the project: 13

It is important to mention that although the other two members of the group didn't work since
the start of the project, they only confirmed their abandonment on mid-to-late May, which means
we had to work for four until then.

**SUMMARY:**

We've implemented a compiler for the Java-- language, a subset of the popular Java programming
language, that generates valid JVM instructions in the jasmin format, which are then 
translated into Java bytecodes. 

This compiler includes syntactic and semantic analysis, and the jasmin code generation too.

We started by implementing the grammar for the Java-- language, solving problems such as ambiguity,
left recursion and operator precedence. We also implemented the error handling part, to better help
us with our debug during the grammar analysis, by adding the line and column to error messages.

Then we proceeded to build the AST, annotating the information  we considered to be relevant. 
On the AST we also added the line and column to every node, so that it was easier to identify
the problem when we had an error report during semantic analysis.

We then built the symbol table to save information related to the class, such as the class name
and its super class, imports, fields, methods and its parameters and variables.
To generate code, we visit the AST nodes and try to "translate" their information to jasmin syntax.

**SEMANTIC ANALYSIS:**
    
  - Super Class must be imported;
  - No Class Fields with the same name;
  - No Class Methods with the same name;
  - No Method Parameters with the same name;
  - No Local Variables with the same name;
  - No Local Variables with the same name as Parameters;
  - If Variable type is not primitive or file class, it must be imported;
  - Return Type must match the method's declared Return Type;
  - Variable in a Return Expression must exist;
  - And Operator must be used with boolean values;
  - Add/Sub/Div/Mul/Lower operators must be used with int values;
  - Keyword 'this' can not be used in a static method;
  - Method called with keyword 'this' must be a Class Method;
  - If the Variable that calls a method does not exist within the class, it must be imported (ex: io.print());
  - If the Variable that calls a method is of the file's Class type, the method must exist in the Class Methods;
  - Array Access must be performed on an Array;
  - Index when accessing an Array must be int;
  - Can not access Class Fields in static method;
  - Variables must exist to perform an assignment;
  - When assigning IntLiteral to a variable, variable must be of type int;
  - On Variable Assignment, types must be the same;
  - If a BinOp is in a Condition, it must be Lower or And operators;
  - Conditions must be of type boolean;
  - If a Method being called exists in the Class, arguments must match with its parameter declaration;
  - Array elements must be of the declared array type;
  - When assigning a value to an array index, assignment must be done on a variable of type array and index must be int;

We consider it is important to mention tried to implement as much semantic rules as we could,
implementing all the ones that were tested through the checkpoints and a few more we tested ourselves.
If there are some missing, it is most likely because we did not remember them, not because we didn't know how to do it.

**CODE GENERATION:**

We made a parser for this language(Java--) using the grammar that provided to us.
Then we correct the lexical and syntactic errors, such as left recursion, we create the AST with information in the nodes and leafs that will be useful to us, then, with
the AST done we create the Symbol Table and make a semantic Analysis.
Finally, we generated JVM instruction to be accepted by jasmin.

**PROS:**

  - Works great for arithmetic operation.


**CONS:**

  - Doesn't really work for anything other than that.
  - Code could be better written and organized.