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
        if (peekToken()?.type == Tokens.EOF) {
            return statements
        }

        val nextStatement = parseStatement() ?: return parseProgram(statements)

        return parseProgram(statements.plus(nextStatement))
    }

    private fun parseStatement(): Statement? =
        when (peekToken()?.type) {
            Tokens.LET -> parseLetStatement()
            Tokens.RETURN -> parseReturnStatement()
            else -> parseExpressionStatement()
        }

    private fun parseLetStatement(): LetStatement? {
        val startToken = consumeToken()
        val identifierToken = consumeTokenAndAssertType(Tokens.IDENT)
        val assignToken = consumeTokenAndAssertType(Tokens.ASSIGN)
        val intToken = consumeTokenAndAssertType(Tokens.INT)

        if (startToken == null || identifierToken == null || assignToken == null || intToken == null) {
            return null
        }

        return LetStatement(startToken, Identifier(identifierToken, identifierToken.literal), IntExpr(intToken, intToken.literal))
    }

    private fun parseReturnStatement(): ReturnStatement? {
        val startToken = consumeToken() ?: return null

        while (lexer.token?.type != Tokens.SEMICOLON) {
            consumeToken()
        }

        return ReturnStatement(startToken, null)
    }

    private fun parseIdentifier(): Identifier? {
        return lexer.token?.let { Identifier(it, it.literal) } ?: return null
    }

    private fun parseIntegerLiteral(): IntegerLiteral? {
        val currentToken = lexer.token ?: return null

        return try {
            val asInt = currentToken.literal.toInt()
            IntegerLiteral(currentToken, asInt)
        } catch (ex: Exception) {
            raiseError("Could not parse ${lexer.token} as integer!")
            null
        }
    }

    private fun parseExpressionStatement(): ExpressionStatement? {
        val currentToken = peekToken() ?: return null
        val expression = parseExpression(OperatorPrecedence.LOWEST.ordinal) ?: return null

        return ExpressionStatement(currentToken, expression)
    }

    private fun parseExpression(precedence: Int): Expression? {
        val currentToken = consumeToken() ?: return null
        var leftExpression = prefixParseFunctions[currentToken.type]?.invoke() ?: return null
        var nextToken = peekToken()

        while (nextToken != null && nextToken.type != Tokens.SEMICOLON && precedence < precedenceOf(nextToken).ordinal) {
            val infixFunc = infixParseFunctions[nextToken.type] ?: { null }
            leftExpression = infixFunc(leftExpression) ?: leftExpression
            nextToken = peekToken()
        }

        return leftExpression
    }

    private fun parsePrefixExpression(): PrefixExpression? {
        val initialToken = lexer.token ?: return null
        val nextExpression = parseExpression(OperatorPrecedence.PREFIX.ordinal) ?: return null

        return PrefixExpression(initialToken, initialToken.literal, nextExpression)
    }

    private fun parseInfixExpression(left: Expression): InfixExpression? {
        val infixToken = consumeToken() ?: return null
        val right = parseExpression(precedenceOf(infixToken).ordinal)

        if (right == null) {
            raiseError("Expected right-hand expression for infix expression")
            return null
        }

        return InfixExpression(infixToken, left, infixToken.literal, right)
    }

    private fun peekToken(): Token? {
        return lexer.nextToken().token
    }

    private fun consumeToken(): Token? {
        lexer = lexer.nextToken()
        return lexer.token
    }

    private fun consumeTokenAndAssertType(tokenType: TokenType): Token? {
        val nextToken = consumeToken()
        if (nextToken?.type != tokenType) {
            raiseError("Expected next token to be ${Tokens.IDENT} but was ${nextToken?.type}")
            return null
        }
        return nextToken
    }

    private fun raiseError(message: String) {
        errors = errors.plus(message)
    }
}
