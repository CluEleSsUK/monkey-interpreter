package cluelessuk

typealias TokenType = Tokens

data class Token(val type: TokenType, val literal: String) {
    constructor(type: TokenType, charLiteral: Char) : this(type, charLiteral.toString())
}

enum class Tokens {
    ILLEGAL,
    EOF,
    IDENT,
    INT,
    ASSIGN,
    COMMA,
    SEMICOLON,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    FUNCTION,
    LET,
    BANG,
    PLUS,
    MINUS,
    DIVIDE,
    MULTIPLY,
    LT,
    GT,
    TRUE,
    FALSE,
    IF,
    ELSE,
    RETURN,
    NOT_EQ,
    EQ
}


val keywords = mapOf(
    "fn" to Tokens.FUNCTION,
    "let" to Tokens.LET,
    "true" to Tokens.TRUE,
    "false" to Tokens.FALSE,
    "if" to Tokens.IF,
    "else" to Tokens.ELSE,
    "return" to Tokens.RETURN

)