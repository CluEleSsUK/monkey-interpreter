package cluelessuk

import spock.lang.Specification

class EvalKtTest extends Specification {

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
}
