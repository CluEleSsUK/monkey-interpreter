package cluelessuk


typealias PrefixParseFun = () -> Expression?
typealias InfixParseFun = (Expression) -> Expression?

class Parser(var lexer: Lexer) {

    private var errors = listOf<String>()
    private val prefixParseFunctions = mapOf<TokenType, PrefixParseFun>(
        Tokens.IDENT to this::parseIdentifier,
        Tokens.INT to this::parseIntegerLiteral,
        Tokens.STRING to this::parseStringLiteral,
        Tokens.BANG to this::parsePrefixExpression,
        Tokens.MINUS to this::parsePrefixExpression,
        Tokens.TRUE to this::parseBoolean,
        Tokens.FALSE to this::parseBoolean,
        Tokens.LPAREN to this::parseGroupedExpression,
        Tokens.IF to this::parseIfExpression,
        Tokens.FUNCTION to this::parseFunctionExpression,
        Tokens.LBRACKET to this::parseArrayExpression,
        Tokens.LBRACE to this::parseMapExpression
    )

    private val infixParseFunctions = mapOf<TokenType, InfixParseFun>(
        Tokens.ASTERISK to this::parseInfixExpression,
        Tokens.SLASH to this::parseInfixExpression,
        Tokens.PLUS to this::parseInfixExpression,
        Tokens.MINUS to this::parseInfixExpression,
        Tokens.EQ to this::parseInfixExpression,
        Tokens.NOT_EQ to this::parseInfixExpression,
        Tokens.GT to this::parseInfixExpression,
        Tokens.LT to this::parseInfixExpression,
        Tokens.LPAREN to this::parseFunctionCall,
        Tokens.LBRACKET to this::parseIndexExpression
    )

    fun parseProgram(): Program {
        return Program(parseProgram(emptyList()), errors)
    }

    private tailrec fun parseProgram(statements: List<Statement>): List<Statement> {
        if (!lexer.hasMore()) {
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
        val expression = parseExpression(OperatorPrecedence.LOWEST)

        if (startToken == null || identifierToken == null || assignToken == null || expression == null) {
            return null
        }

        return LetStatement(startToken, Identifier(identifierToken, identifierToken.literal), expression)
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

        while (peekToken()?.type != Tokens.RBRACE && !endOfFileOrError()) {
            parseStatement()?.let {
                statements = statements.plus(it)
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
            parseIdentifier()?.let {
                parameters = parameters.plus(it)
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

    private fun parseStringLiteral(): StringLiteral? {
        val currentToken = lexer.token ?: return null

        return StringLiteral(currentToken, currentToken.literal)
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

    private fun parseFunctionCall(left: Expression): CallExpression? {
        val startToken = consumeTokenAndAssertType(Tokens.LPAREN) ?: return null
        var args = listOf<Expression>()

        while (peekToken()?.type != Tokens.RPAREN && !endOfFileOrError()) {
            if (peekToken()?.type == Tokens.COMMA) {
                consumeToken()
                continue
            }

            val arg = parseExpression(OperatorPrecedence.LOWEST) ?: return null
            args = args.plus(arg)
        }
        consumeTokenAndAssertType(Tokens.RPAREN) ?: return null

        return CallExpression(startToken, left, args)
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

        while (
            nextToken != null &&
            nextToken.type != Tokens.SEMICOLON &&
            precedence.ordinal < precedenceOf(nextToken).ordinal &&
            !endOfFileOrError()
        ) {
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

    private fun parseArrayExpression(): ArrayLiteral? {
        val startToken = lexer.token ?: return null
        var elements = listOf<Expression>()

        while (peekToken()?.type != Tokens.RBRACKET && !endOfFileOrError()) {
            if (peekToken()?.type == Tokens.COMMA) {
                consumeToken()
                continue
            }

            val element = parseExpression(OperatorPrecedence.LOWEST)
            if (element == null) {
                // parser has likely errored, but
                consumeToken()
            } else {
                elements = elements.plus(element)
            }
        }

        consumeTokenAndAssertType(Tokens.RBRACKET) ?: return null
        return ArrayLiteral(startToken, elements)
    }

    private fun parseIndexExpression(left: Expression): IndexExpression? {
        val infixToken = consumeTokenAndAssertType(Tokens.LBRACKET) ?: return null
        val indexExpression = parseExpression(OperatorPrecedence.LOWEST)

        if (indexExpression == null) {
            raiseError("Cannot parse an index expression without an index")
            return null
        }

        consumeTokenAndAssertType(Tokens.RBRACKET) ?: return null
        return IndexExpression(infixToken, left, indexExpression)
    }

    private fun parseMapExpression(): MapLiteral? {
        val startToken = lexer.token ?: return null
        var values = mapOf<Expression, Expression>()

        while (peekToken()?.type != Tokens.RBRACE && !endOfFileOrError()) {
            if (peekToken()?.type == Tokens.COMMA) {
                consumeToken()
                continue
            }

            val key = parseExpression(OperatorPrecedence.LOWEST)
            val colon = consumeTokenAndAssertType(Tokens.COLON)
            val value = parseExpression(OperatorPrecedence.LOWEST)
            if (key == null || colon == null || value == null) {
                return null
            }

            values = values.plus(key to value)
        }

        return MapLiteral(startToken, values)
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
            raiseError("Expected next token to be $tokenType but was ${nextToken?.type}")
            return null
        }
        return nextToken
    }

    private fun endOfFileOrError(): Boolean {
        return peekToken()?.type == Tokens.EOF || errors.isNotEmpty()
    }

    private fun raiseError(message: String) {
        errors = errors.plus(message)
    }
}
