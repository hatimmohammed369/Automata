package com.computation.generators.regexp;

import com.computation.automata.nfa.NFA;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class RegularExpression implements Expression.Visitor {
    static boolean hadError = false;
    Set<String> alphabet;
    NFA equivalentNFA;

    public NFA visitSymbolExpression(Expression.Symbol expr) {
	return computeEquivalentNFA(expr);
    }
    
    public NFA visitGroupingExpression(Expression.Grouping expr) {
	return computeEquivalentNFA(expr);
    }
    
    public NFA visitStarExpression(Expression.Star expr) {
	return computeEquivalentNFA(expr);
    }
    
    public NFA visitUnionExpression(Expression.Union expr) {
	return computeEquivalentNFA(expr);
    }
    
    public NFA visitConcatExpression(Expression.Concat expr) {
	return computeEquivalentNFA(expr);
    }

    private NFA computeEquivalentNFA(Expression expr) {
	if (equivalentNFA != null) return equivalentNFA;
	equivalentNFA = expr.accept(this, alphabet);
	return equivalentNFA;
    }

    public NFA getEquivalentNFA() {
	return equivalentNFA;
    }

    public static RegularExpression compile(String expression) {
	Scanner scanner = new Scanner(expression);
	List<Token> tokens = scanner.generateTokens();
	Set<String> alphabet = scanner.alphabet;
	Parser parser = new Parser(scanner.tokens, scanner.alphabet, expression);
	Expression parseTree = parser.parse();
	if (hadError) System.exit(1);
	RegularExpression r = new RegularExpression();
	r.alphabet = alphabet;
	r.computeEquivalentNFA(parseTree);
	Expression.counter = 0;
	return r;
    }

    public boolean fullmatch(String input) {
	return equivalentNFA.compute(input, false) == NFA.Computation.Accept;
    }

    static void error(String source, Token token, String message) {
	if (token.type == TokenType.END) {
	    report(source, token.position, " at end", message);
	} else {
	    report(source, token.position, " at '" + token.lexeme + "'", message);
	}
    }

    static void report(String source, int column, String where, String message) {
	System.err.println("Error " + where + ": ");
	System.err.println(message);
	System.err.println(source);
	for (int i = 0;i < column;i++) {
	    System.err.print(" ");
	}
	System.err.println("^");
	hadError = true;
    }
}
