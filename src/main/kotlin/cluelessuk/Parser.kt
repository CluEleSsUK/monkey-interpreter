package cluelessuk


typealias PrefixParseFun = () -> Expression?
typealias InfixParseFun = (Expression) -> Expression?

class Parser(var lexer: Lexer) {

    private var errors = listOf<String>()
    private val prefixParseFunctions = mapOf<TokenType, PrefixParseFun>(
        Tokens.IDENT to this::parseIdentifier,
        Tokens.INT to this::parseIntegerLiteral,
        Tokens.BANG to this::parsePrefixExpression,
        Tokens.MINUS to this::parsePrefixExpression,
        Tokens.TRUE to this::parseBoolean,
        Tokens.FALSE to this::parseBoolean,
        Tokens.LPAREN to this::parseGroupedExpression,
        Tokens.IF to this::parseIfExpression,
        Tokens.FUNCTION to this::parseFunctionExpression
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
        val expression = parseExpression(OperatorPrecedence.LOWEST) ?: return null
        consumeTokenAndAssertType(Tokens.SEMICOLON)

        return ReturnStatement(startToken, expression)
    }

    private fun parseIfExpression(): Expression? {
        val startToken = lexer.token ?: return null
        consumeTokenAndAssertType(Tokens.LPAREN) ?: return null
        val predicate = parseExpression(OperatorPrecedence.LOWEST) ?: return null
        consumeTokenAndAssertType(Tokens.RPAREN) ?: return null
        val trueBlock = parseBlockStatement() ?: return null

        if (peekToken()?.type == Tokens.ELSE) {
            consumeToken()
            val falseBlock = parseBlockStatement()
            return IfExpression(startToken, predicate, trueBlock, falseBlock)
        }

        return IfExpression(startToken, predicate, trueBlock, null)
    }

    private fun parseBlockStatement(): BlockStatement? {
        val startToken = consumeTokenAndAssertType(Tokens.LBRACE) ?: return null
        var statements = listOf<Statement>()

        while (peekToken()?.type != Tokens.RBRACE && peekToken()?.type != Tokens.EOF) {
            val statement = parseStatement()
            if (statement != null) {
                statements = statements.plus(statement)
            }
        }

        consumeTokenAndAssertType(Tokens.RBRACE)
        return BlockStatement(startToken, statements)
    }

    private fun parseFunctionExpression(): FunctionLiteral? {
        val startToken = lexer.token ?: return null
        consumeTokenAndAssertType(Tokens.LPAREN) ?: return null
        var parameters = listOf<Identifier>()

        while (peekToken()?.type == Tokens.IDENT || peekToken()?.type == Tokens.COMMA) {
            if (consumeToken()?.type == Tokens.COMMA) {
                continue
            }
            val nextParam = parseIdentifier()

            if (nextParam != null) {
                parameters = parameters.plus(nextParam)
            }
        }

        consumeTokenAndAssertType(Tokens.RPAREN) ?: return null
        val functionBlock = parseBlockStatement() ?: return null

        return FunctionLiteral(startToken, parameters, functionBlock)
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

    private fun parseBoolean(): BooleanLiteral? {
        val currentToken = lexer.token ?: return null

        return BooleanLiteral(currentToken, currentToken.type == Tokens.TRUE)
    }

    private fun parseGroupedExpression(): Expression? {
        val expression = parseExpression(OperatorPrecedence.LOWEST)
        consumeTokenAndAssertType(Tokens.RPAREN)
        return expression
    }

    private fun parseExpressionStatement(): ExpressionStatement? {
        val currentToken = peekToken() ?: return null
        val expression = parseExpression(OperatorPrecedence.LOWEST) ?: return null

        return ExpressionStatement(currentToken, expression)
    }

    private fun parseExpression(precedence: OperatorPrecedence): Expression? {
        val currentToken = consumeToken() ?: return null
        var leftExpression = prefixParseFunctions[currentToken.type]?.invoke() ?: return null
        var nextToken = peekToken()

        while (nextToken != null && nextToken.type != Tokens.SEMICOLON && precedence.ordinal < precedenceOf(nextToken).ordinal) {
            val infixFunc = infixParseFunctions[nextToken.type] ?: { null }
            leftExpression = infixFunc(leftExpression) ?: leftExpression
            nextToken = peekToken()
        }

        return leftExpression
    }

    private fun parsePrefixExpression(): PrefixExpression? {
        val initialToken = lexer.token ?: return null
        val nextExpression = parseExpression(OperatorPrecedence.PREFIX) ?: return null

        return PrefixExpression(initialToken, initialToken.literal, nextExpression)
    }

    private fun parseInfixExpression(left: Expression): InfixExpression? {
        val infixToken = consumeToken() ?: return null
        val right = parseExpression(precedenceOf(infixToken))

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
