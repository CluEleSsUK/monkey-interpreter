package cluelessuk

class LexerTest extends spock.lang.Specification {


    def "basic syntax tokens return the correct representation"() {
        given:
        def lexer = new Lexer("=+(){},;")

        expect:
        nextAndTest(lexer.nextToken(), new Token(Tokens.ASSIGN, "="))
        nextAndTest(lexer.nextToken().nextToken(), new Token(Tokens.PLUS, "+"))
        nextAndTest(lexer.nextToken().nextToken().nextToken(), new Token(Tokens.LPAREN, "("))
        nextAndTest(lexer.nextToken().nextToken().nextToken().nextToken(), new Token(Tokens.RPAREN, ")"))
        nextAndTest(lexer.nextToken().nextToken().nextToken().nextToken().nextToken(), new Token(Tokens.LBRACE, "{"))
        nextAndTest(lexer.nextToken().nextToken().nextToken().nextToken().nextToken().nextToken(), new Token(Tokens.RBRACE, "}"))
        nextAndTest(lexer.nextToken().nextToken().nextToken().nextToken().nextToken().nextToken().nextToken(), new Token(Tokens.COMMA, ","))
        nextAndTest(lexer.nextToken().nextToken().nextToken().nextToken().nextToken().nextToken().nextToken().nextToken(), new Token(Tokens.SEMICOLON, ";"))

    }

    void nextAndTest(Lexer lexer, Token comparison) {
        assert lexer.token == comparison
    }

    def "variables, keywords and integers return the correct representation"() {
        given:
        def input = """
            let five = 5;
            let ten = 10;
            let add = fn(x, y) {
                x + y;
            };
            let result = add(five, ten);
        """

        def expectedResult = [
                new Token(Tokens.LET, "let"), new Token(Tokens.IDENT, "five"), new Token(Tokens.ASSIGN, "="), new Token(Tokens.INT, "5"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.LET, "let"), new Token(Tokens.IDENT, "ten"), new Token(Tokens.ASSIGN, "="), new Token(Tokens.INT, "10"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.LET, "let"), new Token(Tokens.IDENT, "add"), new Token(Tokens.ASSIGN, "="),
                new Token(Tokens.FUNCTION, "fn"), new Token(Tokens.LPAREN, "("), new Token(Tokens.IDENT, "x"), new Token(Tokens.COMMA, ","), new Token(Tokens.IDENT, "y"), new Token(Tokens.RPAREN, ")"),
                new Token(Tokens.LBRACE, "{"), new Token(Tokens.IDENT, "x"), new Token(Tokens.PLUS, "+"), new Token(Tokens.IDENT, "y"), new Token(Tokens.SEMICOLON, ";"), new Token(Tokens.RBRACE, "}"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.LET, "let"), new Token(Tokens.IDENT, "result"), new Token(Tokens.ASSIGN, "="), new Token(Tokens.IDENT, "add"),
                new Token(Tokens.LPAREN, "("), new Token(Tokens.IDENT, "five"), new Token(Tokens.COMMA, ","), new Token(Tokens.IDENT, "ten"), new Token(Tokens.RPAREN, ")"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.EOF, "")
        ]

        when:
        def actual = readAll(new Lexer(input))

        then:
        actual == expectedResult
    }

    def "mathematical symbols return the correct tokens"() {
        given:
        def input = """
           !-/*5;
           5 < 10 > 5;
        """
        def expectedResult = [
                new Token(Tokens.BANG, "!"), new Token(Tokens.MINUS, "-"), new Token(Tokens.SLASH, "/"), new Token(Tokens.ASTERISK, "*"), new Token(Tokens.INT, "5"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.INT, "5"), new Token(Tokens.LT, "<"), new Token(Tokens.INT, "10"), new Token(Tokens.GT, ">"), new Token(Tokens.INT, "5"), new Token(Tokens.SEMICOLON, ";"), new Token(Tokens.EOF, "")
        ]

        when:
        def actual = readAll(new Lexer(input))

        then:
        actual == expectedResult
    }

    def "control flow keywords return the correct tokens"() {
        given:
        def input = """
            if (true) {
              return false;
            } else {
              return true;
            }
        """
        def expectedResult = [
                new Token(Tokens.IF, "if"), new Token(Tokens.LPAREN, "("), new Token(Tokens.TRUE, "true"), new Token(Tokens.RPAREN, ")"), new Token(Tokens.LBRACE, "{"),
                new Token(Tokens.RETURN, "return"), new Token(Tokens.FALSE, "false"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.RBRACE, "}"), new Token(Tokens.ELSE, "else"), new Token(Tokens.LBRACE, "{"),
                new Token(Tokens.RETURN, "return"), new Token(Tokens.TRUE, "true"), new Token(Tokens.SEMICOLON, ";"),
                new Token(Tokens.RBRACE, "}"), new Token(Tokens.EOF, "")
        ]

        when:
        def actual = readAll(new Lexer(input))

        then:
        actual == expectedResult
    }

    def "multi-character tokens return the correct tokens"() {
        given:
        def input = "!= =="

        def expectedResult = [new Token(Tokens.NOT_EQ, "!="), new Token(Tokens.EQ, "=="), new Token(Tokens.EOF, "")]

        when:
        def actual = readAll(new Lexer(input))

        then:
        actual == expectedResult
    }

    private static def readAll(Lexer lexer) {
        def output = []
        while (lexer.hasMore()) {
            lexer = lexer.nextToken()

            if (lexer.token != null) {
                output.add(lexer.token)
            }
        }
        return output
    }
}
