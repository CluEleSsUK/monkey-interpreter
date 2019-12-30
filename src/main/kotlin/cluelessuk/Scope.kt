package cluelessuk

class Scope(private val outer: Scope? = null) {
    private val storage = mutableMapOf<String, MObject>()

    fun set(variableName: String, value: MObject): MObject {
        if (value is MError) {
            return value
        }
        storage[variableName] = value
        return value
    }

    fun get(identifier: Identifier): MObject {
        val variableName = identifier.value
        return storage[variableName]
            ?: outer?.get(identifier)
            ?: MError.UnknownIdentifier(variableName)
    }

    companion object {
        fun functionScope(func: MFunction, args: List<MObject>, enclosingScope: Scope): Scope {
            val scope = Scope(enclosingScope)

            func.parameters.forEachIndexed { index, param ->
                scope.set(param.value, args[index])
            }

            return scope
        }
    }
}