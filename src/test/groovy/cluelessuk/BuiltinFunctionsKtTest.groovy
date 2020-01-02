package cluelessuk

import spock.lang.Specification

class BuiltinFunctionsKtTest extends Specification {

    def evaluator = new MonkeyRuntime()

    def "len() function supports strings and arrays, returns error otherwise"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                            | expected
        'len("blah")'                    | new MInteger(4)
        'len("with space")'              | new MInteger(10)
        'len("")'                        | new MInteger(0)
        'len([])'                        | new MInteger(0)
        'len([1, "thing", fn(x) { x }])' | new MInteger(3)
        'len(1)'                         | new MError.TypeMismatch("len(${new MInteger(1)})")
        'len("thing", "thing2")'         | new MError.IncorrectNumberOfArgs(1, 2)
    }
}
