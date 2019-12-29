package cluelessuk

class Environment {
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
        return storage[variableName] ?: MError.UnknownIdentifier(variableName)
    }
}