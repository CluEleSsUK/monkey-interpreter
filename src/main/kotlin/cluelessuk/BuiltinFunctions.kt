package cluelessuk

val builtinFunctions = mapOf(
    "len" to BuiltinFunction(withOneArg(::len)),
    "first" to BuiltinFunction(withOneArg(::first))
)

fun len(target: MObject): MObject {
    return when (target) {
        is MString -> MInteger(target.value.length)
        is MArray -> MInteger(target.elements.size)
        else -> MError.TypeMismatch("len($target)")
    }
}

fun first(target: MObject): MObject {
    return when (target) {
        is MArray -> target.elements.firstOrNull() ?: Null
        else -> MError.TypeMismatch("Expected ARRAY got ${target.type}")
    }
}

fun withOneArg(fn: (MObject) -> MObject): (List<MObject>) -> MObject {
    return { args: List<MObject> ->
        if (args.size != 1) {
            MError.IncorrectNumberOfArgs(1, args.size)
        } else {
            fn(args[0])
        }
    }
}