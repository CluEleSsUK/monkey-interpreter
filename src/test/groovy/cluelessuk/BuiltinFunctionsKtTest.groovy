package cluelessuk

import spock.lang.Specification

import java.rmi.MarshalException

class BuiltinFunctionsKtTest extends Specification {

    def evaluator = new MonkeyRuntime()
    def static Null = new Null()

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

    def "first() function only works for arrays, returns null if array is empty"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input              | expected
        'first()'          | new MError.IncorrectNumberOfArgs(1, 0)
        'first("blah")'    | new MError.TypeMismatch("Function does not support type STRING")
        'first([])'        | Null
        'first([0, 1, 2])' | new MInteger(0)
    }

    def "last() function only works for arrays, returns null if array is empty"() {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input             | expected
        'last()'          | new MError.IncorrectNumberOfArgs(1, 0)
        'last("blah")'    | new MError.TypeMismatch("Function does not support type STRING")
        'last([])'        | Null
        'last([0, 1, 2])' | new MInteger(2)
    }

    def "rest() function returns everything apart from the first item in an array"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input                   | expected
        'rest()'                | new MError.IncorrectNumberOfArgs(1, 0)
        'rest("blah")'          | new MError.TypeMismatch("Function does not support type STRING")
        'rest([])'              | Null
        'rest([1])'             | new MArray([])
        'rest([0, 1, 2])'       | new MArray([new MInteger(1), new MInteger(2)])
        'rest([0, "blah", []])' | new MArray([new MString("blah"), new MArray([])])
    }

    def "push() function creates a new array with a new item added"(String input, MObject expected) {
        given:
        def result = evaluator.eval(new Parser(new Lexer(input)).parseProgram())

        expect:
        result == expected

        where:
        input               | expected
        'push()'            | new MError.IncorrectNumberOfArgs(2, 0)
        'push([])'          | new MError.IncorrectNumberOfArgs(2, 1)
        'push([], 1)'       | new MArray([new MInteger(1)])
        'push(1, [])'       | new MError.TypeMismatch("Expected ARRAY, got INTEGER")
        'push([0], 1)'      | new MArray([new MInteger(0), new MInteger(1)])
        'push(["blah"], 1)' | new MArray([new MString("blah"), new MInteger(1)])
        'push([1], "blah")' | new MArray([new MInteger(1), new MString("blah")])
        'push([1], [1])'    | new MArray([new MInteger(1), new MArray([new MInteger(1)])])
    }
}
