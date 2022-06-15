# Design and Implementation of the Java--language Compiler:From Java Source to Bytecodes

## GROUP: 5D

| Name        | Number    | E-Mail               | GRADE | CONTRIBUTION |
|-------------|-----------|----------------------|-------|--------------|
| João Pinho  | 201805000 | up201805000@fe.up.pt | 15    | 45%          |
| Vasco Alves | 201808031 | up201808031@fe.up.pt | 18    | 55%          |

GLOBAL Grade of the project: 15


**SUMMARY:**

We've implemented a compiler of Java-- language to Java bytecodes. This compiler includes syntactic and semantic analysis, and Jasmin code generation too.


**SEMANTIC ANALYSIS:**

  - Operands must have the same type;
  - It is not possible to use arrays directly in arithmetic operations;
  - The array index on an array access must be an integer;
  - The assignee's value is the same as the assigned's;
  - Assumes that parameters are initialized;
  - The "target" of the method exists and contains the method 
  - The number of arguments in the invocation is equal to the number of parameters in the declaration;
  - The type of the parameters matches the type of the arguments;
  - The return type matches the method declaration return type;
  - The method is either imported, belongs to the class or its superclass;
  - The function type is the same as the return type;

**CODE GENERATION:**



**PROS:**



**CONS:**

Time was too short for the size of the project, plus we had to deal with half the team giving up, until they said they quit, we had to work for 4, which delayed everything.
So some features were not implemented.