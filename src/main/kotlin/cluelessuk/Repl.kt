package cluelessuk

fun main() {
    startRepl()
}

fun startRepl() {
    while (true) {
        println("Monkey REPL! Type `exit` to exit.")
        print(">> ")
        val input = readLine()
        if (input.isNullOrBlank() || input == "exit") {
            println("Exiting...")
            break
        }

        eval(Lexer(input).nextToken())
    }
}

tailrec fun eval(lexer: Lexer) {
    if (lexer.hasMore() && lexer.token?.type != Tokens.EOF) {
        println(lexer.token)
        eval(lexer.nextToken())
    }
}

