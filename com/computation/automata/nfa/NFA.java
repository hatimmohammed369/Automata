package com.computation.automata.nfa;

import java.util.function.Function;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;

// Nondeterministic Finite Automata
public class NFA {
    public HashSet<String> states;
    public HashSet<String> alphabet;
    // in (transitionFunction) keys are states
    // keys in (HashMap<String, HashSet<String>>) are alphabet symbols (regular expressions in GNFAs)
    public HashMap<String, HashMap<String, HashSet<String>>> transitionFunction;
    public String startState;
    public HashSet<String> acceptStates;
    public boolean isDeterministic;
    // this field stays null for deterministic automata
    // since calling toDFA just returns this object
    // which is already deterministic
    NFA thisDFACache = null;

    private HashMap<String, HashSet<String>> symbolsMapOf(String state) {
	return transitionFunction.getOrDefault(
            state, new HashMap<String, HashSet<String>>());
    }

    private HashSet<String> symbolTransitionsOf(String state, String symbol) {
	return symbolsMapOf(state).getOrDefault(symbol, new HashSet<String>());
    }

    private void putTransition(String state, String symbol, Collection<String> outputSet) {
	HashMap<String, HashSet<String>> stateMap = symbolsMapOf(state);
	if (stateMap.isEmpty()) {
	    // this state does not an associated symbols map.
	    // Add it to (transitionFunction) map.
	    transitionFunction.put(state, stateMap);
	}
	HashSet<String> symbolOutputSet = stateMap.getOrDefault(symbol, new HashSet<String>());
	if (symbolOutputSet.isEmpty()) {
	    // this (state) has no transition labeled with (symbol).
	    stateMap.put(symbol, symbolOutputSet);
	}
	symbolOutputSet.addAll(outputSet);
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
        Collection<String> states,
	// if the empty string is in (alphabet), then it's nondeterministic
	Collection<String> alphabet,
	// Object[]:{{string:"state-a", string:"input-symbol", set:{"state-0", "state-1", "state-2", ...}}, ...}
	Object[][] transitions,
	String startState,
	Collection<String> acceptStates,
	boolean isDeterministic // manually set determinism, you human know
    ) {
	this.isDeterministic = isDeterministic;
	this.states = new HashSet<>(states);

	StringBuilder error = new StringBuilder();
	if (!this.states.contains(startState)) {
	    // start state not in argument value of states
	    error.append("Start state '" + startState + "' must be included in the (states) array" + "\n");
	} else {
	    this.startState = startState;
	}

	this.alphabet = new HashSet<>(alphabet);
	this.transitionFunction = new HashMap<>();

	// use this boolean to set (isDeterministic) to true
	// if not empty string transitions found.
	boolean emptyStringTransitionFound = false;
	for(Object[] transitionArray : transitions) {
	    if (transitionArray.length != 3) {
		error.append("Transition array " +
                    List.of(transitionArray) +
		    " must of the form " +
		    "{{\"state-a\", \"input-symbol\", {\"state-0\", \"state-1\", \"state-2\", ...}}, ...}\"" +
                    "\n");
	    }

	    String state = (String)transitionArray[0];
	    String symbol = (String)transitionArray[1];
	    ArrayList<String> outputSet = new ArrayList<>();
	    for (Object o : List.of((Object[])transitionArray[2])) {
		outputSet.add((String)o);
	    }

	    // detect if input state in this transition
	    // contains a new state not already in the (states) array
	    if (!this.states.contains(state)) {
		error.append(
                    "State '" + state + "' must be included " +
		    "in the (states) array" + "\n");
	    }

	    // detect if transition contains a new symbol not already in the (alphabet) array
	    if (!this.alphabet.contains(symbol)) {
		error.append(
                    "Symbol '" + symbol + "' must be included " +
		    "in the (alphabet) array" + "\n");
	    }

	    // detect if transition output set contains a new state not already in the (states) array
	    for (String outState : outputSet) {
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
				 "{\"%s\", \"\", %s}".formatted(state, outputSet) +
				 "' has empty string label\n");
		}
	    }

	    // Construct transition function and
	    if (error.isEmpty()) {
		putTransition(state, symbol, outputSet);
	    }
	}

	if (!isDeterministic && !emptyStringTransitionFound) {
	    // DFAs are NFAs by definition
	    // but no need to have (isDeterministic) false
	    // when there's no empty string transitions.
	    isDeterministic = true;
	}

	this.acceptStates = new HashSet<>(acceptStates);

	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	}
    } // constructor NFA

    public NFA(
        Collection<String> states,
	Collection<String> alphabet,
	Object[][] transitions,
	String startState,
	Collection<String> acceptStates
    ) {
	this(states, alphabet, transitions, startState, acceptStates, false);
    }

    public HashSet<String> move(Collection<String> inputSet,
				String inputSymbol) {
	HashSet<String> outputSet = new HashSet<>();
	// Iterate over the expanded input set.
	for (String state : expand(inputSet)) {
	    // for each state X in the input space
	    // add all reachable states from X by reading (inputSymbol)
	    // after that expand the set you just obtained by following
	    // all empty string transitions, if any
	    HashSet<String> x = symbolTransitionsOf(state, inputSymbol);
	    HashSet<String> y = expand(x);
	    outputSet.addAll(y);
	}
	return outputSet;
    }

    // Return all states reachable from the input set with
    // 0 or more empty string transitions
    // Thus the input set is always part of return value
    public HashSet<String> expand(Collection<String> inputSet) {
	// Start with the input set
	HashSet<String> outputSet = new HashSet<>(inputSet);
	if (isDeterministic || inputSet.isEmpty()) {
	    // no empty string transitions
	    // just return the input set
	    return outputSet;
	}
	// Expand the input set.
	// For state (q) in the input set,
	// add all states you can reach from (q) using
	// a single empty string transition.
	for (String state : inputSet) {
	    outputSet.addAll(symbolTransitionsOf(state, ""));
	}
	// Expand the output set itself now.
	while (true) {
	    int before = outputSet.size();
	    for (String state : new HashSet<>(outputSet)) {
		outputSet.addAll(symbolTransitionsOf(state, ""));
	    }
	    if (before == outputSet.size()) {
		break;
	    }
	}
	return outputSet;
    }

    public static enum Computation {Accept, Reject};
    public Computation compute(String input, boolean logging) {
	if (logging) System.err.println("Computing on input '" + input + "'");

	// expand the start set with all states reachable from the start start
	// with empty string transitions
	HashSet<String> automatonStates = expand(List.of(startState));

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
	    if (logging && automatonStates.isEmpty()) {
		// No new states will be reached
		// if there are no states at all
		// So if we get an empty set during computation
		// just stop.
		System.err.println("Early aborting computation because the automaton lost all states");
		break;
	    }
	}

	Computation result = Computation.Reject;
	if (!automatonStates.isEmpty()) {
	    for (String state : acceptStates) {
		if (automatonStates.contains(state)) {
		    result = Computation.Accept;
		    break;
		}
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

	thisDFACache.startState = nameStyle.apply(expand(List.of(this.startState)));

	ArrayList<HashSet<String>> powerSet = new ArrayList<>();
	powerSet.add(new HashSet<String>()); // the empty set

	// At first, mark all states as "unreachable"
	HashSet<String> notReachableStates = new HashSet<>();
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
			notReachableStates.add(nameStyle.apply(newSubset));

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
				// But when some state appears in the output set
				// of some transition, remove it from the "unreachable" list
				notReachableStates.remove(nameStyle.apply(x));
			    }
			    newSubsetMap.put(symbol, symbolOutputs);
			}
			thisDFACache.transitionFunction.put(newSubsetName, newSubsetMap);
		    }
		}
	    }
	}
	// DONT FORGET TO REMOVE THE START STATE FROM "unreachable" LIST
	notReachableStates.remove(thisDFACache.startState);
	for (String s : notReachableStates) {
	    thisDFACache.transitionFunction.remove(s);
	}

	// Add fail state and its transitions
	thisDFACache.states.add(failStateName);
	final HashMap<String, HashSet<String>> failStateTransitions = new HashMap<>();
	for (String symbol : thisDFACache.alphabet) {
	    // for each symbol fail state receives,
	    // it just goes back to itself.
	    // each of its transitions are {failStatename, symbol, {failStateName}}
	    failStateTransitions.put(symbol, new HashSet<>(List.of(failStateName)));
	}
	thisDFACache.transitionFunction.put(failStateName, failStateTransitions);

	return thisDFACache;
    }

    // IF EXPRESSION IS NOT THE EMPTY STRING,
    // NOT AN ALPHABET, AND NOT ALREADY PARAENTHESIZED
    // THEN SURROUND IT WITH PARENTHESIS.
    // (null) REPRESENTS THE EMPTY SET.
    private String starRegex(String expr) {
	if (expr == null) {
	    // Staring the empty set gives the empty string
	    return "";
	}

	if (expr.isEmpty()) {
	    // Starring the empty string gives the empty string.
	    return "";
	}

	if (this.alphabet.contains(expr)) {
	    expr += "*";
	} else if (expr.startsWith("(") && expr.endsWith(")")) {
	    int nesting = 0;
	    for (int i = 0;i < expr.length();i++) {
		char c = expr.charAt(i);
		if (c == '(') nesting++;
		else if (c == ')') nesting--;

		if (nesting == 0 && i != expr.length()-1) {
		    nesting = -1;
		    break;
		}
	    }
	    if (nesting == -1) {
		// First ( does not macth last )
		// Add parenthesis
		expr = "(" + expr + ")*";
	    } else {
		// First ( matches last )
		// add star
		expr += "*";
	    }
	} else {
	    expr = "(" + expr + ")*";
	}

	return expr;
    }

    private String unionRegexes(Collection<String> exprs) {
	if (exprs == null) return null;

	StringBuilder unionExpr = new StringBuilder();

	boolean allNulls = true;
	for (String expr : exprs) {
	    if (expr == null) {
		// Adding the empty language has no effect.
		continue;
	    }
	    int nesting = 0;
	    for (int i = 0;i < expr.length();i++) {
		char c = expr.charAt(i);
		if (c == '(') nesting++;
		else if (c == ')') nesting--;

		if (nesting == 0 && c == '|') {
		    nesting = -1;
		    break;
		}
	    }
	    if (nesting == -1) {
		// (expr) is in form A|B|C|...
		// add parenthesis to perserve the union operation.
		expr = "(" + expr + ")";
	    }
	    unionExpr.append(expr + "|");
	    allNulls = false;
	}

	if (allNulls) return null;

	if (!unionExpr.isEmpty()) {
	     // delete trailing |
	    unionExpr.deleteCharAt(unionExpr.length() - 1);
	} else {
	    return "";
	}

	return unionExpr.toString();
    }

    private String concatRegexes(Collection<String> exprs) {
	if (exprs == null) return null;

	StringBuilder concatExpr = new StringBuilder();

	for (String expr : exprs) {
	    if (expr == null) {
		// Concentating with the empty set yields the empty set.
		return null;
	    }
	    if (expr.isEmpty()) {
		// Concatenating with the empty string has no effect.
		continue;
	    }
	    int nesting = 0;
	    for (int i = 0;i < expr.length();i++) {
		char c = expr.charAt(i);
		if (c == '(') nesting++;
		else if (c == ')') nesting--;

		if (nesting == 0 && c == '|') {
		    nesting = -1;
		    break;
		}
	    }
	    if (nesting == -1) {
		// (expr) is in form A|B|C|...
		// add parenthesis to perserve the union operation.
		expr = "(" + expr + ")";
	    }
	    concatExpr.append(expr);
	}

	return concatExpr.toString();
    }

    public String toRegularExpression(List<String> removalSequence, String gStartState, String gAcceptState) {
	if (!isDeterministic) {
	    System.err.println("Invoking automaton must be deterministic");
	    System.exit(1);
	}
	if (this.states.contains(gStartState)) {
	    System.err.println("Choose another start state");
	    System.exit(1);
	}
	if (this.states.contains(gAcceptState)) {
	    System.err.println("Choose another start accept");
	    System.exit(1);
	}
	{
	    ArrayList<String> missing = new ArrayList<>();
	    for (String state : this.states) {
		if (!removalSequence.contains(state)) {
		    missing.add(state);
		}
	    }
	    if (!missing.isEmpty()) {
		System.err.println("States " +missing+ " are not removed");
		System.exit(1);
	    }
	}
	HashMap<String, HashMap<String, ArrayList<String>>> function =
	    new HashMap<>();
	this.transitionFunction.forEach((state, symbolsMap) -> {
	    HashMap<String, ArrayList<String>> statesMap = function.getOrDefault(state, new HashMap<>());
	    if (statesMap.isEmpty()) function.put(state, statesMap);
	    symbolsMap.forEach((symbol, symbolSet) -> {
		    for (String q : symbolSet) {
			ArrayList<String> qSet = statesMap.getOrDefault(q, new ArrayList<>());
			if (qSet.isEmpty()) statesMap.put(q, qSet);
			qSet.add(symbol);
		    }
	    });
	    if (this.acceptStates.contains(state)) {
		statesMap.put(gAcceptState, new ArrayList<>(List.of("")));
	    }
	});
	function.put(gStartState, new HashMap<>());
	function.get(gStartState).put(this.startState,
            new ArrayList<>(List.of("")));
	ArrayList<String> states = new ArrayList<>(this.states);

	for (String leaving : removalSequence) {
	    states.remove(leaving);

	    HashMap<String, ArrayList<String>> leavingMap = function.getOrDefault(leaving, new HashMap<>());
	    ArrayList<String> leavingSelfTransitions = leavingMap.get(leaving);
	    String loopExpr = starRegex(unionRegexes(leavingSelfTransitions));

	    ArrayList<String> senders = new ArrayList<>(states);
	    senders.add(gStartState);
	    for (String sender : senders) {
		HashMap<String, ArrayList<String>> senderMap = function.getOrDefault(sender, new HashMap<>());
		ArrayList<String> senderToLeavingTransitions = senderMap.get(leaving);
		String senderToLeavingExpr =
		    unionRegexes(senderToLeavingTransitions);
		if (senderToLeavingExpr == null) continue;

		ArrayList<String> receivers = new ArrayList<>(states);
		receivers.add(gAcceptState);
		for (String receiver : receivers) {
		    ArrayList<String> leavingToReceiverTransitions = leavingMap.get(receiver);
		    String leavingToReceiverExpr =
			unionRegexes(leavingToReceiverTransitions);
		    if (leavingToReceiverExpr == null) continue;

		    ArrayList<String> units = new ArrayList<>();
		    units.add(senderToLeavingExpr);
		    units.add(loopExpr);
		    units.add(leavingToReceiverExpr);
		    String throughLeaving = concatRegexes(units);

		    ArrayList<String> senderToReceiverTransitions = senderMap.get(receiver);
		    String direct =
			unionRegexes(senderToReceiverTransitions);
		    units.clear();
		    units.add(throughLeaving);
		    units.add(direct);
		    String newRegex = unionRegexes(units);
		    senderMap.remove(receiver);
		    senderToReceiverTransitions = new ArrayList<>();
		    senderMap.put(receiver, senderToReceiverTransitions);
		    senderToReceiverTransitions.add(newRegex);
		}
	    }

	    function.remove(leaving);
	    for (Map.Entry<String, HashMap<String, ArrayList<String>>> stateMapPair :
		     new ArrayList<>(function.entrySet())) {
		String state = stateMapPair.getKey();
		HashMap<String, ArrayList<String>> stateMap = stateMapPair.getValue();
		stateMap.remove(leaving);
		if (stateMap.isEmpty()) function.remove(state);
	    }
	}
	return function.get(gStartState).get(gAcceptState).toArray(new String[]{})[0];
    }

    // Compute the union automaton of a list of NFAs
    public static NFA union(List<NFA> automataList, String newStartState, List<String> suffixes) {
	// Given a list of automata and naming suffixes in case of state naming conflicts
	if (automataList.size() <= 1) {
	    System.err.println("You can not invoke NFA.union with less two automata");
	    System.exit(1);
	}
	if (automataList.size() != suffixes.size()) {
	    System.err.println("Automata to be unioned are not then same size as naming suffixes.");
	    System.exit(1);
	}
	StringBuilder error = new StringBuilder();
	// Detect if a naming suffix is an empty string.
	for (int i = 0;i < suffixes.size();i++) {
	    if (suffixes.get(i).isEmpty()) {
		error.append("Naming suffix no."+i+" must be non-empty string.\n");
	    }
	}
	// Detect if the start state already in some automaton
	for (int i = 0;i < automataList.size();i++) {
	    if (automataList.get(i).states.contains(newStartState)) {
		error.append("Automaton no."+i+" already contains new start state '"+newStartState+"'\n");
	    }
	}
	
	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	}

	NFA union = new NFA();
	// ----- DETERMINISM -----
	union.isDeterministic = false;

	// ----- START STATE -----
	union.startState = newStartState; // Set start state for the union automaton.

	// ----- ALPHABET -----
	// Add all alphabets of all automata
	for (NFA automaton : automataList) {
	    union.alphabet.addAll(automaton.alphabet);
	}
	// Manually add the empty string to the alphabet
	// just in case both all automata are deterministic
	union.alphabet.add("");

	// ----- STATES -----
	union.states.add(newStartState); // add the start state of the union automaton
	union.states.addAll(automataList.get(0).states); // states of first automaton
	// lambdas don't allow capture of something non-final.
	final HashSet<String> duplicates = new HashSet<>();
	for (int i = 1;i < automataList.size();i++) {
	    // add states for other automata with naming suffix, if necessary.
	    for (String state : automataList.get(i).states) {
		String s = "";
		if (union.states.contains(state)) {
		    // If it exists in first automaton, suffix it with the i-th suffix.
		    duplicates.add(state);
		    s = suffixes.get(i);
		}
		union.states.add(state + s);
	    }
	}

	// ----- ACCEPT STATES ----
	union.acceptStates.addAll(automataList.get(0).acceptStates);
	// Process accept states in the same manner.
	for (int i = 1;i < automataList.size();i++) {
	    for (String state : automataList.get(i).acceptStates) {
		String s = "";
		if (duplicates.contains(state)) {
		    s = suffixes.get(i);
		}
		union.acceptStates.add(state + s);
	    }
	}

	// ----- TRANSITION FUNCTION -----
	// First, put all transitions in the first automaton.
	union.transitionFunction.putAll(automataList.get(0).transitionFunction);
	for (int i = 1;i < automataList.size();i++) {
	    final int j = i; // lambdas don't allow capture of something non-final.
	    automataList.get(j).transitionFunction.forEach((state, stateSymbolsMap) -> {
		    HashMap<String, HashSet<String>> stateCorrectedSymbolsMap = new HashMap<>();
		    stateSymbolsMap.forEach((symbol, symbolOutputSet) -> {
			    HashSet<String> symbolCorrectedOutputSet = new HashSet<>();
			    for (String x : symbolOutputSet) {
				String s = "";
				if (duplicates.contains(x)) {
				    s = suffixes.get(j);
				}
				symbolCorrectedOutputSet.add(x + s);
			    }
			    stateCorrectedSymbolsMap.put(symbol, symbolCorrectedOutputSet);
			});
		    String s = "";
		    if (duplicates.contains(state)) {
			s = suffixes.get(j);
		    }
		    union.transitionFunction.put(state + s, stateCorrectedSymbolsMap);
		});
	}

	// ADD TRANSITIONS OF THE NEW START STATE.
	// For each automaton (N), add an empty string transition
	// leading from the union automaton start state to
	// start state of (N).
	ArrayList<String> startStatesList = new ArrayList<>();
	startStatesList.add(automataList.get(0).startState);
	for (int i = 1;i < automataList.size();i++) {
	    String s = "";
	    if (duplicates.contains(automataList.get(i).startState)) {
		s = suffixes.get(i);
	    }
	    startStatesList.add(automataList.get(i).startState + s);
	}
	union.putTransition(union.startState, "", startStatesList);

	return union;
    }

    public static NFA concatenate(List<NFA> automataList, List<String> suffixes) {
	// Given a list of automata and naming suffixes in case of state naming conflicts
	if (automataList.size() <= 1) {
	    System.err.println("You can not invoke NFA.concatenate with less two automata");
	    System.exit(1);
	}
	if (automataList.size() != suffixes.size()) {
	    System.err.println("Automata to be concatenateed are not then same size as naming suffixes.");
	    System.exit(1);
	}
	StringBuilder error = new StringBuilder();
	// Detect if a naming suffix is an empty string.
	for (int i = 0;i < suffixes.size();i++) {
	    if (suffixes.get(i).isEmpty()) {
		error.append("Naming suffix no."+i+" must be non-empty string.\n");
	    }
	}
	
	if (!error.isEmpty()) {
	    System.err.println(error);
	    System.exit(1);
	}
	
	NFA result = new NFA();
	// ----- DETERMINISM -----
	result.isDeterministic = false;

	// ----- START STATE -----
	// Set start state, which is the start state in first automaton
	result.startState = automataList.get(0).startState;

	// ----- ALPHABET -----
	for (NFA automata : automataList) {
	    result.alphabet.addAll(automata.alphabet);
	}
	// Manually add the empty string to the alphabet
	// just in case all automata are deterministic.
	result.alphabet.add("");

	// ----- STATES -----
	// lambdas don't allow capture of something non-final.
	result.states.addAll(automataList.get(0).states); // states of first automaton
	final HashSet<String> duplicates = new HashSet<>();
	for (int i = 1;i < automataList.size();i++) {
	    for (String state : automataList.get(i).states) {
		String s = "";
		if (result.states.contains(state)) {
		    duplicates.add(state);
		    s = suffixes.get(i);
		}
		result.states.add(state + s);
	    }
	}

	// ----- ACCEPT STATES ----
	// The accept states are those of the last automaton
	for (String state : automataList.get(automataList.size()-1).acceptStates) {
	    String s = "";
	    if (duplicates.contains(state)) {
		s = suffixes.get(automataList.size()-1);
	    }
	    result.acceptStates.add(state + s);
	}

	// ----- TRANSITION FUNCTION -----
	// First, put all transitions in the first automaton.
	result.transitionFunction.putAll(automataList.get(0).transitionFunction);
	for (int i = 1;i < automataList.size();i++) {
	    final int j = i; // lambdas don't allow capture of something non-final.
	    automataList.get(j).transitionFunction.forEach((state, stateSymbolsMap) -> {
		    HashMap<String, HashSet<String>> stateCorrectedSymbolsMap = new HashMap<>();
		    stateSymbolsMap.forEach((symbol, symbolOutputSet) -> {
			    HashSet<String> symbolCorrectedOutputSet = new HashSet<>();
			    for (String x : symbolOutputSet) {
				String s = "";
				if (duplicates.contains(x)) {
				    s = suffixes.get(j);
				}
				symbolCorrectedOutputSet.add(x + s);
			    }
			    stateCorrectedSymbolsMap.put(symbol, symbolCorrectedOutputSet);
			});
		    String s = "";
		    if (duplicates.contains(state)) {
			s = suffixes.get(j);
		    }
		    result.transitionFunction.put(state + s, stateCorrectedSymbolsMap);
		});
	}

	// For each automaton in index (i),
	// add an empty string transition leading from the accept states
	// of automaton (i) to the start state of automaton (i+1).
	// Of course, the last is excepted.
	{
	    // Add empty string transitions for
	    // the accept states of first automaton
	    String s = "";
	    if (duplicates.contains(automataList.get(1).startState)) {
		s = suffixes.get(1);
	    }
	    for (String state : automataList.get(0).acceptStates) {
		result.putTransition(state, "", List.of(automataList.get(1).startState + s));
	    }
	}
	for (int i = 1;i < automataList.size()-1;i++) {
	    NFA current = automataList.get(i);
	    NFA next = automataList.get(i+1);
	    String s1 = "";
	    if (duplicates.contains(next.startState)) {
		s1 = suffixes.get(i+1);
	    }
	    for (String state : current.acceptStates) {
		String s2 = "";
		if (duplicates.contains(state)) {
		    s2 = suffixes.get(i);
		}
		result.putTransition(state + s2, "", List.of(next.startState + s1));
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
	// ----- DETERMINISM -----
	result.isDeterministic = false;

	// ----- START STATE -----
	result.startState = newStartState;

	// ----- ALPHABET -----
	result.alphabet.addAll(automaton.alphabet);
	// Manually add the empty string to the alphabet
	// just in case (automaton) is deterministic.
	result.alphabet.add("");

	// ----- STATES -----
	result.states.add(newStartState);
	result.states.addAll(automaton.states);

	// ----- ACCEPT STATES -----
	result.acceptStates.add(newStartState);
	result.acceptStates.addAll(automaton.acceptStates);

	// ----- TRANSITION FUNCTION -----
	result.transitionFunction.putAll(automaton.transitionFunction);
	// For each accept state in (automaton)
	// add an empty string transition leading
	// to the old start state
	for (String state : automaton.acceptStates) {
	    // Add transition in (transitionFunction).
	    result.putTransition(state, "",
                List.of(automaton.startState));
	}

	// Add one empty string transition
	// for the new start state
	// leading to the start state in (automaton)
	result.putTransition(newStartState, "", List.of(automaton.startState));

	return result;
    }
} // class NFA
