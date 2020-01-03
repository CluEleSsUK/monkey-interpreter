package cluelessuk

val EndOfFile = Token(Tokens.EOF, "")

data class Lexer @JvmOverloads constructor(
    private val code: String,
    private val position: Int = 0,
    val token: Token? = null
) {

    private val current: Char = if (position == code.length) {
        '0'
    } else {
        code[position]
    }

    // careful! This function does _not_ imply the `position` is at the last character of `code`
    // but rather that an EndOfFile marker has been inserted by the Lexer (`position == code.length`)
    fun hasMore(): Boolean {
        return token != EndOfFile
    }

    fun nextToken(): Lexer {
        return when (current) {
            ';' -> readAndIncrement(Tokens.SEMICOLON)
            ',' -> readAndIncrement(Tokens.COMMA)
            '(' -> readAndIncrement(Tokens.LPAREN)
            ')' -> readAndIncrement(Tokens.RPAREN)
            '[' -> readAndIncrement(Tokens.LBRACKET)
            ']' -> readAndIncrement(Tokens.RBRACKET)
            '{' -> readAndIncrement(Tokens.LBRACE)
            '}' -> readAndIncrement(Tokens.RBRACE)
            '+' -> readAndIncrement(Tokens.PLUS)
            '-' -> readAndIncrement(Tokens.MINUS)
            '/' -> readAndIncrement(Tokens.SLASH)
            '*' -> readAndIncrement(Tokens.ASTERISK)
            '<' -> readAndIncrement(Tokens.LT)
            '>' -> readAndIncrement(Tokens.GT)
            ':' -> readAndIncrement(Tokens.COLON)
            '=' -> readEquals()
            '!' -> readBang()
            '"' -> readString()
            else -> readNonSyntax()
        }
    }

    private fun readAndIncrement(type: Tokens): Lexer {
        return incremented(Token(type, current))
    }

    private fun incremented(token: Token?): Lexer {
        return this.copy(token = token, position = position + 1)
    }

    private fun readNonSyntax(): Lexer {
        return when {
            position == code.length -> this.copy(token = EndOfFile)
            current.isWhitespace() -> incremented(null).nextToken()
            current.isLetterOrUnderscore() -> readIdentifier()
            current.isDigit() -> readNumber()
            else -> incremented(Token(Tokens.ILLEGAL, current))
        }
    }

    private fun readIdentifier(): Lexer {
        val lastIdentifierIndex = lookAheadWhile(position) { code[it].isLetterOrUnderscore() }
        val identifier = code.slice(position until lastIdentifierIndex)
        val nextToken = Token(
            type = keywords[identifier] ?: Tokens.IDENT,
            literal = identifier
        )
        return this.copy(token = nextToken, position = lastIdentifierIndex)
    }

    private fun readNumber(): Lexer {
        val lastNumberIndex = lookAheadWhile(position) { code[it].isDigit() }
        val nextToken = Token(
            type = Tokens.INT,
            literal = code.slice(position until lastNumberIndex)
        )
        return this.copy(token = nextToken, position = lastNumberIndex)
    }

    private fun readEquals(): Lexer {
        if (isAtLastIndex() || code[position + 1] != '=') {
            return readAndIncrement(Tokens.ASSIGN)
        }

        return this.copy(token = Token(Tokens.EQ, "=="), position = position + 2)
    }

    private fun readBang(): Lexer {
        if (isAtLastIndex() || code[position + 1] != '=') {
            return readAndIncrement(Tokens.BANG)
        }
        return this.copy(token = Token(Tokens.NOT_EQ, "!="), position = position + 2)
    }

    private fun readString(): Lexer {
        val positionAfterDoubleQuote = position + 1
        val lastIdentifierIndex = lookAheadWhile(positionAfterDoubleQuote) { code[it] != '"' }
        val stringValue = code.slice(positionAfterDoubleQuote until lastIdentifierIndex)
        val nextToken = Token(
            type = Tokens.STRING,
            literal = stringValue
        )
        return this.copy(token = nextToken, position = lastIdentifierIndex + 1)
    }

    private fun lookAheadWhile(next: Int, predicate: (position: Int) -> Boolean): Int {
        if (next > code.length - 1 || !predicate(next)) {
            return next
        }

        return lookAheadWhile(next + 1, predicate)
    }

    private fun isAtLastIndex(): Boolean {
        return position == code.length - 1
    }
}

fun Char?.isLetterOrUnderscore(): Boolean {
    return this != null && (this.isLetter() || this == '_')
}
