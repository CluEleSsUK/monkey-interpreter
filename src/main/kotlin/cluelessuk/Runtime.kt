package cluelessuk

sealed class MObject
data class MInteger(val value: Int) : MObject()
data class MBoolean(val value: Boolean) : MObject() {
    companion object {
        fun of(input: Boolean): MBoolean {
            return if (input) True else False
        }
    }
}

object Null : MObject()

val True = MBoolean(true)
val False = MBoolean(false)


class MonkeyRuntime {
    fun eval(node: Node): MObject =
        when (node) {
            is Program -> eval(node.statements.last())
            is ExpressionStatement -> eval(node.expression)
            is IntegerLiteral -> MInteger(node.value)
            is BooleanLiteral -> MBoolean.of(node.value)
            is PrefixExpression -> evalPrefixExpression(node.operator, eval(node.right))
            is InfixExpression -> evalInfixExpression(node)
            else -> Null
        }

    private fun evalPrefixExpression(operator: String, right: MObject): MObject =
        when (operator) {
            "!" -> evalBangExpression(right)
            "-" -> evalMinusPrefixExpression(right)
            else -> Null
        }

    private fun evalBangExpression(right: MObject): MObject =
        when (right) {
            True -> False
            False -> True
            is Null -> True
            else -> False
        }

    private fun evalMinusPrefixExpression(right: MObject) =
        when (right) {
            is MInteger -> MInteger(-right.value)
            else -> Null
        }

    private fun evalInfixExpression(expression: InfixExpression): MObject {
        val operator = expression.operator
        val left = eval(expression.left)
        val right = eval(expression.right)

        return when {
            left is MInteger && right is MInteger -> evalIntegerInfixExpression(operator, left, right)
            else -> evalBooleanInfixExpression(operator, left, right)
        }
    }

    private fun evalIntegerInfixExpression(operator: String, left: MInteger, right: MInteger): MObject =
        when (operator) {
            "+" -> MInteger(left.value + right.value)
            "-" -> MInteger(left.value - right.value)
            "*" -> MInteger(left.value * right.value)
            "/" -> MInteger(left.value / right.value)
            ">" -> MBoolean(left.value > right.value)
            "<" -> MBoolean(left.value < right.value)
            else -> evalBooleanInfixExpression(operator, left, right)
        }

    private fun evalBooleanInfixExpression(operator: String, left: MObject, right: MObject): MObject =
        when (operator) {
            "==" -> MBoolean.of(left == right)
            "!=" -> MBoolean.of(left != right)
            else -> Null
        }
}
