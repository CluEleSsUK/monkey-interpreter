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

data class MString(val value: String) : MObject("STRING")
data class MArray(val elements: List<MObject>) : MObject("ARRAY")
data class MMap(val elements: Map<MObject, MObject>) : MObject("MAP")
data class MFunction(val parameters: List<Identifier>, val body: BlockStatement, val scope: Scope) : MObject("FUNCTION")
data class BuiltinFunction(val f: (List<MObject>) -> MObject) : MObject("BUILTIN-FUNCTION")
data class MReturnValue(val value: MObject) : MObject("RETURN")
object Null : MObject("NULL") {
    // doesn't play nice with groovy without an overridden equals
    override fun equals(other: Any?): Boolean {
        return other is Null
    }
}

sealed class MError(message: String) : MObject("ERROR") {
    data class ParseError(val errors: List<String>) : MError("Programs with parse errors cannot be evaluated: \n ${errors.joinToString("\n")}")
    data class TypeMismatch(val expression: String) : MError("Type mismatch: $expression")
    data class UnknownOperator(val expression: String) : MError("Unknown operator: $expression")
    data class UnknownIdentifier(val identifier: String) : MError("Unknown identifier: $identifier")
    data class IncorrectNumberOfArgs(val expectedCount: Int, val actualCount: Int) : MError("Invalid number of arguments.  Expected $expectedCount, got $actualCount")
}

val True = MBoolean(true)
val False = MBoolean(false)


class MonkeyRuntime {

    private val globalScope = Scope()

    fun eval(node: Node): MObject = eval(node, globalScope)
    private fun eval(node: Node, scope: Scope): MObject =
        when (node) {
            is Program -> evalProgram(node, scope)
            is ExpressionStatement -> eval(node.expression, scope)
            is IntegerLiteral -> MInteger(node.value)
            is BooleanLiteral -> MBoolean.of(node.value)
            is StringLiteral -> MString(node.value)
            is FunctionLiteral -> MFunction(node.arguments, node.body, scope)
            is ArrayLiteral -> evalArrayExpression(node, scope)
            is MapLiteral -> evalMapExpression(node, scope)
            is PrefixExpression -> evalPrefixExpression(node, scope)
            is InfixExpression -> evalInfixExpression(node, scope)
            is IfExpression -> evalIfExpression(node, scope)
            is BlockStatement -> evalStatements(node.statements, scope)
            is ReturnStatement -> MReturnValue(eval(node.returnValue, scope))
            is LetStatement -> evalLetStatement(node, scope)
            is Identifier -> evalIdentifier(node, scope)
            is CallExpression -> evalCallExpression(node, scope)
            is IndexExpression -> evalIndexExpression(node, scope)
        }

    private fun evalProgram(program: Program, scope: Scope): MObject {
        if (program.hasErrors()) {
            return MError.ParseError(program.errors)
        }

        return evalStatements(program.statements, scope)
    }

    private fun evalStatements(statements: List<Statement>, scope: Scope): MObject {
        if (statements.isEmpty()) {
            return Null
        }

        val currentValue = eval(statements.first(), scope)
        if (statements.size == 1) {
            return currentValue
        }

        return when (currentValue) {
            is MReturnValue -> when (currentValue.value) {
                is MError -> currentValue.value
                else -> currentValue
            }
            is MError -> currentValue
            else -> evalStatements(statements.drop(1), scope)
        }
    }

    private fun evalIdentifier(identifier: Identifier, scope: Scope): MObject {
        return scope.get(identifier)
    }

    private fun evalPrefixExpression(expression: PrefixExpression, scope: Scope): MObject {
        val right = eval(expression.right, scope)
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

    private fun evalInfixExpression(expression: InfixExpression, scope: Scope): MObject {
        val operator = expression.operator
        val left = eval(expression.left, scope)
        val right = eval(expression.right, scope)

        return when {
            left is MError -> left
            right is MError -> right
            left is MInteger && right is MInteger -> evalIntegerInfixExpression(operator, left, right)
            left is MString && right is MString -> evalStringInfixExpression(operator, left, right)
            left.type != right.type -> MError.TypeMismatch("$left $operator $right")
            else -> evalBooleanInfixExpression(operator, left, right)
        }
    }

    private fun evalStringInfixExpression(operator: String, left: MString, right: MString): MObject =
        when (operator) {
            "+" -> MString(left.value + right.value)
            else -> MError.UnknownOperator("$left $operator $right")
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

    private fun evalArrayExpression(literal: ArrayLiteral, scope: Scope): MObject {
        return MArray(literal.elements.map {
            val result = eval(it, scope)
            if (result is MError) {
                return result
            }

            result
        })
    }

    private fun evalMapExpression(literal: MapLiteral, scope: Scope): MObject {
        var map = mapOf<MObject, MObject>()
        literal.elements.forEach {
            val key = eval(it.key, scope)
            if (key is MError) {
                return key
            }

            if (key !is MString && key !is MInteger && key !is MBoolean) {
                return MError.TypeMismatch("Map key must be a String, Boolean or an Integer")
            }

            val value = eval(it.value, scope)
            if (value is MError) {
                return value
            }

            map = map.plus(key to value)
        }
        return MMap(map)
    }

    private fun evalIfExpression(expression: IfExpression, scope: Scope): MObject {
        val condition = eval(expression.condition, scope)

        if (isTruthy(condition)) {
            return eval(expression.consequence, scope)
        }

        if (expression.alternative == null) {
            return Null
        }

        return eval(expression.alternative, scope)
    }

    private fun evalLetStatement(node: LetStatement, scope: Scope): MObject {
        val varName = node.name.value
        val value = eval(node.value, scope)
        if (value is Error) {
            return value
        }

        return scope.set(varName, value)
    }

    private fun evalCallExpression(node: CallExpression, scope: Scope): MObject {
        val func = eval(node.function, scope)

        if (func is MError) {
            return func
        }

        val evaluatedArgs = node.arguments.map {
            val result = eval(it, scope)
            if (result is MError) {
                return@evalCallExpression result
            } else {
                result
            }
        }

        return applyFunction(func, evaluatedArgs)
    }

    private fun evalIndexExpression(node: IndexExpression, scope: Scope): MObject {
        val left = eval(node.left, scope)

        if (left is MError) {
            return left
        }

        val index = eval(node.index, scope)

        return when {
            index is MError -> index
            left is MArray -> evalArrayIndex(index, left)
            left is MMap -> evalMapIndex(index, left)
            else -> MError.TypeMismatch("Index not supported for type ${left.type}")
        }
    }

    private fun evalArrayIndex(index: MObject, array: MArray): MObject {
        return when {
            index !is MInteger -> MError.TypeMismatch("Arrays cannot be indexed by ${index.type}")
            index.value < 0 -> Null
            index.value > array.elements.size - 1 -> Null
            else -> array.elements[index.value]
        }
    }

    private fun evalMapIndex(index: MObject, map: MMap): MObject {
        if (index !is MInteger && index !is MString && index !is MBoolean) {
            return MError.TypeMismatch("Maps cannot be indexed by ${index.type}")
        }

        return map.elements[index] ?: Null
    }

    private fun applyFunction(func: MObject, args: List<MObject>): MObject {
        return when (func) {
            is BuiltinFunction -> func.f(args)
            is MFunction -> {
                val functionScope = Scope.functionScope(func, args)
                val result = eval(func.body, functionScope)

                return if (result is MReturnValue) {
                    result.value
                } else {
                    result
                }
            }
            else -> MError.TypeMismatch("Expected function but got ${func.type}")
        }
    }

    private fun isTruthy(obj: MObject): Boolean =
        when (obj) {
            True -> true
            False -> false
            is Null -> false
            else -> true
        }
}
