package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

import org.processmining.ptrframework.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class SearchGraphPT {
    private final ReplayProcessTreeUtils ptUtil = new ReplayProcessTreeUtils();
    private final SearchGraphPtReplaySemantics ptSem = new SearchGraphPtReplaySemantics();

    public SGASearchResult constructResultDictionary(SGASearchState state, List<String> variant) {
        SGASearchResult result = new SGASearchResult();
        LinkedList<Pair<Object, Object>> alignment = new LinkedList<>();
        SGASearchState currentState = state;
        SGASearchState parentState = state.parent;
        while (parentState != null) {
            if (!currentState.leaves.isEmpty()) {
                if (currentState.index == variant.size()) {
                    List<String> leafLabels = currentState.leaves.stream().map(ReplayProcessTree::getLabel).collect(Collectors.toList());
                    Collections.reverse(leafLabels);

                    LinkedList<ReplayProcessTree> leavesReversed = new LinkedList<>(currentState.leaves);
                    Collections.reverse(leavesReversed);

                    if (leafLabels.contains(variant.get(variant.size() - 1))) {
                        int i = leafLabels.indexOf(variant.get(variant.size() - 1));

                        for (int j = 0; j < leavesReversed.size(); j++) {
                            if (i == j) {
                                alignment.add(new Pair<>(variant.get(variant.size() - 1), leavesReversed.get(j)));
                            } else {
                                alignment.add(new Pair<>(">>", leavesReversed.get(j)));
                            }
                        }
                    } else {
                        if (currentState.costs - currentState.leaves.size() > parentState.costs) {
                            alignment.add(new Pair<>(variant.get(parentState.index), ">>"));
                        }

                        for (ReplayProcessTree leaf : leavesReversed) {
                            alignment.add(new Pair<>(">>", leaf));
                        }
                    }
                } else {
                    alignment.add(new Pair<>(variant.get(parentState.index), currentState.leaves.get(currentState.leaves.size() - 1)));
                    LinkedList<ReplayProcessTree> leavesReversed = new LinkedList<>(currentState.leaves);
                    Collections.reverse(leavesReversed);
                    leavesReversed.removeFirst();
                    for (ReplayProcessTree n : leavesReversed) {
                        alignment.add(new Pair<>(">>", n));
                    }
                }
            } else {
                alignment.add(new Pair<>(variant.get(parentState.index), ">>"));
            }
            parentState = parentState.parent;
            currentState = currentState.parent;
        }
        Collections.reverse(alignment);
        result.setAlignment(alignment);

        return result;
    }

    public boolean isFinalTreeState(ProcessTreeState state, ReplayProcessTree pt) {
        return state.get(new Pair<>(System.identityHashCode(pt), pt)) == ReplayProcessTree.OperatorState.CLOSED;
    }

    public void updateCostsRecursively(float delta, Collection<SGASearchState> states) {
        for (SGASearchState state : states) {
            state.costs = state.costs - delta;
            updateCostsRecursively(delta, state.children);
        }
    }

    public boolean checkIfStateExistsAndUpdate(SGASearchState searchState, Collection<SGASearchState> states) {
        boolean match = false;

        for (SGASearchState state : states) {
            if (state.index == searchState.index && state.state == searchState.state) {
                match = true;

                if (searchState.costs < state.costs) {
                    updateCostsRecursively(state.costs - searchState.costs, state.children);
                    state.costs = searchState.costs;
                    state.parent = searchState.parent;
                    state.leaves = searchState.leaves;
                }
            }
        }

        return match;
    }

    public void addNewState(SGASearchState state, SGASearchState parent, PriorityQueue<SGASearchState> openStates, HashSet<SGASearchState> closedStates) {
        if (!checkIfStateExistsAndUpdate(state, closedStates)) {
            if (!checkIfStateExistsAndUpdate(state, openStates)) {
                parent.children.add(state);
                openStates.add(state);
            }
        }
    }

    public LinkedList<ReplayProcessTree> obtainLeavesFromStatePath(List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path, boolean includeTau) {
        LinkedList<ReplayProcessTree> result = new LinkedList<>();

        for (Pair<ReplayProcessTree, ReplayProcessTree.OperatorState> element : path) {
            ReplayProcessTree pt = element.getKey();
            ReplayProcessTree.OperatorState opState = element.getValue();
            if (pt.getOperator() == null && (includeTau || !pt.isSilent()) && opState == ReplayProcessTree.OperatorState.OPEN) {
                result.add(pt);
            }
        }

        return result;
    }

    public boolean needLogMove(ProcessTreeState oldState, ProcessTreeState newState, List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path) {
        if (!obtainLeavesFromStatePath(path, true).isEmpty()) {
            return true;
        }

        List<ReplayProcessTree> choices = oldState.keySet().stream().map(Pair::getValue).filter(tree -> ptUtil.isAnyOperatorOf(tree, Arrays.asList(Operator.XOR, Operator.LOOP))).collect(Collectors.toList());
        for (ReplayProcessTree choice : choices) {
            if (ptUtil.isOperator(choice, Operator.XOR)) {
                if (ptUtil.isInState(choice, ReplayProcessTree.OperatorState.FUTURE, oldState) || ptUtil.isInState(choice, ReplayProcessTree.OperatorState.CLOSED, oldState) &&
                        oldState.get(new Pair<>(System.identityHashCode(choice), choice)) != newState.get(new Pair<>(System.identityHashCode(choice), choice))) {
                    return true;
                }
            } else if (ptUtil.isOperator(choice, Operator.LOOP)) {
                for (ReplayProcessTree loopChild : choice.getChildren()) {
                    if (ptUtil.isInState(loopChild, ReplayProcessTree.OperatorState.FUTURE, oldState) || ptUtil.isInState(loopChild, ReplayProcessTree.OperatorState.CLOSED, oldState) &&
                            oldState.get(new Pair<>(System.identityHashCode(loopChild), loopChild)) != newState.get(new Pair<>(System.identityHashCode(loopChild), loopChild))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public SGASearchResult alignVariant(List<String> variant, HashSet<Pair<Integer, ReplayProcessTree>> treeLeafSet, ReplayProcessTree pt) {
        SGASearchState initialSearchState = new SGASearchState(0, 0, ptSem.getInitialState(pt));
        HashSet<SGASearchState> closedSet = new HashSet<>();
        PriorityQueue<SGASearchState> openSet = new PriorityQueue<>();
        openSet.add(initialSearchState);
        int count = 0;
        while (!openSet.isEmpty()) {
            count = count + 1;
            SGASearchState sgaState = openSet.poll();
            if (isFinalTreeState(sgaState.state, pt) && sgaState.index == variant.size()) {
                return constructResultDictionary(sgaState, variant);
            } else {
                closedSet.add(sgaState);
                if (sgaState.index < variant.size()) {
                    List<ReplayProcessTree> candidates = treeLeafSet.stream().map(Pair::getValue).filter(tree -> !tree.isSilent() && tree.getLabel().equals(variant.get(sgaState.index))).collect(Collectors.toList());
                    boolean needLogMove = candidates.isEmpty();

                    for (ReplayProcessTree leaf : candidates) {
                        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = ptSem.shortestPathToEnable(leaf, (ProcessTreeState) sgaState.state.clone());
                        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
                        ProcessTreeState newState = pair.getValue();
                        needLogMove = path == null || path.isEmpty() || needLogMove;

                        if (path != null && !path.isEmpty()) {
                            LinkedList<ReplayProcessTree> modelMoves = obtainLeavesFromStatePath(path, false);
                            needLogMove = needLogMove || needLogMove(sgaState.state, newState, path);
                            Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair2 = ptSem.shortestPathToClose(leaf, newState);
                            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> syncPath = pair2.getKey();
                            newState = pair2.getValue();
                            path.addAll(syncPath);
                            LinkedList<ReplayProcessTree> leaves = obtainLeavesFromStatePath(path, true);
                            SGASearchState newSGAState = new SGASearchState(sgaState.costs + modelMoves.size(), sgaState.index + 1, newState, leaves, sgaState);
                            addNewState(newSGAState, sgaState, openSet, closedSet);
                        }
                    }
                    if (needLogMove) {
                        addNewState(new SGASearchState(sgaState.costs + 1, sgaState.index + 1, (ProcessTreeState) sgaState.state.clone(), sgaState), sgaState, openSet, closedSet);
                    }
                } else {
                    // FINISH
                    Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = ptSem.shortestPathToClose(pt, sgaState.state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
                    ProcessTreeState newState = pair.getValue();
                    LinkedList<ReplayProcessTree> modelMoves = obtainLeavesFromStatePath(path, false);
                    sgaState.state = newState;
                    sgaState.costs = sgaState.costs + modelMoves.size();
                    sgaState.leaves.addAll(obtainLeavesFromStatePath(path, true));
                    openSet.add(sgaState);
                }
            }
        }

        return null;
    }

    public SGASearchResult applyVariant(List<String> variant, ReplayProcessTree pt) {
        HashSet<Pair<Integer, ReplayProcessTree>> treeLeafSet = ptUtil.getLeavesAsTuples(pt, null);
        return alignVariant(variant, treeLeafSet, pt);
    }

    public HashMap<List<String>, SGASearchResult> applyToVariantStrings(Collection<List<String>> variants, ReplayProcessTree pt) {
        HashMap<List<String>, SGASearchResult> result = new HashMap<>();

        for (List<String> variantInput : variants) {
            result.put(variantInput, applyVariant(variantInput, pt));
        }

        return result;
    }

    public static class SGASearchState implements Comparable<SGASearchState> {
        private final int index;
        private final HashSet<SGASearchState> children;
        private float costs;
        private ProcessTreeState state;
        private LinkedList<ReplayProcessTree> leaves;
        private SGASearchState parent;

        public SGASearchState(float costs, int index, ProcessTreeState state) {
            this(costs, index, state, null, null, null);
        }

        public SGASearchState(float costs, int index, ProcessTreeState state, SGASearchState parent) {
            this(costs, index, state, null, parent, null);
        }

        public SGASearchState(float costs, int index, ProcessTreeState state, LinkedList<ReplayProcessTree> leaves, SGASearchState parent) {
            this(costs, index, state, leaves, parent, null);
        }

        public SGASearchState(float costs, int index, ProcessTreeState state, LinkedList<ReplayProcessTree> leaves, SGASearchState parent, HashSet<SGASearchState> children) {
            this.costs = costs;
            this.index = index;
            this.state = state;
            this.leaves = leaves != null ? leaves : new LinkedList<>();
            this.parent = parent;
            this.children = children != null ? children : new HashSet<>();
        }

        public String toString() {
            return "(" + costs + "," + index + "," + state + ")";
        }


        @Override
        public int compareTo(SGASearchState o) {
            return Float.compare(costs, o.costs);
        }
    }
}
