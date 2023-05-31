package com.computation.automata;

import java.util.Arrays;
import java.util.Formatter;
import java.util.function.Function;
import java.util.Collection;
import java.util.Set;
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
    HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    HashMap<String, HashSet<String>> emptyStringTransitions;
    String startState;
    HashSet<String> acceptStates;
    boolean isDeterministic;
    NFA deterministicThisCache = null;

    NFA() {}

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

	// store states appearing in transitions of the form {X, "", {A, B, C, ...}}
	// thus states that have transitions labeled with the empty string.
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

	    if (symbol.equals("")) {
		if (isDeterministic) {
		    error.append("This automaton is deterministic, but transition '" +
				 new Formatter().format("{\"%s\", \"\", %s}", inState, Arrays.asList(outStates).toString()).toString() +
				 "' has empty string label\n");
		} else {
		    // if this automaton object is nondeterministic
		    // and current transition has the empty string
		    // then store input state (inState)
		    statesWithEmptyStringTransitions.add(inState);
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
	    for (Object outState : outStates) {
		if (outState instanceof String) {
		    if (!this.states.contains(outState)) {
			error.append(
                            "State '" + outState + "' must be included " +
                            "in the (states) array" + "\n");
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

	// Fill in the maps for each symbol for each state
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
	this.emptyStringTransitions = new HashMap<>();
	// We iterater only through states known to have empty string transition
	for (String state : statesWithEmptyStringTransitions) {
	    // here it's different from for loop before, the last .get("") part
	    // will always succeed because all states in (stateswithemptystringtransitions)
	    // are guaranteed to have at least one empty string transition
	    HashSet<String> allReachableStates = this.transitionFunction.get(state).get("");
	    while (true) {
		int before = allReachableStates.size();
		for (String reachedState : new HashSet<String>(allReachableStates)) {
		    // state stored in (reachedState) may not have empty string transitions
		    // thus calling transitionfunction.get(reachedState).get("") may fail
		    // because (reachedState) may not transitions labeled with ""
		    // Thus we use the safer getOrDefault to protect ourselves from a null
		    allReachableStates.addAll(this.transitionFunction.get(reachedState).getOrDefault("", new HashSet<String>()));
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
	    // for each state X in the input space
	    // add all reachable states from X by reading (inputSymbol)
	    // after that expand the set you just obtained by following
	    // all empty string transitions, if any
	    HashSet<String> x = transitionFunction.get(state).get(inputSymbol);
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
	    outputSpace.addAll(emptyStringTransitions.getOrDefault(state, new HashSet<String>()));
	}
	return outputSpace;
    }

    public NFA toDFA(String failState) {
	if (this.deterministicThisCache != null) {
	    return this.deterministicThisCache;
	}

	this.deterministicThisCache = new NFA();
	this.deterministicThisCache.emptyStringTransitions = new HashMap<String, HashSet<String>>();
	this.deterministicThisCache.isDeterministic = true;

	final HashSet<String> localAlphabet = new HashSet<String>(this.alphabet);
	localAlphabet.remove("");
	this.deterministicThisCache.alphabet = new HashSet<String>(localAlphabet);

	if (isDeterministic) {
	    // This automaton is deterministic
	    // Just return a copy of this automaton
	    this.deterministicThisCache.states = new HashSet<String>(this.states);
	    this.deterministicThisCache.transitionFunction = new HashMap<String, HashMap<String, HashSet<String>>>(transitionFunction);
	    this.deterministicThisCache.startState = String.valueOf(startState);
	    this.deterministicThisCache.acceptStates = new HashSet<String>(acceptStates);

	    return this.deterministicThisCache;
	}

	final Function<Object, String> rep = (x) -> {
	    StringBuilder s = new StringBuilder(x.toString());
	    s.setCharAt(0, '<');
	    s.setCharAt(s.length() - 1, '>');
	    return s.toString();
	};

	ArrayList<TreeSet<String>> powerSet = new ArrayList<>();
	powerSet.add(new TreeSet<>()); // the empty set, the first subset
	HashSet<String> finalSets = new HashSet<>(); // all sets containing at least one final (accept) state
	for (int size = 1; size <= this.states.size(); size++) {
	    // for each number in {1, ..., this.states.size()}
	    int currentSubsets = powerSet.size();
	    for (int i = 0;i < currentSubsets;i++) {
		TreeSet<String> subset = powerSet.get(i);
		// for each subset of this.states
		for (String element : this.states) {
		    // for each state in this.states,
		    // add (element) to this subset forming a new subset
		    TreeSet<String> x = new TreeSet<>(subset);
		    x.add(element);
		    // since we're using an array list to store the subsets
		    // we must ensure we don't have duplicates
		    boolean add = true; // assume this (x) is new
		    for (int j = 0;j < powerSet.size();j++) {
			if (powerSet.get(j).containsAll(x) && x.containsAll(powerSet.get(j))) {
			    // this subset already exists
			    add = false;
			    break;
			}
		    }
		    if (add) {
			powerSet.add(x); // new subset added
			boolean isFinalSet = false;
			for (String finalState : this.acceptStates) {
			    if (x.contains(finalState)) {
				isFinalSet = true;
				break;
			    }
			}
			if (isFinalSet) {
			    // if it contains at least one final state of this automaton
			    // then this (x) is a final state of the deterministic
			    // counterpart of this nondeterministic automaton
			    finalSets.add(rep.apply(x));
			}
		    }
		}
	    }
	}
	powerSet.get(0).add(failState); // "the empty set", which is no longer empty

	// transition function for the deterministic version of (this)
	HashMap<String, HashMap<String, HashSet<String>>> function = new HashMap<>();
	{
	    HashMap<String, HashSet<String>> failStateTransitions = new HashMap<>();
	    for (String symbol : localAlphabet) {
		final HashSet<String> out = new HashSet<>(Set.<String>of( "<" + failState + ">" ));
		failStateTransitions.put(symbol, out);
	    }
	    function.put(rep.apply(powerSet.get(0)), failStateTransitions);
	}

	this.deterministicThisCache.states = new HashSet<String>();
	this.deterministicThisCache.states.add("<" + failState + ">");

	HashSet<String> startSet = expand(Set.<String>of(this.startState));
	String startSetName = null;
	for (int i = 1; i < powerSet.size(); i++) {
	    TreeSet<String> subset = powerSet.get(i);
	    HashMap<String, HashSet<String>> subsetMap = new HashMap<>();
	    for (String symbol : localAlphabet) {
		HashSet<String> x = this.move(subset, symbol);
		if (x.isEmpty()) {
		    x.add(failState);
		}
		subsetMap.put(symbol, new HashSet<String>(Set.<String>of(rep.apply(x))));
	    }
	    String setName = rep.apply(subset);
	    function.put(setName, subsetMap);
	    this.deterministicThisCache.states.add(setName);
	    if (startSetName == null) {
		if (subset.containsAll(startSet) && startSet.containsAll(subset)) {
		    startSetName = setName;
		}
	    }
	}

	this.deterministicThisCache.transitionFunction = function;
	this.deterministicThisCache.startState = startSetName;
	this.deterministicThisCache.acceptStates = finalSets;

	return this.deterministicThisCache;
    }

    public static enum Computation {Accept, Reject};
    public Computation compute(String input, boolean logging) {
	if (logging) System.out.println("Computing on input '" + input + "'");

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
		System.out.println(automatonStates + " reading '" + next + "'");
	    }

	    // the actual computation
	    automatonStates = move(automatonStates, next);

	    if (logging) {
		System.out.println("=> " + automatonStates + "\n");
	    }
	    if (automatonStates.isEmpty()) {
		// No new states will be reached
		// if there are no states at all
		// So if we get an empty set during computation
		// just stop.
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

	if (logging) System.out.println(result + "ed input '" + input + "'");

	return result;
    }

    public Computation compute(String input) {
	return compute(input, true);
    }
} // class NFA
