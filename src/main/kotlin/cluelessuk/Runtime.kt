package cluelessuk

sealed class MObject
data class MInteger(val value: Int) : MObject()
data class MBoolean(val value: Boolean) : MObject()

object Null : MObject()

val True = MBoolean(true)
val False = MBoolean(false)


class MonkeyRuntime {
    fun eval(node: Node): MObject =
        when (node) {
            is Program -> eval(node.statements.last())
            is ExpressionStatement -> eval(node.expression)
            is IntegerLiteral -> MInteger(node.value)
            is BooleanLiteral -> if (node.value) True else False
            is PrefixExpression -> evalPrefixExpression(node.operator, eval(node.right))
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
}
