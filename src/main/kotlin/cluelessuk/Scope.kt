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
            ?: builtinFunctions[identifier.value]
            ?: MError.UnknownIdentifier(variableName)
    }

    companion object {
        fun functionScope(func: MFunction, args: List<MObject>): Scope {
            val localScope = Scope(func.scope)

            func.parameters.forEachIndexed { index, param ->
                localScope.set(param.value, args[index])
            }

            return localScope
        }
    }
}