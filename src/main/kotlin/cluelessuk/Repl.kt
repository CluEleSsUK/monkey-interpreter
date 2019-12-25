package cluelessuk


const val exitKeyword = "exit"
val runtime = MonkeyRuntime()

fun main() {
    startRepl()
}

fun startRepl() {
    println("Monkey REPL! Type `exit` to exit.")

    while (true) {
        val input = readConsoleInput()
        if (input.isNullOrBlank() || input == exitKeyword) {
            break
        }

        val parser = Parser(Lexer(input))
        val program = parser.parseProgram()

        if (program.hasErrors()) {
            renderError(program)
        } else {
            render(runtime.eval(program))
        }
    }
}

fun readConsoleInput(): String? {
    print(">> ")
    return readLine()
}

fun render(obj: MObject) {
    println(obj)
}

fun renderError(program: Program) {
    println("Error(s) in program!")
    program.errors.forEach(::println)
}

