package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

import org.processmining.ptrframework.utils.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SearchGraphPtReplaySemantics {
    private final ReplayProcessTreeUtils ptu = new ReplayProcessTreeUtils();

    public ProcessTreeState getInitialState(ReplayProcessTree tree) {
        ProcessTreeState state = new ProcessTreeState();
        state.put(new Pair<>(System.identityHashCode(tree), tree), ReplayProcessTree.OperatorState.FUTURE);
        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = enableVertex(tree, state);
        state = pair.getValue();

        return state;
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> transformTree(ReplayProcessTree tree, ReplayProcessTree.OperatorState stateType, ProcessTreeState state) {
        state = (ProcessTreeState) state.clone();
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = new LinkedList<>();

        if (!state.containsKey(new Pair<>(System.identityHashCode(tree), tree)) || state.get(new Pair<>(System.identityHashCode(tree), tree)) != stateType) {
            state.put(new Pair<>(System.identityHashCode(tree), tree), stateType);
            path.add(new Pair<>(tree, stateType));
        }

        for (ReplayProcessTree child : tree.getChildren()) {
            Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(child, stateType, state);
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
            state = pair.getValue();
            path.addAll(extendingPath);
        }

        return new Pair<>(path, state);
    }

    public boolean canEnable(ReplayProcessTree tree, ProcessTreeState state) {
        if (ptu.isInState(tree, ReplayProcessTree.OperatorState.FUTURE, state)) {
            if (ptu.isRoot(tree)) {
                return true;
            }
            if (ptu.isInState(tree.getParent(), ReplayProcessTree.OperatorState.OPEN, state)) {
                if (ptu.isAnyOperatorOf(tree.getParent(), Arrays.asList(Operator.PARALLEL, Operator.OR))) {
                    return true;
                } else if (ptu.isOperator(tree.getParent(), Operator.XOR)) {
                    for (ReplayProcessTree child : tree.getParent().getChildren()) {
                        if (state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.FUTURE) {
                            return false;
                        }
                    }
                    return true;
                } else if (ptu.isOperator(tree.getParent(), Operator.SEQUENCE)) {
                    int indexOfTree = tree.getParent().getChildren().indexOf(tree);
                    if (indexOfTree == 0) {
                        return true;
                    } else {
                        return ptu.isInState(tree.getParent().getChildren().get(indexOfTree - 1), ReplayProcessTree.OperatorState.CLOSED, state);
                    }
                } else if (ptu.isOperator(tree.getParent(), Operator.LOOP)) {
                    for (ReplayProcessTree child : tree.getParent().getChildren()) {
                        if (state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.FUTURE && state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.CLOSED) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canOpen(ReplayProcessTree tree, ProcessTreeState state) {
        return ptu.isInState(tree, ReplayProcessTree.OperatorState.ENABLED, state);
    }

    public boolean canClose(ReplayProcessTree tree, ProcessTreeState state) {
        if (ptu.isLeaf(tree)) {
            return ptu.isInState(tree, ReplayProcessTree.OperatorState.OPEN, state);
        } else if (ptu.isAnyOperatorOf(tree, Arrays.asList(Operator.SEQUENCE, Operator.PARALLEL, Operator.XOR))) {
            for (ReplayProcessTree child : tree.getChildren()) {
                if (state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.CLOSED) {
                    return false;
                }
            }
            return true;
        } else if (ptu.isOperator(tree, Operator.OR)) {
            for (ReplayProcessTree child : tree.getChildren()) {
                if (state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.CLOSED && state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.FUTURE) {
                    return false;
                }
            }
            return true;
        } else if (ptu.isOperator(tree, Operator.LOOP)) {
            return ptu.isInState(tree.getChildren().get(0), ReplayProcessTree.OperatorState.CLOSED, state) && ptu.isInState(tree.getChildren().get(1), ReplayProcessTree.OperatorState.FUTURE, state);
        }

        // Can not occur
        return false;
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> closeVertex
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (canClose(tree, state)) {
            ReplayProcessTree.OperatorState currentState = state.get(new Pair<>(System.identityHashCode(tree), tree));
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = new LinkedList<>();
            state = (ProcessTreeState) state.clone();
            for (ReplayProcessTree child : tree.getChildren()) {
                if (!ptu.isInState(child, ReplayProcessTree.OperatorState.CLOSED, state)) {
                    Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(child, ReplayProcessTree.OperatorState.CLOSED, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                }
            }
            state.put(new Pair<>(System.identityHashCode(tree), tree), ReplayProcessTree.OperatorState.CLOSED);
            path.add(new Pair<>(tree, ReplayProcessTree.OperatorState.CLOSED));
            if (ptu.isOperator(tree.getParent(), Operator.LOOP) && tree.getParent().getChildren().indexOf(tree) == 1 && currentState == ReplayProcessTree.OperatorState.OPEN) {
                Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = enableVertex(tree.getParent().getChildren().get(0), state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                path.addAll(extendingPath);
                state = pair.getValue();
            }

            return new Pair<>(path, state);
        } else {
            return new Pair<>(null, null);
        }
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> enableVertex
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (state.get(new Pair<>(System.identityHashCode(tree), tree)) == ReplayProcessTree.OperatorState.ENABLED) {
            return new Pair<>(new LinkedList<>(), state);
        }
        if (canEnable(tree, state)) {
            state = (ProcessTreeState) state.clone();
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = new LinkedList<>();
            state.put(new Pair<>(System.identityHashCode(tree), tree), ReplayProcessTree.OperatorState.ENABLED);
            path.add(new Pair<>(tree, ReplayProcessTree.OperatorState.ENABLED));

            if (ptu.isOperator(tree.getParent(), Operator.LOOP)) {
                if (tree.getParent().getChildren().indexOf(tree) == 0) {
                    Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(tree.getParent().getChildren().get(1), ReplayProcessTree.OperatorState.FUTURE, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    path.addAll(extendingPath);
                    state = pair.getValue();
                }
                if (tree.getParent().getChildren().indexOf(tree) == 1) {
                    Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(tree.getParent().getChildren().get(0), ReplayProcessTree.OperatorState.FUTURE, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    path.addAll(extendingPath);
                    state = pair.getValue();
                }
            }

            if (ptu.isOperator(tree.getParent(), Operator.XOR)) {
                for (ReplayProcessTree child : tree.getParent().getChildren()) {
                    if (child != tree) {
                        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(child, ReplayProcessTree.OperatorState.CLOSED, state);
                        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                        path.addAll(extendingPath);
                        state = pair.getValue();
                    }
                }
            }

            for (ReplayProcessTree child : tree.getChildren()) {
                Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = transformTree(child, ReplayProcessTree.OperatorState.FUTURE, state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                path.addAll(extendingPath);
                state = pair.getValue();
            }

            return new Pair<>(path, state);
        } else {
            return new Pair<>(null, null);
        }
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> openVertex
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (canOpen(tree, state)) {
            state = (ProcessTreeState) state.clone();
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = new LinkedList<>();
            state.put(new Pair<>(System.identityHashCode(tree), tree), ReplayProcessTree.OperatorState.OPEN);
            path.add(new Pair<>(tree, ReplayProcessTree.OperatorState.OPEN));

            if (ptu.isAnyOperatorOf(tree, Arrays.asList(Operator.XOR, Operator.OR, Operator.PARALLEL))) {
                for (ReplayProcessTree child : tree.getChildren()) {
                    state.put(new Pair<>(System.identityHashCode(child), child), ReplayProcessTree.OperatorState.FUTURE);
                    path.add(new Pair<>(child, ReplayProcessTree.OperatorState.FUTURE));
                }
            } else if (ptu.isAnyOperatorOf(tree, Arrays.asList(Operator.SEQUENCE, Operator.LOOP))) {
                state.put(new Pair<>(System.identityHashCode(tree.getChildren().get(0)), tree.getChildren().get(0)), ReplayProcessTree.OperatorState.ENABLED);
                path.add(new Pair<>(tree.getChildren().get(0), ReplayProcessTree.OperatorState.ENABLED));
                for (int i = 1; i < tree.getChildren().size(); i++) {
                    state.put(new Pair<>(System.identityHashCode(tree.getChildren().get(i)), tree.getChildren().get(i)), ReplayProcessTree.OperatorState.FUTURE);
                    path.add(new Pair<>(tree.getChildren().get(i), ReplayProcessTree.OperatorState.FUTURE));
                }
            }

            return new Pair<>(path, state);
        } else {
            return new Pair<>(null, null);
        }
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> shortestPathToOpen
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (ptu.isInState(tree, ReplayProcessTree.OperatorState.OPEN, state)) {
            return new Pair<>(new LinkedList<>(), state);
        }

        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = openVertex(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> fastPath = pair.getKey();
        ProcessTreeState fastState = pair.getValue();
        if (fastState != null) {
            return new Pair<>(fastPath, fastState);
        }

        pair = shortestPathToEnable(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
        state = pair.getValue();
        if (path != null) {
            pair = openVertex(tree, state);
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
            path.addAll(extendingPath);
            state = pair.getValue();
        }

        return new Pair<>(path, state);
    }

    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> shortestPathToClose
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (ptu.isInState(tree, ReplayProcessTree.OperatorState.CLOSED, state)) {
            return new Pair<>(new LinkedList<>(), state);
        }

        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = closeVertex(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> fastPath = pair.getKey();
        ProcessTreeState fastState = pair.getValue();
        if (fastState != null) {
            return new Pair<>(fastPath, fastState);
        }

        pair = shortestPathToOpen(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
        state = pair.getValue();
        if (ptu.isLeaf(tree)) {
            pair = closeVertex(tree, state);
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
            state = pair.getValue();
            path.addAll(extendingPath);

            return new Pair<>(path, state);
        } else if (ptu.isAnyOperatorOf(tree, Arrays.asList(Operator.SEQUENCE, Operator.PARALLEL))) {
            for (ReplayProcessTree child : tree.getChildren()) {
                pair = shortestPathToClose(child, state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);
            }
        } else if (ptu.isOperator(tree, Operator.LOOP)) {
            if (state.get(new Pair<>(System.identityHashCode(tree.getChildren().get(0)), tree.getChildren().get(0))) == ReplayProcessTree.OperatorState.ENABLED || state.get(new Pair<>(System.identityHashCode(tree.getChildren().get(0)), tree.getChildren().get(0))) == ReplayProcessTree.OperatorState.OPEN) {
                pair = shortestPathToClose(tree.getChildren().get(0), state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);
            } else if (state.get(new Pair<>(System.identityHashCode(tree.getChildren().get(1)), tree.getChildren().get(1))) == ReplayProcessTree.OperatorState.ENABLED || state.get(new Pair<>(System.identityHashCode(tree.getChildren().get(1)), tree.getChildren().get(1))) == ReplayProcessTree.OperatorState.OPEN) {
                pair = shortestPathToClose(tree.getChildren().get(1), state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);

                pair = shortestPathToOpen(tree.getChildren().get(0), state);
                extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);

                pair = shortestPathToClose(tree.getChildren().get(0), state);
                extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);
            }
        } else if (ptu.isAnyOperatorOf(tree, Arrays.asList(Operator.XOR, Operator.OR))) {
            boolean busy = false;
            for (ReplayProcessTree child : tree.getChildren()) {
                if (state.get(new Pair<>(System.identityHashCode(child), child)) == ReplayProcessTree.OperatorState.ENABLED || state.get(new Pair<>(System.identityHashCode(child), child)) == ReplayProcessTree.OperatorState.OPEN) {
                    pair = shortestPathToClose(child, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                    busy = true;
                }
            }

            if (!busy) {
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> currPath = new LinkedList<>();
                ProcessTreeState currState = (ProcessTreeState) state.clone();
                long currPathCosts = Long.MAX_VALUE;

                for (ReplayProcessTree child : tree.getChildren()) {
                    if (state.get(new Pair<>(System.identityHashCode(child), child)) != ReplayProcessTree.OperatorState.CLOSED) {
                        pair = shortestPathToClose(child, state);
                        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> candidateP = pair.getKey();
                        ProcessTreeState candidateS = pair.getValue();
                        long candidateCosts = candidateP.stream().filter(t -> t.getKey().getOperator() == null && !t.getKey().isSilent() && t.getValue() == ReplayProcessTree.OperatorState.OPEN).count();
                        if (candidateCosts < currPathCosts) {
                            currPath = candidateP;
                            currState = candidateS;
                            currPathCosts = candidateCosts;
                        }
                    }
                }

                path.addAll(currPath);
                state = currState;
            }
        }

        pair = closeVertex(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
        state = pair.getValue();
        path.addAll(extendingPath);

        return new Pair<>(path, state);
    }


    public Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> shortestPathToEnable
            (ReplayProcessTree tree, ProcessTreeState state) {
        if (state.get(new Pair<>(System.identityHashCode(tree), tree)) == ReplayProcessTree.OperatorState.ENABLED) {
            return new Pair<>(new LinkedList<>(), state);
        }

        state = (ProcessTreeState) state.clone();

        Pair<List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>>, ProcessTreeState> pair = enableVertex(tree, state);
        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> fastPath = pair.getKey();
        ProcessTreeState fastState = pair.getValue();
        if (fastState != null) {
            return new Pair<>(fastPath, fastState);
        }


        if (state.get(new Pair<>(System.identityHashCode(tree), tree)) == ReplayProcessTree.OperatorState.FUTURE) {
            pair = shortestPathToOpen(tree.getParent(), state);
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
            state = pair.getValue();

            if (ptu.isAnyOperatorOf(tree.getParent(), Arrays.asList(Operator.XOR, Operator.PARALLEL, Operator.OR))) {
                pair = enableVertex(tree, state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                state = pair.getValue();
                if (state != null) { // choice if another choice has already been taken!
                    path.addAll(extendingPath);
                }
            }

            if (tree.getParent().getOperator() == Operator.SEQUENCE) {
                for (int i = 0; i < tree.getParent().getChildren().size(); i++) {
                    ReplayProcessTree child = tree.getParent().getChildren().get(i);
                    if (child == tree) {
                        if (i > 0) {
                            pair = enableVertex(tree, state);
                            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                            state = pair.getValue();
                            path.addAll(extendingPath);
                        }
                        break;
                    } else {
                        pair = shortestPathToClose(child, state);
                        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                        state = pair.getValue();
                        path.addAll(extendingPath);
                    }
                }
            } else if (tree.getParent().getOperator() == Operator.LOOP) {
                if (tree.getParent().getChildren().indexOf(tree) == 0) {
                    if (state.get(new Pair<>(System.identityHashCode(tree.getParent().getChildren().get(1)), tree.getParent().getChildren().get(1))) != ReplayProcessTree.OperatorState.FUTURE &&
                            state.get(new Pair<>(System.identityHashCode(tree.getParent().getChildren().get(1)), tree.getParent().getChildren().get(1))) != ReplayProcessTree.OperatorState.CLOSED) {
                        pair = shortestPathToClose(tree.getParent().getChildren().get(1), state);
                        List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                        state = pair.getValue();
                        path.addAll(extendingPath);
                    }

                    pair = enableVertex(tree, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    ProcessTreeState optionalNewState = pair.getValue();
                    path.addAll(extendingPath);
                    state = optionalNewState;
                } else {
                    pair = shortestPathToClose(tree.getParent().getChildren().get(0), state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);

                    pair = enableVertex(tree, state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath2 = pair.getKey();
                    ProcessTreeState optionalNewState = pair.getValue();
                    path.addAll(extendingPath2);
                    state = optionalNewState;
                }
            }

            return new Pair<>(path, state);
        } else {
            pair = shortestPathToClose(tree, state);
            List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> path = pair.getKey();
            state = pair.getValue();

            ReplayProcessTree parent = tree.getParent();
            while (parent != null) {
                if (parent.getOperator() == Operator.LOOP && state.get(new Pair<>(System.identityHashCode(parent), parent)) == ReplayProcessTree.OperatorState.OPEN) {
                    break;
                }
                parent = parent.getParent();
            }

            if (parent != null && parent.getOperator() == Operator.LOOP) {
                if (state.get(new Pair<>(System.identityHashCode(parent.getChildren().get(0)), parent.getChildren().get(0))) == ReplayProcessTree.OperatorState.OPEN) {
                    pair = shortestPathToClose(parent.getChildren().get(0), state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);

                    pair = shortestPathToEnable(parent.getChildren().get(1), state);
                    extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                } else if (state.get(new Pair<>(System.identityHashCode(parent.getChildren().get(1)), parent.getChildren().get(1))) == ReplayProcessTree.OperatorState.OPEN) {
                    pair = shortestPathToClose(parent.getChildren().get(1), state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);

                    pair = shortestPathToEnable(parent.getChildren().get(0), state);
                    extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                } else if (state.get(new Pair<>(System.identityHashCode(parent.getChildren().get(0)), parent.getChildren().get(0))) == ReplayProcessTree.OperatorState.FUTURE) {
                    pair = shortestPathToEnable(parent.getChildren().get(0), state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                } else if (state.get(new Pair<>(System.identityHashCode(parent.getChildren().get(1)), parent.getChildren().get(1))) == ReplayProcessTree.OperatorState.FUTURE) {
                    pair = shortestPathToEnable(parent.getChildren().get(1), state);
                    List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                    state = pair.getValue();
                    path.addAll(extendingPath);
                }

                pair = shortestPathToEnable(tree, state);
                List<Pair<ReplayProcessTree, ReplayProcessTree.OperatorState>> extendingPath = pair.getKey();
                state = pair.getValue();
                path.addAll(extendingPath);

                return new Pair<>(path, state);
            } else {
                return new Pair<>(null, null);
            }
        }
    }
}
