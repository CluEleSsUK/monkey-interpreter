package cluelessuk

sealed class MObject
data class MInteger(val value: Int) : MObject()
data class Bool(val value: Boolean) : MObject()
object Null : MObject()


class MonkeyRuntime {
    fun eval(node: Node): MObject {
        return when (node) {
            is Program -> eval(node.statements.last())
            is ExpressionStatement -> eval(node.expression)
            is IntegerLiteral -> MInteger(node.value)
            is BooleanLiteral -> Bool(node.value)
            else -> Null
        }
    }
}