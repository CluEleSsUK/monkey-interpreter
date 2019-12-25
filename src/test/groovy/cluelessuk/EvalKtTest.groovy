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
}
