package com.computation.generators.regexp;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Parser {
    static class ParserError extends RuntimeException {}

    String source;
    List<Token> tokens;
    Set<String> alphabet;
    int current = 0;

    Parser(List<Token> tokens, Set<String> alphabet, String source) {
	this.tokens = new ArrayList<>(tokens);
	this.alphabet = new HashSet<>(alphabet);
	this.source = source;
    }

    // R => expression
    // expression => union
    // union => concat ( '|' concat )? ( '|' concat ) \*
    // concat => star star*
    // star => primary ( '*'  )?
    // primary => SYMBOL | '(' expression ')'
    Expression parse() {
	return expression();
    }

    // expression => union
    Expression expression() {
	return union();
    }

    // union => concat ( '|' concat )? ( '|' concat )*
    Expression union() {
	Expression first = concat();
	List<Expression> exprs = new ArrayList<>();
	exprs.add(first);
	if (match(TokenType.PIPE)) {
	    Expression second = concat();
	    exprs.add(second);
	}
	while (match(TokenType.PIPE)) {
	    Expression newExpr = concat();
	    exprs.add(newExpr);
	}
	if (exprs.size() <= 1) return first;
	return new Expression.Union(exprs);
    }

    // concat => star star*
    Expression concat() {
	List<Expression> exprs = new ArrayList<>();
	while (true) {
	    Expression expr = star();
	    if (expr != null) {
		exprs.add(expr);
	    } else break;
	}
	
	if (exprs.size() == 0) return null;
	if (exprs.size() == 1) return exprs.get(0);
	
	return new Expression.Concat(exprs);
    }

    // star => primary ( '*' )?
    Expression star() {
	Expression expr = primary();
	if (match(TokenType.STAR)) {
	    return new Expression.Star(expr);
	}
	return expr;
    }

    // primary => SYMBOL | '(' expression ')'
    Expression primary() {
	if (isAtEnd()) return null;
	
	if (match(TokenType.LEFT_PAREN)) {
	    Expression expr = expression();
	    consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
	    return new Expression.Grouping(expr);
	} else if (alphabet.contains(peek().lexeme)) {
	    return new Expression.Symbol(advance().lexeme);
	}
	return null;
    }

    Token consume(TokenType type, String message) {
	if (check(type)) return advance();
	throw error(peek(), message);
    }

    ParserError error(Token token, String message) {
	RegularExpression.error(source, token, message);
	return new ParserError();
    }

    boolean match(TokenType expected) {
	if (check(expected)) {
	    advance();
	    return true;
	}
	return false;
    }

    boolean check(TokenType expected) {
	if (isAtEnd()) return false;
	return (peek().type == expected);
    }

    Token peek() {
	return tokens.get(current);
    }

    Token advance() {
	if(!isAtEnd()) current++;
	return previous();
    }

    Token previous() {
	return tokens.get(current - 1);
    }

    boolean isAtEnd() {
	return peek().type == TokenType.END;
    }
}

