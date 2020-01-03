package cluelessuk

val builtinFunctions = mapOf(
    "len" to BuiltinFunction(withOneArg(::len)),
    "first" to BuiltinFunction(withOneArg(checkingType(::first))),
    "last" to BuiltinFunction(withOneArg(checkingType(::last)))
)

fun len(target: MObject): MObject {
    return when (target) {
        is MString -> MInteger(target.value.length)
        is MArray -> MInteger(target.elements.size)
        else -> MError.TypeMismatch("len($target)")
    }
}

fun first(target: MArray): MObject = target.elements.firstOrNull() ?: Null
fun last(target: MArray): MObject = target.elements.lastOrNull() ?: Null

inline fun <reified T : MObject> checkingType(crossinline fn: (T) -> MObject): (MObject) -> MObject {
    return { input: MObject ->
        if (input !is T) {
            MError.TypeMismatch("Expected ARRAY got ${input.type}")
        } else {
            fn(input)
        }
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