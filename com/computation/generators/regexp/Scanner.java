package com.computation.generators.regexp;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Scanner {
    private final String source;
    final List<Token> tokens = new ArrayList<>();
    final Set<String> alphabet = new HashSet<>();

    int current = 0;

    Scanner(String source) {
	this.source = source;
    }

    List<Token> generateTokens() {
	while (!isAtEnd()) {
	    char prev = previous();
	    char cur = peek();
	    if (
		(prev == '\0' && cur == '\0') ||
		(prev == '\0' && cur == '|') ||
		(prev == '(' && cur == '|') ||
		(prev == '(' && cur == ')') ||
		(prev == '|' && cur == '|') ||
		(prev == '|' && cur == ')') ||
		(prev == '|' && cur == '\0')
            ) {
		tokens.add(new Token(TokenType.EMPTY_STRING, "", current));
		alphabet.add("");
	    }
	    tokens.add(generateNextToken());
	}
	tokens.add(new Token(TokenType.END, "", current));
	return tokens;
    }

    Token generateNextToken() {
	char c = advance();
	switch(c) {
	case '(':
	    return new Token(TokenType.LEFT_PAREN, "(", current-1);
	case ')':
	    return new Token(TokenType.RIGHT_PAREN, ")", current-1);
	case '*':
	    return new Token(TokenType.STAR, "*", current-1);
	case '|':
	    return new Token(TokenType.PIPE, "|", current-1);
	}
	alphabet.add(String.valueOf(c));
	// Assume that terminals single characters.
	return new Token(TokenType.SYMBOL, String.valueOf(c), current-1);
    }

    char advance() {
	if (!isAtEnd()) return source.charAt(current++);
	return '\0';
    }

    char peek() {
	return source.charAt(current);
    }

    char previous() {
	if (current > 0) return source.charAt(current-1);
	return '\0';
    }

    boolean isAtEnd() {
	return current >= source.length();
    }
}
