package cluelessuk

import spock.lang.Specification

class EvalKtTest extends Specification {

    def evaluator = new MonkeyRuntime()

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
        result == new MBoolean(true)
    }
}
