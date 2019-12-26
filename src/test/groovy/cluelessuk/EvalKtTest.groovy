package cluelessuk

import spock.lang.Specification

class EvalKtTest extends Specification {

    def evaluator = new MonkeyRuntime()
    def static True = new MBoolean(true)
    def static False = new MBoolean(false)

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

    def "infix boolean expressions evaluate to the correct boolean literal"(String input, MBoolean expected) {
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
}
