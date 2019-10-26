package cluelessuk


data class ParserState(val currentPosition: Lexer, val statements: List<Statement>)
typealias PrefixParseFun = (Lexer) -> Expression?
typealias InfixParseFun = (Lexer, Expression) -> Expression?

class Parser(private val lexer: Lexer) {

    private var errors = listOf<String>()
    private val prefixParseFunctions = mapOf<TokenType, PrefixParseFun>(
        Tokens.IDENT to this::parseIdentifier
    )
    private val infixParseFunctions = mapOf<TokenType, InfixParseFun>()

    fun parseProgram(): Program {
        val state = parseProgram(ParserState(lexer, emptyList()))
        return Program(state.statements, errors)
    }

    private tailrec fun parseProgram(state: ParserState): ParserState {
        val (nextLexer, currentToken) = incrementForNext(state.currentPosition)
        if (currentToken?.type == Tokens.EOF) {
            return state
        }

        val (updatedLexer, statement) = parseStatement(nextLexer)

        if (statement == null) {
            return parseProgram(state.copy(currentPosition = updatedLexer))
        }

        return parseProgram(ParserState(updatedLexer, state.statements.plus(statement)))
    }

    private fun parseStatement(lexer: Lexer): Pair<Lexer, Statement?> =
        when (lexer.token?.type) {
            Tokens.LET -> parseLetStatement(lexer)
            Tokens.RETURN -> parseReturnStatement(lexer)
            else -> parseExpressionStatement(lexer)
        }

    private fun parseLetStatement(lexer: Lexer): Pair<Lexer, LetStatement?> {
        val startToken = lexer.token

        val (nextLexer, identifierToken) = incrementForNext(lexer)
        if (identifierToken?.type != Tokens.IDENT) {
            raiseError(Tokens.IDENT, identifierToken?.type)
            return nextLexer to null
        }

        val (nextLexerAgain, assignmentToken) = incrementForNext(nextLexer)
        if (assignmentToken?.type != Tokens.ASSIGN) {
            raiseError(Tokens.ASSIGN, assignmentToken?.type)
            return nextLexerAgain to null
        }

        val (lastLexer, intToken) = incrementForNext(nextLexerAgain)
        if (intToken?.type != Tokens.INT) {
            raiseError(Tokens.INT, intToken?.type)
            return lastLexer to null
        }

        return lastLexer to LetStatement(startToken!!, Identifier(identifierToken, identifierToken.literal), IntExpr(intToken, intToken.literal))
    }

    private fun parseReturnStatement(lexer: Lexer): Pair<Lexer, ReturnStatement?> {
        val startToken = lexer.token
        var currentLexer = lexer
        var currentToken = startToken

        while (currentToken?.type != Tokens.SEMICOLON) {
            val (nextLexer, nextToken) = incrementForNext(currentLexer)
            currentLexer = nextLexer
            currentToken = nextToken
        }

        return currentLexer to ReturnStatement(startToken!!, null)
    }

    private fun parseExpressionStatement(lexer: Lexer): Pair<Lexer, ExpressionStatement?> {
        val currentToken = lexer.token ?: return lexer to null
        val expression = parseExpression(lexer, OperatorPrecedence.LOWEST.ordinal) ?: return lexer to null

        val expr = ExpressionStatement(currentToken, expression)

        if (lexer.hasMore() && lexer.nextToken().token?.type == Tokens.SEMICOLON) {
            return lexer.nextToken() to expr
        }

        return lexer to expr
    }

    private fun parseExpression(lexer: Lexer, precedence: Int): Expression? {
        val prefix = prefixParseFunctions[lexer.token?.type] ?: return null
        return prefix(lexer)
    }

    private fun parseIdentifier(lexer: Lexer): Identifier? {
        if (lexer.token == null) {
            return null
        }
        return Identifier(lexer.token, lexer.token.literal)
    }

    private fun incrementForNext(lexer: Lexer): Pair<Lexer, Token?> {
        val next = lexer.nextToken()
        return next to next.token
    }

    private fun raiseError(expected: TokenType, actual: TokenType?) {
        val errorMessage = "Expected next token to be $expected but was $actual"
        errors = errors.plus(errorMessage)
    }
}

