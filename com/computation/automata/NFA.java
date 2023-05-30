package com.computation.automata;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

// Nondeterministic Finite Automata
public class NFA {
    HashSet<String> states;
    HashSet<String> alphabet;
    HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    HashMap<String, HashSet<String>> emptyStringTransitions;
    String startState;
    HashSet<String> acceptStates;
    boolean isDeterminstic;

    public NFA(
        String[] states,
	// if the empty string is in (alphabet), then it's nondeterministic
	String[] alphabet,
	// {{"state-a", "input-symbol", "state-b"}, ...}
	String[][] transitions,
	String startState,
	String[] acceptStates
    ) {
	this.states = new HashSet<>(Arrays.asList(states));
	this.alphabet = new HashSet<>(Arrays.asList(alphabet));
	this.isDeterminstic = this.alphabet.contains("");
	this.transitionFunction = new HashMap<>();

	String error = "";
	for(String[] transitionArray : transitions) {
	    if (transitionArray.length != 3) {
		error += "Transition array " +
                    Arrays.asList(transitionArray) +
		    " must of the form " +
		    "{\"in-state\", \"input-symbol\", \"state-b\"}" +
		    "\n\n";
	    }

	    String inState = transitionArray[0];
	    String symbol = transitionArray[1];
	    String outState = transitionArray[2];

	    // detect if transition contains a new state not already in the (states) array
	    if (!this.states.contains(inState)) {
		error += (
                    "State '" + inState + "' must be included " +
		    "in the (states) array"
                ) + "\n\n";
	    }

	    // detect if transition contains a new symbol not already in the (alphabet) array
	    if (!this.alphabet.contains(symbol)) {
		error += (
                    "Symbol '" + symbol + "' must be included " +
		    "in the (alphabet) array"
                ) + "\n\n";
	    }

	    // detect if transition contains a new state not already in the (states) array
	    if (!this.states.contains(outState)) {
		error += (
                    "State '" + inState + "' must be included " +
		    "in the (states) array"
                ) + "\n\n";
	    }
	}

	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
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
	for (String[] transitionArray : transitions) {
	    String inState = transitionArray[0];
	    String symbol = transitionArray[1];
	    String outState = transitionArray[2];
	    this.transitionFunction.get(inState).get(symbol).add(outState);
	}

	// Now we have the complete transition function,
	// we now compute all the empty string transitions
	// for each state, since they will be used later
	this.emptyStringTransitions = new HashMap<>();
	for (String state : this.states) {
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
} // class NFA
