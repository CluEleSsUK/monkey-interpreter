package cluelessuk

interface Node {
    fun tokenLiteral(): String
}

interface Statement : Node

interface Expression : Node

data class Program(val statements: List<Statement>, val errors: List<String>) : Node {
    constructor(statements: List<Statement>) : this(statements, emptyList())

    override fun tokenLiteral(): String {
        if (statements.isEmpty()) {
            return ""
        }
        return statements.first().tokenLiteral()
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }
}

data class LetStatement(
    val token: Token,
    val name: Identifier,
    val value: Expression
) : Statement {

    override fun tokenLiteral() = token.literal
}

data class ReturnStatement(
    val token: Token,
    val returnValue: Expression?
) : Statement {
    override fun tokenLiteral() = token.literal
}

data class Identifier(
    val token: Token,
    val value: String
) : Expression {
    override fun tokenLiteral() = token.literal
}

data class ExpressionStatement(
    val token: Token,
    val expression: Expression
) : Statement {
    override fun tokenLiteral() = token.literal
}

data class IntegerLiteral(
    val token: Token,
    val value: Int
) : Expression {
    override fun tokenLiteral() = token.literal
}

data class IntExpr(
    val token: Token,
    val value: String
) : Expression {
    override fun tokenLiteral() = token.literal
}

enum class OperatorPrecedence {
    LOWEST,
    EQUALS,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL
}
