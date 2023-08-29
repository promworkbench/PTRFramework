package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;


import org.processmining.ptrframework.utils.Pair;

import java.util.Collection;
import java.util.HashSet;

public class ReplayProcessTreeUtils {

    public boolean isLeaf(ReplayProcessTree tree) {
        return (tree.getChildren() == null || tree.getChildren().isEmpty()) && tree.getOperator() == null;
    }

    public boolean isOperator(ReplayProcessTree tree, Operator operator) {
        return tree != null && tree.getOperator() != null && tree.getOperator() == operator;
    }

    public boolean isAnyOperatorOf(ReplayProcessTree tree, Collection<Operator> operators) {
        return tree != null && tree.getOperator() != null && operators.contains(tree.getOperator());
    }

    public boolean isInState(ReplayProcessTree tree, ReplayProcessTree.OperatorState targetState, ProcessTreeState treeState) {
        return tree != null && treeState.containsKey(new Pair<>(System.identityHashCode(tree), tree)) && treeState.get(new Pair<>(System.identityHashCode(tree), tree)).equals(targetState);
    }

    public boolean isRoot(ReplayProcessTree tree) {
        return tree.getParent() == null;
    }

    public HashSet<Pair<Integer, ReplayProcessTree>> getLeavesAsTuples(ReplayProcessTree t, HashSet<Pair<Integer, ReplayProcessTree>> leaves) {
        leaves = leaves == null ? new HashSet<>() : leaves;

        if (t.getChildren().isEmpty()) {
            leaves.add(new Pair<>(System.identityHashCode(t), t));
        } else {
            for (ReplayProcessTree c : t.getChildren()) {
                getLeavesAsTuples(c, leaves);
            }
        }

        return leaves;
    }
}
