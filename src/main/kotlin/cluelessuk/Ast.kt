package cluelessuk

sealed class Node {
    abstract fun tokenLiteral(): String
}

sealed class Statement : Node()

sealed class Expression : Node()

data class Program(val statements: List<Statement>, val errors: List<String>) : Node() {
    constructor(statements: List<Statement>) : this(statements, emptyList())

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    override fun tokenLiteral(): String {
        if (statements.isEmpty()) {
            return ""
        }
        return statements.first().tokenLiteral()
    }

    override fun toString() = statements.map { it.toString() }.reduce { a, b -> a + b }
}

data class LetStatement(
    val token: Token,
    val name: Identifier,
    val value: Expression
) : Statement() {

    override fun tokenLiteral() = token.literal
    override fun toString() = "(${tokenLiteral()} $name = $value)"
}

data class ReturnStatement(
    val token: Token,
    val returnValue: Expression
) : Statement() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "(${tokenLiteral()} $returnValue)"
}

data class Identifier(
    val token: Token,
    val value: String
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = value
}

data class ExpressionStatement(
    val token: Token,
    val expression: Expression
) : Statement() {
    override fun tokenLiteral() = token.literal
    override fun toString() = expression.toString()
}

data class IntegerLiteral(
    val token: Token,
    val value: Int
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = tokenLiteral()
}

data class BooleanLiteral(
    val token: Token,
    val value: Boolean
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = tokenLiteral()
}

data class StringLiteral(
    val token: Token,
    val value: String
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = value
}

data class PrefixExpression(
    val token: Token,
    val operator: String,
    val right: Expression
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "($operator $right)"
}

data class InfixExpression(
    val token: Token,
    val left: Expression,
    val operator: String,
    val right: Expression
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "($left $operator $right)"
}

data class IfExpression(
    val token: Token,
    val condition: Expression,
    val consequence: BlockStatement,
    val alternative: BlockStatement?
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "(if $condition then $consequence else ${alternative ?: "(none)"})"
}

data class BlockStatement(
    val token: Token,
    val statements: List<Statement>
) : Statement() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "{ ${statements.joinToString("; ")} }"
}

data class FunctionLiteral(
    val token: Token,
    val arguments: List<Identifier>,
    val body: BlockStatement
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "fn(${arguments.joinToString(",")}) $body"
}

data class ArrayLiteral(
    val token: Token,
    val elements: List<Expression>
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "[${elements.joinToString(",")}]"
}

data class CallExpression(
    val token: Token,
    val function: Expression,
    val arguments: List<Expression>
) : Expression() {
    override fun tokenLiteral() = token.literal
    override fun toString() = "${function.tokenLiteral()}(${arguments.joinToString(", ")})"
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

val precedences = mapOf(
    Tokens.EQ to OperatorPrecedence.EQUALS,
    Tokens.NOT_EQ to OperatorPrecedence.EQUALS,
    Tokens.LT to OperatorPrecedence.LESSGREATER,
    Tokens.GT to OperatorPrecedence.LESSGREATER,
    Tokens.PLUS to OperatorPrecedence.SUM,
    Tokens.MINUS to OperatorPrecedence.SUM,
    Tokens.SLASH to OperatorPrecedence.PRODUCT,
    Tokens.ASTERISK to OperatorPrecedence.PRODUCT,
    Tokens.LPAREN to OperatorPrecedence.CALL
)

fun precedenceOf(token: Token): OperatorPrecedence {
    return precedences[token.type] ?: OperatorPrecedence.LOWEST
}
