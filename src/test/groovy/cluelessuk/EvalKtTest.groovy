package cluelessuk

import spock.lang.Specification

class EvalKtTest extends Specification {

    def evaluator = new MonkeyRuntime()

    def "Single integer literal returns itself"() {
        given:
        def input = "5;"
        def program = new Parser(new Lexer(input)).parseProgram()

        when:
        def result = evaluator.eval(program)

        then:
        result == new MInteger(5)
    }
}
