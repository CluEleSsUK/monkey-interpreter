package cluelessuk

import spock.lang.Specification

class ParserTest extends Specification {

    def "multiple let statements are parsed the correct values"() {
        given:
        def input = """
            let x = 5;
            let y = 10;
            let blah = 10101010;
        """
        def expectedProgram = new Program([
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "x"), "x"), new IntExpr(new Token(Tokens.INT, "5"), "5")),
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "y"), "y"), new IntExpr(new Token(Tokens.INT, "10"), "10")),
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "blah"), "blah"), new IntExpr(new Token(Tokens.INT, "10101010"), "10101010")),
        ])

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        program == expectedProgram
        !program.hasErrors()
    }

    def "return statement returns everything up to next semicolon"() {
        given:
        def input = """
            return 10;
        """
        def expectedProgram = new Program([
                new ReturnStatement(new Token(Tokens.RETURN, "return"), null)
        ])

        when:
        def actualProgram = new Parser(new Lexer(input)).parseProgram()

        then:
        !actualProgram.hasErrors()
        actualProgram == expectedProgram
    }

    def "identifier expressions on their own are parsed correctly"() {
        given:
        def input = "foobar;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 1
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.IDENT, "foobar"), new Identifier(new Token(Tokens.IDENT, "foobar"), "foobar"))
    }

    def "int expressions on their own are parsed correctly"() {
        given:
        def input = "5;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 1
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.INT, "5"), new IntegerLiteral(new Token(Tokens.INT, "5"), 5))
    }
}
