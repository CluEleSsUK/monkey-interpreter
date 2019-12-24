package cluelessuk


const val exitKeyword = "exit"

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
        render(parser.parseProgram())
    }
}

fun readConsoleInput(): String? {
    print(">> ")
    return readLine()
}

fun render(program: Program) {
    if (program.hasErrors()) {
        println("Error(s) in program!")
        program.errors.forEach(::println)
    } else {
        println(program)
    }
}
