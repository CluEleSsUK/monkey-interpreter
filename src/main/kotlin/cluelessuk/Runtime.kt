package cluelessuk

sealed class MObject(val type: String)
data class MInteger(val value: Int) : MObject("INTEGER")
data class MBoolean(val value: Boolean) : MObject("BOOLEAN") {
    companion object {
        fun of(input: Boolean): MBoolean {
            return if (input) True else False
        }
    }
}

data class MReturnValue(val value: MObject) : MObject("RETURN")
object Null : MObject("NULL") {
    // doesn't play nice with groovy without an overridden equals
    override fun equals(other: Any?): Boolean {
        return other is Null
    }
}

sealed class MError(message: String) : MObject("ERROR") {
    data class TypeMismatch(val expression: String) : MError("Type mismatch: $expression")
    data class UnknownOperator(val expression: String) : MError("Unknown operator: $expression")
}

val True = MBoolean(true)
val False = MBoolean(false)


class MonkeyRuntime {
    fun eval(node: Node): MObject =
        when (node) {
            is Program -> evalStatements(node.statements)
            is ExpressionStatement -> eval(node.expression)
            is IntegerLiteral -> MInteger(node.value)
            is BooleanLiteral -> MBoolean.of(node.value)
            is PrefixExpression -> evalPrefixExpression(node)
            is InfixExpression -> evalInfixExpression(node)
            is IfExpression -> evalIfExpression(node)
            is BlockStatement -> evalStatements(node.statements)
            is ReturnStatement -> MReturnValue(eval(node.returnValue))
            else -> Null
        }

    private fun evalStatements(statements: List<Statement>): MObject {
        if (statements.isEmpty()) {
            return Null
        }

        val currentValue = eval(statements.first())
        if (statements.size == 1) {
            return currentValue
        }

        return when (currentValue) {
            is MReturnValue -> when (currentValue.value) {
                is MError -> currentValue.value
                else -> currentValue
            }
            is MError -> currentValue
            else -> evalStatements(statements.drop(1))
        }
    }

    private fun evalPrefixExpression(expression: PrefixExpression): MObject {
        val right = eval(expression.right)
        if (right is MError) {
            return right
        }

        return when (val operator = expression.operator) {
            "!" -> evalBangExpression(right)
            "-" -> evalMinusPrefixExpression(right)
            else -> MError.UnknownOperator("$operator $right")
        }
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
            is Error -> right
            else -> MError.UnknownOperator("-$right")
        }

    private fun evalInfixExpression(expression: InfixExpression): MObject {
        val operator = expression.operator
        val left = eval(expression.left)
        val right = eval(expression.right)

        return when {
            left is MError -> left
            right is MError -> right
            left is MInteger && right is MInteger -> evalIntegerInfixExpression(operator, left, right)
            left.type != right.type -> MError.TypeMismatch("$left $operator $right")
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
            else -> MError.UnknownOperator("$left $operator $right")
        }

    private fun evalIfExpression(expression: IfExpression): MObject {
        val condition = eval(expression.condition)

        if (isTruthy(condition)) {
            return eval(expression.consequence)
        }

        if (expression.alternative == null) {
            return Null
        }

        return eval(expression.alternative)
    }

    private fun isTruthy(obj: MObject): Boolean =
        when (obj) {
            True -> true
            False -> false
            is Null -> false
            else -> true
        }
}
