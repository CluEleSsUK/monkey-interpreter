package cluelessuk

val builtinFunctions = mapOf("len" to BuiltinFunction(::len))

fun len(args: List<MObject>): MObject {
    if (args.size != 1) {
        return MError.IncorrectNumberOfArgs(1, args.size)
    }

    return when (val arg = args[0]) {
        is MString -> MInteger(arg.value.length)
        else -> MError.TypeMismatch("len(${args[0]})")
    }
}
