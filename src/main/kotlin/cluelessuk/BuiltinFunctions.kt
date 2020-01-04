package cluelessuk

val builtinFunctions = mapOf(
    "len" to BuiltinFunction(withOneArg(::len)),
    "first" to BuiltinFunction(withOneArg(checkingType(::first))),
    "last" to BuiltinFunction(withOneArg(checkingType(::last))),
    "rest" to BuiltinFunction(withOneArg(checkingType(::rest))),
    "push" to BuiltinFunction(::push),
    "puts" to BuiltinFunction(::puts)
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
fun rest(target: MArray): MObject = if (target.elements.isEmpty()) Null else MArray(target.elements.drop(1))
fun push(args: List<MObject>): MObject {
    if (args.size != 2) {
        return MError.IncorrectNumberOfArgs(2, args.size)
    }

    val target = args[0]
    val element = args[1]

    return when (target) {
        !is MArray -> MError.TypeMismatch("Expected ARRAY, got ${target.type}")
        else -> target.copy(elements = target.elements.plus(element))
    }
}

fun puts(args: List<MObject>): MObject {
    args.forEach(::println)
    return Null
}

inline fun <reified T : MObject> checkingType(crossinline fn: (T) -> MObject): (MObject) -> MObject {
    return { input: MObject ->
        if (input !is T) {
            MError.TypeMismatch("Function does not support type ${input.type}")
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
