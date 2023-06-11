package com.computation.generators.regexp;

import com.computation.automata.nfa.NFA;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public abstract class Expression {
    public static int counter = 0;

    interface Visitor {
	public NFA visitSymbolExpression(Expression.Symbol expr);
	public NFA visitGroupingExpression(Expression.Grouping expr);
	public NFA visitStarExpression(Expression.Star expr);
	public NFA visitUnionExpression(Expression.Union expr);
	public NFA visitConcatExpression(Expression.Concat expr);
    }

    Expression parent = null;

    public abstract NFA accept(Visitor visitor, Collection<String> alphabet);
    public abstract String stringFormat();

    public String toString() {
	String thisString = stringFormat();
	if (parent == null) return thisString;
	return "(%s <- %s)".formatted(parent.stringFormat(), thisString);
    }

    static class Symbol extends Expression{
	String symbol;
	public Symbol(String symbol) {
	    this.symbol = symbol;
	}

	@Override
	public NFA accept(Visitor visitor, Collection<String> alphabet) {
	    String startState = "<%s-%d>".formatted(toString(), counter++);
	    String acceptState = "<%s-%d>".formatted(toString(), counter++);
	    NFA x = new NFA(
			    List.of(startState, acceptState),
			    alphabet,
			    new Object[][] {
				{startState, symbol, new String[]{acceptState}},
			    },
			    startState,
			    List.of(acceptState),
			    false
            );
	    return x;
	}

	@Override
	public String stringFormat() {
	    return symbol;
	}
    }

    static class Grouping extends Expression {
	Expression expr;
	public Grouping(Expression expr) {
	    this.expr = expr;
	    this.expr.parent = this;
	}

	@Override
	public NFA accept(Visitor visitor, Collection<String> alphabet) {
	    NFA x = expr.accept(visitor, alphabet);
	    return x;
	}

	@Override
	public String stringFormat() {
	    return "(%s)".formatted(expr.stringFormat());
	}
    }

    static class Star extends Expression {
	Expression expr;
	public Star(Expression expr) {
	    this.expr = expr;
	    this.expr.parent = this;
	}

	@Override
	public NFA accept(Visitor visitor, Collection<String> alphabet) {
	    NFA x = NFA.star(expr.accept(visitor, alphabet), "<%s-%d>".formatted(toString(), counter++));
	    return x;
	}

	@Override
	public String stringFormat() {
	    return expr.stringFormat() + "*";
	}
    }

    static class Union extends Expression {
	public List<Expression> expressions;

	public Union(List<Expression> expressions) {
	    this.expressions = expressions;
	    for (Expression expr : this.expressions) {
		expr.parent = this;
	    }
	}

	@Override
	public NFA accept(Visitor visitor, Collection<String> alphabet) {
	    List<NFA> automataList = new ArrayList<>();
	    for (Expression expr : expressions) {
		automataList.add(expr.accept(visitor, alphabet));
	    }

	    List<String> suffixes = new ArrayList<>();
	    for (int i = 0;i < expressions.size();i++) {
		suffixes.add("-AUTOMATA-"+i);
	    }
	    NFA x = NFA.union(automataList, "<%s-%d>".formatted(toString(), counter++), suffixes);
	    return x;
	}

	@Override
	public String stringFormat() {
	    StringBuilder s = new StringBuilder();
	    for (Expression expr : expressions) {
		s.append(expr.stringFormat() + "|");
	    }
	    s.deleteCharAt(s.length()-1);
	    if (parent == null) {
		return s.toString();
	    }
	    return s.toString();
	}
    }

    static class Concat extends Expression {
	public List<Expression> expressions;

	public Concat(List<Expression> expressions) {
	    this.expressions = new ArrayList<>(expressions);
	    for (Expression expr : this.expressions) {
		expr.parent = this;
	    }
	}

	@Override
	public NFA accept(Visitor visitor, Collection<String> alphabet) {
	    List<NFA> automataList = new ArrayList<>();
	    for (Expression expr : expressions) {
		automataList.add(expr.accept(visitor, alphabet));
	    }

	    List<String> suffixes = new ArrayList<>();
	    for (int i = 0;i < expressions.size();i++) {
		suffixes.add("-AUTOMATA-"+i);
	    }
	    NFA x = NFA.concatenate(automataList, suffixes);
	    return x;
	}

	@Override
	public String stringFormat() {
	    StringBuilder s = new StringBuilder();
	    for (Expression expr : expressions) {
		s.append(expr.stringFormat());
	    }
	    if (parent == null) {
		return s.toString();
	    }
	    return s.toString();
	}
    };
}
