package cluelessuk

import spock.lang.Specification

class ParserTest extends Specification {

    def "multiple let statements are parsed the correct values"() {
        given:
        def input = """
            let x = 5;
            let y = 10;
            let parseExpression = 10101010;
        """
        def expectedProgram = new Program([
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "x"), "x"), new IntegerLiteral(new Token(Tokens.INT, "5"), 5)),
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "y"), "y"), new IntegerLiteral(new Token(Tokens.INT, "10"), 10)),
                new LetStatement(new Token(Tokens.LET, "let"), new Identifier(new Token(Tokens.IDENT, "parseExpression"), "parseExpression"), new IntegerLiteral(new Token(Tokens.INT, "10101010"), 10101010)),
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
                new ReturnStatement(
                        new Token(Tokens.RETURN, "return"),
                        new IntegerLiteral(new Token(Tokens.INT, "10"), 10)
                )
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

    def "multiple expressions are parsed correctly"() {
        given:
        def input = "5; 1;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 2
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.INT, "5"), new IntegerLiteral(new Token(Tokens.INT, "5"), 5))
        program.statements.get(1) == new ExpressionStatement(new Token(Tokens.INT, "1"), new IntegerLiteral(new Token(Tokens.INT, "1"), 1))
    }

    def "Long integer literals don't bomb out"() {
        given:
        def input = "500000;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 1
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.INT, "500000"), new IntegerLiteral(new Token(Tokens.INT, "500000"), 500000))
    }

    def "prefix expressions are parsed and typed correctly"() {
        given:
        def input = "-5;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 1
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.MINUS, "-"), new PrefixExpression(new Token(Tokens.MINUS, "-"), "-", new IntegerLiteral(new Token(Tokens.INT, "5"), 5)))
    }

    def "simple int infix expressions are parsed and typed as such"() {
        given:
        def input = "10 + 5;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 1
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.INT, "10"),
                new InfixExpression(
                        new Token(Tokens.PLUS, "+"),
                        new IntegerLiteral(new Token(Tokens.INT, "10"), 10),
                        "+",
                        new IntegerLiteral(new Token(Tokens.INT, "5"), 5)
                )
        )
    }

    def "mixed fix expressions all print correctly bracketed strings"(String input, String expected) {
        def program = new Parser(new Lexer(input)).parseProgram()

        expect:
        program.toString() == expected

        where:
        input                   | expected
        "!-a"                   | "(! (- a))"
        "a + b - c"             | "((a + b) - c)"
        "-a + b"                | "((- a) + b)"
        "a + b * c"             | "(a + (b * c))"
        "a + b * c + d / e - f" | "(((a + (b * c)) + (d / e)) - f)"
    }

    def "boolean literals are parsed as booleans"() {
        given:
        def input = "true; false; true;"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.size() == 3
        program.statements.get(0) == new ExpressionStatement(new Token(Tokens.TRUE, "true"), new BooleanLiteral(new Token(Tokens.TRUE, "true"), true))
        program.statements.get(1) == new ExpressionStatement(new Token(Tokens.FALSE, "false"), new BooleanLiteral(new Token(Tokens.FALSE, "false"), false))
        program.statements.get(2) == new ExpressionStatement(new Token(Tokens.TRUE, "true"), new BooleanLiteral(new Token(Tokens.TRUE, "true"), true))
    }

    def "grouped expressions remain grouped"() {
        given:
        def input = "(5 + 5) * 10"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.toString() == "((5 + 5) * 10)"
    }

    def "lone if statement has a block statement"() {
        given:
        def input = "if (x < y) { x }"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.IF, "if"),
                new IfExpression(
                        new Token(Tokens.IF, "if"),
                        new InfixExpression(
                                new Token(Tokens.LT, "<"),
                                new Identifier(new Token(Tokens.IDENT, "x"), "x"),
                                "<",
                                new Identifier(new Token(Tokens.IDENT, "y"), "y")
                        ),
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(new Token(Tokens.IDENT, "x"), new Identifier(new Token(Tokens.IDENT, "x"), "x"))]
                        ),
                        null
                )
        )
    }

    def "if statement with else has block statements for both"() {
        given:
        def input = """
            if (x < y) { 
                x 
            } else {
               y
            }
            
        """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.IF, "if"),
                new IfExpression(
                        new Token(Tokens.IF, "if"),
                        new InfixExpression(
                                new Token(Tokens.LT, "<"),
                                new Identifier(new Token(Tokens.IDENT, "x"), "x"),
                                "<",
                                new Identifier(new Token(Tokens.IDENT, "y"), "y")
                        ),
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(new Token(Tokens.IDENT, "x"), new Identifier(new Token(Tokens.IDENT, "x"), "x"))]
                        ),
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(new Token(Tokens.IDENT, "y"), new Identifier(new Token(Tokens.IDENT, "y"), "y"))]
                        )
                )
        )
    }

    def "function literals with no parameters contain a block function and empty parameters"() {
        given:
        def input = """
            fn() {
                1;
            }
        """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.FUNCTION, "fn"),
                new FunctionLiteral(
                        new Token(Tokens.FUNCTION, "fn"),
                        [],
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(
                                        new Token(Tokens.INT, "1"),
                                        new IntegerLiteral(new Token(Tokens.INT, "1"), 1)
                                )]
                        )
                )
        )
    }

    def "function literal with a single argument contains the correct parameter"() {
        given:
        def input = """
            fn(x) {
                x;
            }
        """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.FUNCTION, "fn"),
                new FunctionLiteral(
                        new Token(Tokens.FUNCTION, "fn"),
                        [new Identifier(new Token(Tokens.IDENT, "x"), "x")],
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(new Token(Tokens.IDENT, "x"), new Identifier(new Token(Tokens.IDENT, "x"), "x"))]
                        )
                )
        )
    }

    def "function literals with multiple arguments contain the correct parameters"() {
        given:
        def input = """
            fn(x, y) {
                x + y;
            }
        """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.FUNCTION, "fn"),
                new FunctionLiteral(
                        new Token(Tokens.FUNCTION, "fn"),
                        [new Identifier(new Token(Tokens.IDENT, "x"), "x"), new Identifier(new Token(Tokens.IDENT, "y"), "y")],
                        new BlockStatement(
                                new Token(Tokens.LBRACE, "{"),
                                [new ExpressionStatement(
                                        new Token(Tokens.IDENT, "x"),
                                        new InfixExpression(
                                                new Token(Tokens.PLUS, "+"),
                                                new Identifier(new Token(Tokens.IDENT, "x"), "x"),
                                                "+",
                                                new Identifier(new Token(Tokens.IDENT, "y"), "y"),
                                        ))]
                        )
                )
        )
    }

    def "function call expression nested sub-expressions as args"() {
        given:
        def input = """
           someFun(2 + 2, 5 * 5, 1);
        """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.IDENT, "someFun"),
                new CallExpression(
                        new Token(Tokens.LPAREN, "("),
                        new Identifier(new Token(Tokens.IDENT, "someFun"), "someFun"),
                        [
                                new InfixExpression(
                                        new Token(Tokens.PLUS, "+"),
                                        new IntegerLiteral(new Token(Tokens.INT, "2"), 2),
                                        "+",
                                        new IntegerLiteral(new Token(Tokens.INT, "2"), 2)
                                ),
                                new InfixExpression(
                                        new Token(Tokens.ASTERISK, "*"),
                                        new IntegerLiteral(new Token(Tokens.INT, "5"), 5),
                                        "*",
                                        new IntegerLiteral(new Token(Tokens.INT, "5"), 5)
                                ),
                                new IntegerLiteral(new Token(Tokens.INT, "1"), 1)
                        ]
                )
        )
    }

    def "String literal expression parses to the correct representation"() {
        given:
        def input = """ "Hello world" """

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        !program.hasErrors()
        program.statements.get(0) == new ExpressionStatement(
                new Token(Tokens.STRING, "Hello world"),
                new StringLiteral(new Token(Tokens.STRING, "Hello world"), "Hello world")
        )
    }

    def "Array literals are parsed regardless of elements"(String input, ArrayLiteral expected) {
        given:
        def program = new Parser(new Lexer(input)).parseProgram()

        expect:
        def expression = oneExpressionOrError(program)
        expression.expression == expected

        where:
        input                    | expected
        "[]"                     | new ArrayLiteral(new Token(Tokens.LBRACKET, "["), [])
        "[1]"                    | new ArrayLiteral(new Token(Tokens.LBRACKET, "["), [new IntegerLiteral(new Token(Tokens.INT, "1"), 1)])
        "[1, 2]"                 | new ArrayLiteral(new Token(Tokens.LBRACKET, "["), [new IntegerLiteral(new Token(Tokens.INT, "1"), 1), new IntegerLiteral(new Token(Tokens.INT, "2"), 2)])
        "[1, true, \"blah\"]"    | new ArrayLiteral(new Token(Tokens.LBRACKET, "["), [new IntegerLiteral(new Token(Tokens.INT, "1"), 1), new BooleanLiteral(new Token(Tokens.TRUE, "true"), true), new StringLiteral(new Token(Tokens.STRING, "blah"), "blah")])
        "[1 * 2, \"a\" + \"b\"]" | new ArrayLiteral(new Token(Tokens.LBRACKET, "["), [
                new InfixExpression(new Token(Tokens.ASTERISK, "*"),
                        new IntegerLiteral(new Token(Tokens.INT, "1"), 1),
                        "*",
                        new IntegerLiteral(new Token(Tokens.INT, "2"), 2)
                ),
                new InfixExpression(new Token(Tokens.PLUS, "+"),
                        new StringLiteral(new Token(Tokens.STRING, "a"), "a"),
                        "+",
                        new StringLiteral(new Token(Tokens.STRING, "b"), "b"),
                )])
    }

    def "Array literals that aren't terminated blow up"() {
        given:
        def input = "[1, 2, 3"

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        program.hasErrors()
    }

    def "Index expressions are parsed and account for operator precedence"() {
        given:
        def input = "myArray[1 + 1 * 2]"
        def expected = new IndexExpression(
                new Token(Tokens.LBRACKET, "["),
                new Identifier(new Token(Tokens.IDENT, "myArray"), "myArray"),
                new InfixExpression(
                        new Token(Tokens.PLUS, "+"),
                        new IntegerLiteral(new Token(Tokens.INT, "1"), 1), "+",
                        new InfixExpression(
                                new Token(Tokens.ASTERISK, "*"),
                                new IntegerLiteral(new Token(Tokens.INT, "1"), 1),
                                "*",
                                new IntegerLiteral(new Token(Tokens.INT, "2"), 2),
                        )
                )
        )

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        def expression = oneExpressionOrError(program)
        expression.expression == expected
    }

    def "Map literals parse anything as key or value (even things that should not evaluate)"() {
        given:
        def input = '{ "thing": 1, true: "blah", fn(x) { x }: 1 + 1 }'
        def expected = new MapLiteral(
                new Token(Tokens.LBRACE, "{"),
                [
                        (new StringLiteral(new Token(Tokens.STRING, "thing"), "thing"))                                                                                                                                                                                                            : new IntegerLiteral(new Token(Tokens.INT, "1"), 1),
                        (new BooleanLiteral(new Token(Tokens.TRUE, "true"), true))                                                                                                                                                                                                                 : new StringLiteral(new Token(Tokens.STRING, "blah"), "blah"),
                        (new FunctionLiteral(new Token(Tokens.FUNCTION, "fn"), [new Identifier(new Token(Tokens.IDENT, "x"), "x")], new BlockStatement(new Token(Tokens.LBRACE, "{"), [new ExpressionStatement(new Token(Tokens.IDENT, "x"), new Identifier(new Token(Tokens.IDENT, "x"), "x"))]))): new InfixExpression(new Token(Tokens.PLUS, "+"), new IntegerLiteral(new Token(Tokens.INT, "1"), 1), "+", new IntegerLiteral(new Token(Tokens.INT, "1"), 1))
                ]
        )

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        oneExpressionOrError(program).expression == expected
    }

    def "Empty map literals are parsed as map literals"() {
        given:
        def input = "{}"
        def expected = new MapLiteral(new Token(Tokens.LBRACE, "{"), [:])

        when:
        def program = new Parser(new Lexer(input)).parseProgram()

        then:
        def expression = oneExpressionOrError(program)
        expression.expression == expected

    }

    ExpressionStatement oneExpressionOrError(Program program) {
        if (program.hasErrors()) {
            throw new RuntimeException("Program has errors: ${program.errors}")
        }

        if (!(program.getStatements().get(0) instanceof ExpressionStatement)) {
            throw new RuntimeException("Program first statement is not an expression")
        }

        return (ExpressionStatement) program.getStatements().get(0)
    }
}
