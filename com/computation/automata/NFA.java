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
    // keys in (HashMap<String, HashSet<String>>) are alphabet symbols
    // WHENEVER USING (transitionFunction) USE getOrDefault.
    HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    HashMap<String, HashSet<String>> emptyStringTransitions;
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

    private HashSet<String> epsilonStates(String state) {
	return this.emptyStringTransitions.getOrDefault(state, new HashSet<String>());
    }

    NFA() {
	this.states = new HashSet<>();
	this.alphabet = new HashSet<>();
	this.transitionFunction = new HashMap<>();
	this.emptyStringTransitions = new HashMap<>();
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
	this.emptyStringTransitions = new HashMap<>();

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

	    if (isDeterministic && symbol.equals("")) {
		error.append("This automaton is deterministic, but transition '" +
                    "{\"%s\", \"\", %s}".formatted(inState, outStates.toString()) +
                    "' has empty string label\n");
	    }

	    // Construct transition function and
	    // partially compute empty string transitions.
	    // Empty string transitions will be fully computed later
	    // when the transition function is fully computed.
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

		// Empty string transitions.
		if (!isDeterministic && symbol.equals("")) {
		    HashSet<String> set = epsilonStates(inState);
		    if (set.isEmpty()) {
			this.emptyStringTransitions.put(inState, set);
		    }
		    set.addAll(outStates);
		}
	    }
	}

	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	} else {
	    this.acceptStates = new HashSet<>(Set.<String>of(acceptStates));
	}

	// Now we have the complete transition function,
	// we now compute all the empty string transitions
	// We iterater only through states known to have empty string transition
	for (Map.Entry<String, HashSet<String>> e : this.emptyStringTransitions.entrySet()) {
	    HashSet<String> allReachableStates = symbolTransitionsOf(e.getKey(), "");
	    while (true) {
		int before = allReachableStates.size();
		for (String reachedState : new HashSet<>(allReachableStates)) {
		    // Not all states stored in (reachedState) have empty string transitions
		    // thus calling transitionFunction.get(reachedState).get("") may fail
		    // because (reachedState) may not have transitions labeled with ""
		    // Thus we use the safer getOrDefault to protect ourselves from a null
		    allReachableStates.addAll(symbolTransitionsOf(reachedState, ""));
		}
		if (before == allReachableStates.size()) {
		    // we fully expanded the transitions set.
		    // no more new states are reachable
		    break;
		}
	    }
	    // store full expanded path for this state
	    // now we know all the states we can reach when reading
	    // an empty string on this (state)
	    e.setValue(allReachableStates);
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
	    // for each state X in the input space
	    // add all reachable states from X by reading (inputSymbol)
	    // after that expand the set you just obtained by following
	    // all empty string transitions, if any

	    // The use of getOrDefault with transitionFunction is that
	    // the map (transitionFunction) has key only those states
	    // with transitions going out of them
	    // Thus if a state (q) is reached but once the automaton
	    // is there it can not go to other state, then (q)
	    // will not be a key in (transitionFunction)
	    // Such state (q) can used as a sink (failure) state
	    // When the automaton can not recover anymore.

	    // The second use of getOrDefault is because
	    // even those states who made it as keys in (transitionFunction)
	    // their associated maps do not contain
	    // every single alphabet symbol as keys
	    // Thus if a state (q) has transitions with labels {a1, a2, ..., an}
	    // then the keys its map are just those {a1, a2, ..., an}
	    // and nothing else.
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
	if (emptyStringTransitions.isEmpty()) {
	    // If this automaton has empty string transitions
	    // then this method will effectively do nothing
	    // so just return the input set alone.
	    return outputSpace;
	}
	for (String state : inputSpace) {
	    // not all states have empty string transitions
	    // thus calling get on emptyStringTransitions may fail
	    // Hence, use the safer getOrDefault
	    outputSpace.addAll(epsilonStates(state));
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
	if (first.states.contains(newStartState)) {
	    System.err.println(
                "First automaton already has state '" +
		newStartState + "'");
	    System.exit(1);
	}
	if (second.states.contains(newStartState)) {
	    System.err.println(
                "Second automaton already has state '" +
		newStartState + "'");
	    System.exit(1);
	}
	if (suffix.isEmpty()) {
	    System.err.println("Provided suffix must be non-empty string.");
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
	    String stateName = state;
	    if (union.states.contains(state)) {
		stateName += suffix;
		duplicateStates.add(state);
	    }
	    union.states.add(stateName);
	}

	union.acceptStates.addAll(first.acceptStates);
	for (String state : second.acceptStates) {
	    // Do the same as above when adding states to (union)
	    // accept states must also be handled similary
	    // to avoid name clashes.
	    String stateName = state;
	    if (duplicateStates.contains(state)) {
		stateName += suffix;
	    }
	    union.acceptStates.add(stateName);
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

	// Construct empty string transitions map.
	union.emptyStringTransitions.putAll(first.emptyStringTransitions);
	for (String state : second.emptyStringTransitions.keySet()) {
	    String stateName = state;
	    if (duplicateStates.contains(state)) {
		stateName += suffix;
	    }
	    HashSet<String> correctedSet = new HashSet<>();
	    for (String x : second.emptyStringTransitions.get(state)) {
		// If (x) is a duplicate state, suffix it with value of (suffix)
		// so it refers to (x) in (second) not in (first).
		// This is because we're handling transitions in (second)
		// so if state (q) is in both (first) and (second)
		// we interpreter it as belonging to (second).
		correctedSet.add(
                    x + (duplicateStates.contains(x) ? suffix : "")
                );
	    }
	    union.emptyStringTransitions.put(stateName, correctedSet);
	}

	// the new start state has two empty string transitions
	// one leading to start state of (first)
	// another leading to start state of (second)
	union.emptyStringTransitions.put(newStartState,
            new HashSet<String>(
                Set.<String>of(
                    first.startState,
		    // Append the value of (suffix) to name of the start state in (second)
		    // if it already exist in (first).
		    // Otherwise, do nothing.
		    second.startState +
		    (duplicateStates.contains(second.startState) ? suffix : "")
                )
            )
        );

	return union;
    }
} // class NFA
