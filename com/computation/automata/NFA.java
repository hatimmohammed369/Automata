package com.computation.automata;

import java.util.function.Function;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;

// Nondeterministic Finite Automata
public class NFA {
    HashSet<String> states;
    HashSet<String> alphabet;
    // in (transitionFunction) keys are states
    // keys in (HashMap<String, HashSet<String>>) are alphabet symbols (regular expressions in GNFAs)
    HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    String startState;
    HashSet<String> acceptStates;
    boolean isDeterministic;
    // this field stays null for deterministic automata
    // since calling toDFA just returns this object
    // which is already deterministic
    NFA thisDFACache = null;

    private HashMap<String, HashSet<String>> mapOf(String state) {
	return transitionFunction.getOrDefault(
            state, new HashMap<String, HashSet<String>>());
    }

    private HashSet<String> symbolTransitionsOf(String state, String symbol) {
	return mapOf(state).getOrDefault(symbol, new HashSet<String>());
    }

    NFA() {
	this.states = new HashSet<>();
	this.alphabet = new HashSet<>();
	this.transitionFunction = new HashMap<>();
	this.startState = null;
	this.acceptStates = new HashSet<>();
	this.isDeterministic = false;
    }

    public NFA(
        String[] states,
	// if the empty string is in (alphabet), then it's nondeterministic
	String[] alphabet,
	// {{"state-a", "input-symbol", {"state-0", "state-1", "state-2", ...}}, ...}
	Object[][] transitions,
	String startState,
	String[] acceptStates,
	boolean isDeterministic // manually set determinism, you human know
    ) {
	this.states = new HashSet<>(Set.<String>of(states));

	StringBuilder error = new StringBuilder();
	if (!this.states.contains(startState)) {
	    // start state not in argument value of states
	    error.append("Start state '" + startState + "' must be included in the (states) array" + "\n");
	} else {
	    this.startState = startState;
	}

	this.alphabet = new HashSet<>(Set.<String>of(alphabet));
	this.transitionFunction = new HashMap<>();

	boolean emptyStringTransitionFound = false;
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
	    ArrayList<String> outStates = new ArrayList<>();
	    {
		Object[] x = (Object[])transitionArray[2];
		for(int i = 0; i < x.length; i++) {
		    try {
			outStates.add((String)x[i]);
		    } catch (ClassCastException e) {
			error.append(
                            "Element %s in transition {\"%s\", \"%s\", %s} must be a String object".
			    formatted(x[i], inState, symbol, Arrays.asList(x))
                        );
		    }
		}
	    }

	    // detect if input state in this transition
	    // contains a new state not already in the (states) array
	    if (!this.states.contains(inState)) {
		error.append(
                    "State '" + inState + "' must be included " +
		    "in the (states) array" + "\n");
	    }

	    // detect if transition contains a new symbol not already in the (alphabet) array
	    if (!this.alphabet.contains(symbol)) {
		error.append(
                    "Symbol '" + symbol + "' must be included " +
		    "in the (alphabet) array" + "\n");
	    }

	    // detect if transition output set contains a new state not already in the (states) array
	    for (String outState : outStates) {
		if (!this.states.contains(outState)) {
		    error.append(
                       "State '" + outState + "' must be included " +
                        "in the (states) array" + "\n");
		}
	    }

	    if (symbol.equals("")) {
		emptyStringTransitionFound = true;
		if (isDeterministic) {
		    // Since the error string becomes non-empty
		    // we can not have (isDeterministic) true
		    // when empty string transitions exist in an NFA object.
		    error.append("This automaton is deterministic, but transition '" +
				 "{\"%s\", \"\", %s}".formatted(inState, outStates.toString()) +
				 "' has empty string label\n");
		}
	    }

	    // Construct transition function and
	    // partially compute empty string transitions.
	    if (error.isEmpty()) {
		HashMap<String, HashSet<String>> inStateSymbolsMap = mapOf(inState);
		if (inStateSymbolsMap.isEmpty()) {
		    this.transitionFunction.put(inState, inStateSymbolsMap);
		}
		HashSet<String> symbolOutputsSet = inStateSymbolsMap.getOrDefault(symbol, new HashSet<String>());
		if (symbolOutputsSet.isEmpty()) {
		    inStateSymbolsMap.put(symbol, symbolOutputsSet);
		}
		symbolOutputsSet.addAll(outStates);

	    }
	}

	if (!isDeterministic && !emptyStringTransitionFound) {
	    // DFAs are NFAs by definition
	    // but no need to have (isDeterministic) false
	    // when there's no empty string transitions.
	    isDeterministic = true;
	}

	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	}

	this.acceptStates = new HashSet<>(Set.<String>of(acceptStates));
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
	HashSet<String> inputSet = new HashSet<String>(expand(inputSpace));
	for (String state : inputSet) {
	    // for each state X in the input space
	    // add all reachable states from X by reading (inputSymbol)
	    // after that expand the set you just obtained by following
	    // all empty string transitions, if any
	    HashSet<String> x = symbolTransitionsOf(state, inputSymbol);
	    HashSet<String> y = expand(x);
	    outputSpace.addAll(y);
	}
	return outputSpace;
    }

    // Return all states reachable from the input set with
    // 0 or more empty string transitions
    // Thus the input set is always part of return value
    public HashSet<String> expand(Collection<String> inputSpace) {
	// Start with the input set
	HashSet<String> outputSpace = new HashSet<>(inputSpace);
	if (isDeterministic) {
	    // no empty string transitions
	    // just return the input set
	    return outputSpace;
	}
	// Expand the input set.
	// For state (q) in the input set,
	// add states you can reach from (q) using
	// a single empty string transition.
	for (String state : inputSpace) {
	    outputSpace.addAll(symbolTransitionsOf(state, ""));
	}
	// Expand the output set itself now.
	while (true) {
	    int before = outputSpace.size();
	    for (String state : new HashSet<>(outputSpace)) {
		outputSpace.addAll(symbolTransitionsOf(state, ""));
	    }
	    if (before == outputSpace.size()) {
		break;
	    }
	}
	return outputSpace;
    }

    public static enum Computation {Accept, Reject};
    public Computation compute(String input, boolean logging) {
	if (logging) System.err.println("Computing on input '" + input + "'");

	// expand the start set with all states reachable from the start start
	// with empty string transitions
	HashSet<String> automatonStates = expand(Set.<String>of(startState));

	for (char c : input.toCharArray()) {
	    String next = String.valueOf(c);
	    if (!this.alphabet.contains(next)) {
		System.err.println("Un-recoginized symbol '" + next + "'");
		System.exit(1);
	    }
	    if (logging) {
		System.err.println(automatonStates + " reading '" + next + "'");
	    }

	    // the actual computation
	    automatonStates = move(automatonStates, next);

	    if (logging) {
		System.err.println("=> " + automatonStates + "\n");
	    }
	    if (automatonStates.isEmpty()) {
		// No new states will be reached
		// if there are no states at all
		// So if we get an empty set during computation
		// just stop.
		System.err.println("Early aborting computation because the automaton lost all states");
		break;
	    }
	}

	Computation result = Computation.Reject;
	for (String state : acceptStates) {
	    if (automatonStates.contains(state)) {
		result = Computation.Accept;
		break;
	    }
	}

	if (logging) System.err.println(result + "ed input '" + input + "'");

	return result;
    }

    public Computation compute(String input) {
	return compute(input, true);
    }

    public NFA toDFA(String failState) {
	if (isDeterministic) return this;
	if (thisDFACache != null) return thisDFACache; // no need to redo the computation

	// constructor NFA() initializes all fileds
	thisDFACache = new NFA();

	thisDFACache.isDeterministic = true; // by definition

	thisDFACache.alphabet.addAll(this.alphabet);
	thisDFACache.alphabet.remove(""); // remove the empty string if present

	final Function<Object, String> nameStyle = (x) -> {
	    StringBuilder s = new StringBuilder(x.toString());
	    s.setCharAt(0, '<');
	    s.setCharAt(s.length() - 1, '>');
	    return s.toString();
	};
	thisDFACache.startState = nameStyle.apply(expand(Set.<String>of(this.startState)));

	ArrayList<HashSet<String>> powerSet = new ArrayList<>();
	powerSet.add(new HashSet<String>()); // the empty set

	final String failStateName = "<" + failState + ">";
	for (int size = 1; size <= this.states.size(); size++) {
	    int current = powerSet.size();
	    for (int i = 0; i < current; i++) {
		HashSet<String> subset = powerSet.get(i);
		for (String state : this.states) {
		    HashSet<String> newSubset = new HashSet<>(subset);
		    newSubset.add(state);

		    // make sure this (newSubset) is actually "new"
		    // search through all (powerSet),
		    // if this (newSubset) matches something there
		    // we will not add it to (powerSet)
		    // otherwise we will add it to (powerSet)
		    boolean addNewSubset = true; // assume it's new
		    for (int j = 0; j < powerSet.size(); j++) {
			if (newSubset.containsAll(powerSet.get(j)) && powerSet.get(j).containsAll(newSubset)) {
			    // it's a subset of powerSet.get(j) and powerSet.get(j) is a subset of it
			    // thus newSubset is just powerSet.get(j)
			    // it's already there, do not allow it
			    addNewSubset = false;
			    break;
			}
		    }

		    if (addNewSubset) {
			powerSet.add(newSubset); // so that we continue this iteration

			final String newSubsetName = nameStyle.apply(newSubset);
			thisDFACache.states.add(newSubsetName);

			// check to see if it contains an accept state
			// because if then it's an accept state of the deterministic
			// counterpart of this nondeterministic automaton object
			for (String acceptState : this.acceptStates) {
			    if (newSubset.contains(acceptState)) {
				thisDFACache.acceptStates.add(newSubsetName);
				break;
			    }
			}

			// add all of its transitions
			HashMap<String, HashSet<String>> newSubsetMap = new HashMap<>();
			for (String symbol : thisDFACache.alphabet) {
			    HashSet<String> symbolOutputs = new HashSet<>();
			    HashSet<String> x = move(newSubset, symbol);
			    if (x.isEmpty()) {
				// all states in (newSubset) do not move further
				// when given input (symbol), thus the expression
				// move(newSubset, symbol) yields the empty set
				// which we represent as fail state
				symbolOutputs.add(failStateName);
			    } else {
				symbolOutputs.add(nameStyle.apply(x));
			    }
			    newSubsetMap.put(symbol, symbolOutputs);
			}
			thisDFACache.transitionFunction.put(newSubsetName, newSubsetMap);
		    }
		}
	    }
	}

	// Add fail state and its transitions
	thisDFACache.states.add(failStateName);
	final HashMap<String, HashSet<String>> failStateTransitions = new HashMap<>();
	for (String symbol : thisDFACache.alphabet) {
	    // for each symbol fail state receives,
	    // it just goes back to itself.
	    // each of its transitions are {failStatename, symbol, {failStateName}}
	    failStateTransitions.put(symbol, new HashSet<>(Set.<String>of(failStateName)));
	}
	thisDFACache.transitionFunction.put(failStateName, failStateTransitions);

	return thisDFACache;
    }

    public static NFA union(NFA first, NFA second, String newStartState, String suffix) {
	StringBuilder error = new StringBuilder();
	if (suffix.isEmpty()) {
	    error.append("Naming suffix must be non-empty string.\n");
	}
	if (first.states.contains(newStartState)) {
	    error.append("New start state is already in first automaton.\n");
	}
	if (second.states.contains(newStartState)) {
	    error.append("New start state is already in second automaton.\n");
	}
	if (first.states.contains(newStartState)) {
	    error.append(
                "First automaton already has state '" +
		newStartState + "'\n");
	}
	if (second.states.contains(newStartState)) {
	    error.append(
                "Second automaton already has state '" +
		newStartState + "'\n");
	}
	if (suffix.isEmpty()) {
	    error.append("Provided suffix must be non-empty string.\n");
	}
	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	}

	NFA union = new NFA();
	union.startState = newStartState; // Set start state for the union automaton.
	union.states.add(newStartState);
	union.isDeterministic = false;

	union.alphabet.addAll(first.alphabet);
	union.alphabet.addAll(second.alphabet);
	// Manually add the empty string to the alphabet
	// just in case both (first) and (second) are deterministic.
	union.alphabet.add("");

	HashSet<String> duplicateStates = new HashSet<>(); // Store states in (second) also in (first).
	union.states.addAll(first.states);
	for (String state : second.states) {
	    // If this state, which is from (second),
	    // is already in (union.states), append string in (suffix)
	    // so that names don't clash and the two automata
	    // operate separately when running (union).
	    if (union.states.contains(state)) {
		duplicateStates.add(state);
		state += suffix;
	    }
	    union.states.add(state);
	}

	union.acceptStates.addAll(first.acceptStates);
	for (String state : second.acceptStates) {
	    // Do the same as above when adding states to (union)
	    // accept states must also be handled similary
	    // to avoid name clashes.
	    if (duplicateStates.contains(state)) {
		state += suffix;
	    }
	    union.acceptStates.add(state);
	}

	// Construct transition function.
	// First, put all transitions in the first automaton.
	union.transitionFunction.putAll(first.transitionFunction);
	// Second, handle transitions in the second automaton.
	for (String state : second.states) {
	    // If (state) is in both (first) and (second),
	    // do not use its name, rather append value of (suffix)
	    String stateName = state;
	    if (duplicateStates.contains(state)) {
		stateName += suffix;
	    }
	    HashMap<String, HashSet<String>> correctedMap = new HashMap<>();
	    for (Map.Entry<String, HashSet<String>> e : second.mapOf(state).entrySet()) {
		// e.getKey() is a symbol from second.alphabet.
		// e.getValue() is all states reachable from (state)
		// when reading symbol e.getKey()
		HashSet<String> correctedSet = new HashSet<>();
		for (String x : e.getValue()) {
		    // If (x) is a duplicate state, suffix it with value of (suffix)
		    // so it refers to (x) in (second) not in (first).
		    // This is because we're handling transitions in (second)
		    // so if state (q) is in both (first) and (second)
		    // we interpreter it as belonging to (second).
		    correctedSet.add(
                        x + (duplicateStates.contains(x) ? suffix : "")
                    );
		}
		correctedMap.put(e.getKey(), correctedSet);
	    }
	    union.transitionFunction.put(stateName, correctedMap);
	}

	// Add transitions of the new start state.
	// It has only two transitions, both of which are empty string transitions
	// one leading to start state of (first)
	// another leading to start state of (second).
	final HashMap<String, HashSet<String>> newStartStateTransitions =
	    new HashMap<>();
	newStartStateTransitions.put("",
            new HashSet<String>(
                Set.<String>of(
                    first.startState,
		    // Append (suffix) to name of the start state in (second)
		    // if it already exist in (first).
		    // Otherwise, do nothing.
		    second.startState +
		    (duplicateStates.contains(second.startState) ? suffix : "")
                )
            )
        );
	union.transitionFunction.put(
            newStartState, newStartStateTransitions);

	return union;
    }

    public static NFA concatenate(NFA first, NFA second, String suffix) {
	if (suffix.isEmpty()) {
	    System.err.println("Naming suffix must be non-empty string.");
	    System.exit(1);
	}
	NFA result = new NFA();
	result.isDeterministic = false;

	result.alphabet.addAll(first.alphabet);
	result.alphabet.addAll(second.alphabet);
	result.alphabet.add("");

	// Add all states in first automaton
	result.states.addAll(first.states);
	HashSet<String> duplicateStates = new HashSet<>();
	// Add all states in second automaton, but
	// a state (q) is in both (first) and (second)
	// suffix its name with string (suffix).
	for (String state : second.states) {
	    if (result.states.contains(state)) {
		duplicateStates.add(state);
		state += suffix;
	    }
	    result.states.add(state);
	}

	// Set start state, which is the start state in (first)
	result.startState = first.startState;

	// Set the accept states
	// Which are the accept states in (second)
	// with (suffix) appended when accept state (q) in (second)
	// is a state in (first).
	for (String state : second.acceptStates) {
	    if (duplicateStates.contains(state)) {
		state += suffix;
	    }
	    result.acceptStates.add(state);
	}

	// Add all transitions in first automaton
	result.transitionFunction.putAll(first.transitionFunction);
	// Add all transitions in second automaton
	// but when state (q) in (second) appears either as
	// a key in the transitions map or in the outputs set in
	// some map in the transitions map, suffix that state (q).
	for (Map.Entry<String,HashMap<String,HashSet<String>>> stateMapPair : Map.copyOf(second.transitionFunction).entrySet()) {
	    HashMap<String, HashSet<String>> stateCorrectedMap = stateMapPair.getValue();
	    for (Map.Entry<String, HashSet<String>> symbolSetPair : stateCorrectedMap.entrySet()) {
		HashSet<String> set = symbolSetPair.getValue();
		for (String state : set.toArray(new String[]{})) {
		    if (duplicateStates.contains(state)) {
			set.remove(state);
			set.add(state + suffix);
		    }
		}
	    }
	    String state = stateMapPair.getKey();
	    if (duplicateStates.contains(state)) {
		result.transitionFunction.remove(state);
		result.transitionFunction.put(state + suffix, stateCorrectedMap);
	    }
	}

	return result;
    }

    public static NFA star(NFA automaton, String newStartState) {
	if (automaton.states.contains(newStartState)) {
	    System.err.println("New start state is already in this automaton.");
	    System.exit(1);
	}
	NFA result = new NFA();
	result.startState = newStartState;
	result.isDeterministic = false;

	result.alphabet.addAll(automaton.alphabet);
	result.alphabet.add("");

	result.states.addAll(automaton.states);
	result.states.add(newStartState);

	result.acceptStates.addAll(automaton.acceptStates);
	result.acceptStates.add(newStartState);

	result.transitionFunction.putAll(automaton.transitionFunction);

	// For each accept state in (automaton)
	// add an empty string transition leading
	// to the new start state
	for (String state : automaton.acceptStates) {
	    // Add transition in (transitionFunction).
	    HashMap<String, HashSet<String>> stateMap = automaton.mapOf(state);
	    if (stateMap.isEmpty()) {
		result.transitionFunction.put(state, stateMap);
	    }
	    stateMap.put("",
                new HashSet<String>(Set.<String>of(newStartState))
            );
	}

	// Add one empty string transition
	// for the new start state
	// leading to the start state in (automaton)
	result.transitionFunction.put(newStartState,
            new HashMap<String, HashSet<String>>()
        );
	result.mapOf(newStartState).put(
            "", new HashSet<String>(Set.<String>of(automaton.startState))
        );

	return result;
    }
} // class NFA
