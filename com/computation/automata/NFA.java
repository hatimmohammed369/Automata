package com.computation.automata;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

// Nondeterministic Finite Automata
public class NFA {
    HashSet<String> states;
    HashSet<String> alphabet;
    // in (transitionFunction) keys are states
    // keys in (HashMap<String, HashSet<String>>) are alphabet symbols
    HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    HashMap<String, HashSet<String>> emptyStringTransitions;
    String startState;
    HashSet<String> acceptStates;
    boolean isDeterministic;

    public NFA(
        String[] states,
	// if the empty string is in (alphabet), then it's nondeterministic
	String[] alphabet,
	// {{"state-a", "input-symbol", {"state-0", "state-1", "state-2", ...}}, ...}
	Object[][] transitions,
	String startState,
	String[] acceptStates,
	boolean isDeterministic
    ) {
	this.states = new HashSet<>(Set.<String>of(states));

	StringBuilder error = new StringBuilder();
	if (!this.states.contains(startState)) {
	    error.append("Unknown start state '" + startState + "'" + "\n");
	} else {
	    this.startState = startState;
	}

	this.alphabet = new HashSet<>(Set.<String>of(alphabet));
	this.transitionFunction = new HashMap<>();

	HashSet<String> statesWithEmptyStringTransitions = new HashSet<>();

	for(Object[] transitionArray : transitions) {
	    if (transitionArray.length != 3) {
		error.append("Transition array " +
                    Set.<Object>of(transitionArray) +
		    " must of the form " +
		    "{{\"state-a\", \"input-symbol\", {\"state-0\", \"state-1\", \"state-2\", ...}}, ...}\"" +
			     "\n");
	    }

	    String inState = (String)transitionArray[0];
	    String symbol = (String)transitionArray[1];
	    Object[] outStates = (Object[])transitionArray[2];

	    if (!isDeterministic && symbol.equals("")) {
		statesWithEmptyStringTransitions.add(inState);
	    }

	    // detect if transition contains a new state not already in the (states) array
	    if (!this.states.contains(inState)) {
		error.append(
                    "State '" + inState + "' must be included " +
		    "in the (states) array" + "\n");
	    }

	    // detect if transition contains a new symbol not already in the (alphabet) array
	    if (!this.alphabet.contains(symbol)) {
		error.append(
                    "Symbol '" + symbol + "' must be included " +
		    "in the (alphabet) array"
                 + "\n");
	    }

	    // detect if transition contains a new state not already in the (states) array
	    for (Object outState : outStates) {
		if (outState instanceof String) {
		    if (!this.states.contains(outState)) {
			error.append(
				     "State '" + outState + "' must be included " +
				     "in the (states) array"
				     + "\n");
		    }
		} else {
		    error.append("Element '" + outState + "' must be a string.\n");
		}
	    }
	}

	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	} else {
	    this.acceptStates = new HashSet<>(Set.<String>of(acceptStates));
	}

	// for each state X, go through each symbol (a) creating a set
	// corresponding to that symbol containing all states reachable
	// from state X by reading symbol (a)
	// store this symbol-set correspondce in a map in (transitionfunction)
	// each map created below will later be filled with the actual states
	// reachable from state (X) by reading symbol (a)
	for (String state : this.states) {
	    HashMap<String, HashSet<String>> stateTransitions = new HashMap<>();
	    for (String symbol : this.alphabet) {
		// the map is empty here
		stateTransitions.put(symbol, new HashSet<String>());
	    }
	    this.transitionFunction.put(state, stateTransitions);
	}

	// fill in the maps for each symbol for each state
	for (Object[] transitionArray : transitions) {
	    String inState = (String)transitionArray[0];
	    String symbol = (String)transitionArray[1];
	    HashSet<String> set = this.transitionFunction.get(inState).get(symbol);
	    for (Object outState : (Object[])transitionArray[2]) {
		set.add((String)outState);
	    }
	}

	// Now we have the complete transition function,
	// we now compute all the empty string transitions
	// for each state, since they will be used later
	this.emptyStringTransitions = new HashMap<>();
	for (String state : statesWithEmptyStringTransitions) {
	    HashSet<String> allReachableStates = this.transitionFunction.get(state).get("");
	    while (true) {
		int before = allReachableStates.size();
		for (String reachedState : new HashSet<String>(allReachableStates)) {
		    allReachableStates.addAll(this.transitionFunction.get(reachedState).get(""));
		}
		if (before == allReachableStates.size()) {
		    break;
		}
	    }
	    this.emptyStringTransitions.put(state, allReachableStates);
	}
    } // constructor NFA

    public NFA(
        String[] states,
	String[] alphabet,
	Object[][] transitions,
	String startState,
	String[] acceptStates
    ) {
	this(states, alphabet, transitions, startState, acceptStates, false);
    }

    public HashSet<String> move(Collection<String> inputSpace,
				String inputSymbol) {
	HashSet<String> outputSpace = new HashSet<>();
	for (String state : inputSpace) {
	    outputSpace.addAll(
                moveByEmptyString(
                    transitionFunction.get(state).get(inputSymbol)
                )
            );
	}
	return outputSpace;
    }

    public HashSet<String> moveByEmptyString(Collection<String> inputSpace) {
	HashSet<String> outputSpace = new HashSet<>(inputSpace);
	if (isDeterministic || emptyStringTransitions.size() == 0) {
	    return outputSpace;
	}
	for (String state : inputSpace) {
	    // not all states have empty string transitions, thus get may fail
	    // use the safer getOrDefault
	    outputSpace.addAll(emptyStringTransitions.getOrDefault(state, new HashSet<String>()));
	}
	return outputSpace;
    }

    public static enum Computation {Accept, Reject};
    public Computation compute(String input, boolean logging) {
	if (logging) System.out.println("Computing on input '" + input + "'");
	HashSet<String> automataStates = new HashSet<>(Set.<String>of(startState));
	for (char c : input.toCharArray()) {
	    String next = String.valueOf(c);
	    if (!this.alphabet.contains(next)) {
		System.err.println("Un-recoginized symbol '" + next + "'");
		System.exit(1);
	    }
	    if (logging) {
		System.out.println(automataStates + " reading '" + next + "'");
	    }
	    automataStates = move(automataStates, next);
	    if (logging) {
		System.out.println("=> " + automataStates + "\n");
	    }
	}

	Computation result = Computation.Reject;
	for (String state : acceptStates) {
	    if (automataStates.contains(state)) {
		result = Computation.Accept;
		break;
	    }
	}

	if (logging) System.out.println(result + "ed input '" + input + "'");

	return result;
    }

    public Computation compute(String input) {
	return compute(input, true);
    }
} // class NFA
