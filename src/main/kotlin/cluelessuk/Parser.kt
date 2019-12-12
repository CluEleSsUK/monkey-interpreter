package cluelessuk


typealias PrefixParseFun = () -> Expression?
typealias InfixParseFun = (Expression) -> Expression?

class Parser(var lexer: Lexer) {

    private var errors = listOf<String>()
    private val prefixParseFunctions = mapOf<TokenType, PrefixParseFun>(
        Tokens.IDENT to this::parseIdentifier,
        Tokens.INT to this::parseIntegerLiteral,
        Tokens.BANG to this::parsePrefixExpression,
        Tokens.MINUS to this::parsePrefixExpression
    )

    private val infixParseFunctions = mapOf<TokenType, InfixParseFun>(
        Tokens.ASTERISK to this::parseInfixExpression,
        Tokens.SLASH to this::parseInfixExpression,
        Tokens.PLUS to this::parseInfixExpression,
        Tokens.MINUS to this::parseInfixExpression,
        Tokens.EQ to this::parseInfixExpression,
        Tokens.NOT_EQ to this::parseInfixExpression,
        Tokens.GT to this::parseInfixExpression,
        Tokens.LT to this::parseInfixExpression
    )

    fun parseProgram(): Program {
        return Program(parseProgram(emptyList()), errors)
    }

    private tailrec fun parseProgram(statements: List<Statement>): List<Statement> {
        val currentToken = incrementLexerForToken()
        if (currentToken?.type == Tokens.EOF) {
            return statements
        }

        val nextStatement = parseStatement() ?: return parseProgram(statements)

        return parseProgram(statements.plus(nextStatement))
    }

    private fun parseStatement(): Statement? =
        when (lexer.token?.type) {
            Tokens.LET -> parseLetStatement()
            Tokens.RETURN -> parseReturnStatement()
            else -> parseExpressionStatement()
        }

    private fun parseLetStatement(): LetStatement? {
        val startToken = lexer.token ?: return null

        val identifierToken = incrementLexerForToken()
        if (identifierToken?.type != Tokens.IDENT) {
            raiseError(Tokens.IDENT, identifierToken?.type)
            return null
        }

        val assignmentToken = incrementLexerForToken()
        if (assignmentToken?.type != Tokens.ASSIGN) {
            raiseError(Tokens.ASSIGN, assignmentToken?.type)
            return null
        }

        val intToken = incrementLexerForToken()
        if (intToken?.type != Tokens.INT) {
            raiseError(Tokens.INT, intToken?.type)
            return null
        }

        return LetStatement(startToken, Identifier(identifierToken, identifierToken.literal), IntExpr(intToken, intToken.literal))
    }

    private fun parseReturnStatement(): ReturnStatement? {
        val startToken = lexer.token ?: return null

        while (lexer.token?.type != Tokens.SEMICOLON) {
            incrementLexerForToken()
        }

        return ReturnStatement(startToken, null)
    }

    private fun parseExpressionStatement(): ExpressionStatement? {
        val currentToken = lexer.token ?: return null
        val expression = parseExpression(OperatorPrecedence.LOWEST.ordinal) ?: return null

        return ExpressionStatement(currentToken, expression)
    }

    private fun parseExpression(precedence: Int): Expression? {
        var leftExpression = prefixParseFunctions[lexer.token?.type]?.invoke() ?: return null
        var nextToken = lexer.nextToken().token

        while (nextToken != null && nextToken.type != Tokens.SEMICOLON && precedence < precedenceOf(nextToken).ordinal) {
            val infixFunc = infixParseFunctions[nextToken.type] ?: { null }
            incrementLexerForToken()
            val nextLeft = infixFunc(leftExpression) ?: return leftExpression

            nextToken = lexer.nextToken().token
            leftExpression = nextLeft
        }

        return leftExpression
    }

    private fun parseIdentifier(): Identifier? {
        return lexer.token?.let { Identifier(it, it.literal) } ?: return null
    }

    private fun parseIntegerLiteral(): IntegerLiteral? {
        if (lexer.token == null) {
            return null
        }

        return try {
            val asInt = lexer.token!!.literal.toInt()
            IntegerLiteral(lexer.token!!, asInt)
        } catch (ex: Exception) {
            errors = errors.plus("Could not parse ${lexer.token} as integer!")
            null
        }
    }

    private fun parsePrefixExpression(): PrefixExpression? {
        val initialToken = lexer.token ?: return null
        incrementLexerForToken()
        val nextExpression = parseExpression(OperatorPrecedence.PREFIX.ordinal) ?: return null

        return PrefixExpression(initialToken, initialToken.literal, nextExpression)
    }

    private fun parseInfixExpression(left: Expression): InfixExpression? {
        val initialToken = lexer.token ?: return null
        incrementLexerForToken()
        val right = parseExpression(precedenceOf(initialToken).ordinal)

        if (right == null) {
            errors = errors.plus("Expected right-hand expression for infix expression")
            return null
        }

        return InfixExpression(initialToken, left, initialToken.literal, right)
    }

    private fun incrementLexerForToken(): Token? {
        lexer = lexer.nextToken()
        return lexer.token
    }

    private fun raiseError(expected: TokenType, actual: TokenType?) {
        val errorMessage = "Expected next token to be $expected but was $actual"
        errors = errors.plus(errorMessage)
    }
}
