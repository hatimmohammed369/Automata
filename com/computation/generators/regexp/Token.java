package com.computation.generators.regexp;

public class Token {
    final TokenType type;
    final String lexeme;
    final int position;

    Token(TokenType type, String lexeme, int position) {
	this.type = type;
	this.lexeme = lexeme;
	this.position = position;
    }

    public String toString() {
	return "<%s %s %d>".formatted(type, lexeme, position);
    }
}

