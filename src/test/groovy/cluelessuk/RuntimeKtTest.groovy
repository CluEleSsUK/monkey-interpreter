package cluelessuk

import spock.lang.Specification

class RuntimeKtTest extends Specification {

    def evaluator = new MonkeyRuntime()
    def static True = new MBoolean(true)
    def static False = new MBoolean(false)
    def static Null = new Null()

    def "Single integer literal evaluates to itself"() {
        given:
        def input = "5;"
        def program = new Parser(new Lexer(input)).parseProgram()

        when:
        def result = evaluator.eval(program)

        then:
        result == new MInteger(5)
    }

    def "Single boolean literal evaluates to itself"() {
        given:
        def input = "true;"
        def program = new Parser(new Lexer(input)).parseProgram()

        when:
        def result = evaluator.eval(program)

        then:
        result == True
    }

    def "Bang operator negates and coerces to a boolean"(String input, MBoolean expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input     | expected
        "!true"   | False
        "!false"  | True
        "!5"      | False
        "!!true"  | True
        "!!false" | False
        "!!5"     | True
    }

    def "Negative integer expression evaluates to itself"() {
        given:
        def input = "-10"

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result == new MInteger(-10)
    }

    def "infix integer expressions evaluate to the correct integer literal"(String input, MInteger expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input            | expected
        "5 + 5"          | new MInteger(10)
        "5 - 5"          | new MInteger(0)
        "5 * 5"          | new MInteger(25)
        "5 + 5 * 5"      | new MInteger(30)
        "5 + 5 - 10 / 2" | new MInteger(5)
        "(5 + 5) / 2"    | new MInteger(5)
        "-5 * -5"        | new MInteger(25)

    }

    def "infix integer comparison expressions evaluate to the correct boolean literal"(String input, MBoolean expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input    | expected
        "1 < 2"  | True
        "1 > 2"  | False
        "2 > 1"  | True
        "1 > 2"  | False
        "1 == 1" | True
        "1 != 1" | False
        "1 == 2" | False
        "1 != 2" | True
    }

    def "infix boolean comparison expressions evaluate to the correct boolean literal"(String input, MBoolean expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input              | expected
        "true == true"     | True
        "false == false"   | True
        "true == false"    | False
        "true != false"    | True
        "false != true"    | True
        "(1 < 2) == true"  | True
        "(1 < 2) == false" | False
        "(1 > 2) == true"  | False
        "(1 > 2) == false" | True
    }

    def "if expressions return left if non-null or non-falsy, and right if null or falsy"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                           | expected
        "if (true) { 10 }"              | new MInteger(10)
        "if (false) { 10 }"             | Null
        "if (1) { 10 }"                 | new MInteger(10)
        "if (1 < 2) { 10 }"             | new MInteger(10)
        "if (1 > 2) { 10 }"             | Null
        "if (1 < 2) { 10 } else { 20 }" | new MInteger(10)
        "if (1 > 2) { 10 } else { 20 }" | new MInteger(20)
    }

    def "return statements evaluate expressions"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                 | expected
        "return 1;"           | new MReturnValue(new MInteger(1))
        "return 10; 9;"       | new MReturnValue(new MInteger(10))
        "return 2 * 5; 9;"    | new MReturnValue(new MInteger(10))
        "9; return 2 * 5; 9;" | new MReturnValue(new MInteger(10))
    }

    def "nested return statements don't return from outer scopes"() {
        given:
        def input = """
            if (1 < 2) {
              if (1 < 2) {
                return 10;
              }
                
              return 1;
            }
        """
        def program = new Parser(new Lexer(input)).parseProgram()

        when:
        def result = evaluator.eval(program)

        then:
        result == new MReturnValue(new MInteger(10))
    }

    def "Runtime returns errors for invalid evaluations, no matter where they happen"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                         | expected
        "1 + true"                    | new MError.TypeMismatch("${new MInteger(1)} + ${new MBoolean(true)}")
        "1 + true; return 9;"         | new MError.TypeMismatch("${new MInteger(1)} + ${new MBoolean(true)}")
        "-true"                       | new MError.UnknownOperator("-${new MBoolean(true)}")
        "true + false;"               | new MError.UnknownOperator("${new MBoolean(true)} + ${new MBoolean(false)}")
        "5; true + false; 5;"         | new MError.UnknownOperator("${new MBoolean(true)} + ${new MBoolean(false)}")
        "if (10 > 1) { true + true }" | new MError.UnknownOperator("${new MBoolean(true)} + ${new MBoolean(true)}")
        """
        if (10 > 1) {
          if (10 > 1) {
            return true + false;
          }
          return 1;
        }
        """                | new MError.UnknownOperator("${new MBoolean(true)} + ${new MBoolean(false)}")
    }

    def "Variable bindings return their value when evaluation, and an error if they are unbound"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                     | expected
        "let a = 5; a;"           | new MInteger(5)
        "let a = 5 * 5; a;"       | new MInteger(25)
        "let a = 5; let b = a; b" | new MInteger(5)
        """
        let a = 5;
        let b = a;
        let c = b + a + 5;
        c;
        """            | new MInteger(15)
        "a"                       | new MError.UnknownIdentifier("a")
    }

    def "Functions evaluate to the correct object representation"() {
        given:
        def input = "fn(x) { x + 2 };"
        def expectedIdentifier = new Identifier(new Token(Tokens.IDENT, "x"), "x")

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result instanceof MFunction
        def resultFunction = (MFunction) result
        resultFunction.parameters == [expectedIdentifier]
        resultFunction.body.statements.size() == 1
        resultFunction.body.statements.get(0) instanceof ExpressionStatement
        def expressionStatement = (ExpressionStatement) resultFunction.body.statements.get(0)
        expressionStatement.expression instanceof InfixExpression
    }

    def "Functions can be applied to values or expressions to return values"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                                              | expected
        "let identity = fn(x) { x; }; identity(5);"        | new MInteger(5)
        "let identity = fn(x) { return x; }; identity(5);" | new MInteger(5)
        "let double = fn(x) { x * 2 }; double(5);"         | new MInteger(10)
        "let add = fn(x, y) { x + y }; add(2, 3);"         | new MInteger(5)
        "let add = fn(x, y) { x + y }; add(2, add(2, 2));" | new MInteger(6)
        "fn(x) { x; }(5);"                                 | new MInteger(5)
    }

    def "Closures have their own scope"() {
        given:
        def input = """
            let adder = fn(x) {
                fn(y) { x + y };
            }
            
            let addTwo = adder(2);
            addTwo(2);
        """

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result == new MInteger(4)
    }

    def "String literals evaluate as the correct object representation"() {
        given:
        def input = """
            "hello world"
        """

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result == new MString("hello world")
    }

    def "String literals concatenate by appending"() {
        given:
        def input = """
            "hello" + " " + "world"
        """

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result == new MString("hello world")
    }

    def "Other operators for String return an UnknownOperator error"() {
        given:
        def input = """
            "hello" - "world"
        """

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result == new MError.UnknownOperator("${new MString("hello")} - ${new MString("world")}")
    }

    def "Array elements are evaluated on creation"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                    | expected
        "[1, 2]"                 | new MArray([new MInteger(1), new MInteger(2)])
        """[1 * 2, "a" + "b"]""" | new MArray([new MInteger(2), new MString("ab")])
        """[len("blah")]"""      | new MArray([new MInteger(4)])
    }

    def "Array elements can contain function literals as values"() {
        given:
        def input = """
            [fn (x) { x }, 1]
        """

        when:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        then:
        result instanceof MArray
        def resultArray = (MArray) result
        resultArray.elements.size() == 2
        resultArray.elements.get(0) instanceof MFunction
        resultArray.elements.get(1) == new MInteger(1)

        def resultFunction = (MFunction) resultArray.elements.get(0)
        resultFunction.parameters == [new Identifier(new Token(Tokens.IDENT, "x"), "x")]
        resultFunction.body == new BlockStatement(
                new Token(Tokens.LBRACE, "{"),
                [new ExpressionStatement(new Token(Tokens.IDENT, "x"), new Identifier(new Token(Tokens.IDENT, "x"), "x"))]
        )
    }
}
