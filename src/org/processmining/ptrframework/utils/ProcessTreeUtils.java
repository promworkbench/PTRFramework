package org.processmining.ptrframework.utils;

import org.processmining.processtree.*;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.ArrayList;
import java.util.List;

public class ProcessTreeUtils {

    public static boolean isAllFlowerTree(ProcessTree tree) {
        boolean rerun = true;
        ProcessTree copyTree = tree;

        if (tree.getRoot() instanceof Block) {
            while (rerun) {
                rerun = false;

                // Bring into normal form
                copyTree = new ProcessTreeImpl(copyTree);
                if (copyTree.getRoot() instanceof Block) {
                    Block copyRoot = (Block) copyTree.getRoot();
                    for (Node child : (copyRoot).getChildren()) {
                        if (canBeRolledUp(copyRoot, child)) {
                            rollUpBlock(copyTree, copyRoot, child);
                            rerun = true;
                        }
                    }
                }
            }
        }

        Node currNode = copyTree.getRoot();

        if (currNode instanceof Block.Xor && ((Block.Xor) currNode).numChildren() == 2 && ((Block.Xor) currNode).getChildren().get(0) instanceof Task.Automatic) {
            currNode = ((Block.Xor) currNode).getChildren().get(1);
        }

        if (currNode instanceof Block.And) {

            List<Node> children = ((Block.And) currNode).getChildren();

            for (Node child : children) {
                if (!(child instanceof Block.Xor)) {
                    return false;
                }

                List<Node> childChildren = ((Block.Xor) child).getChildren();
                if (childChildren.size() != 2 || !(childChildren.get(0) instanceof Task.Automatic) || !(childChildren.get(1) instanceof Block.XorLoop)) {
                    return false;
                }

                List<Node> loopingChildren = ((Block.XorLoop) childChildren.get(1)).getChildren();
                if (!(loopingChildren.get(0) instanceof Task.Manual && loopingChildren.get(1) instanceof Task.Automatic && loopingChildren.get(2) instanceof Task.Automatic)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public static void cutSubProcessTreeByNode(ProcessTree tree, Node node) {
        ArrayList<Node> unvisitedNodes = new ArrayList<>();
        unvisitedNodes.add(node);

        while (!unvisitedNodes.isEmpty()) {
            ArrayList<Node> newUnvisitedNodes = new ArrayList<>();

            for (Node unvisitedNode : unvisitedNodes) {
                for (Edge incomingEdge : unvisitedNode.getIncomingEdges()) {
                    tree.removeEdge(incomingEdge);
                }

                if (unvisitedNode instanceof Block) {
                    newUnvisitedNodes.addAll(((Block) unvisitedNode).getChildren());
                }

                tree.removeNode(unvisitedNode);
            }

            unvisitedNodes = newUnvisitedNodes;
        }
    }

    public static ProcessTree replaceSubProcessTreeBySubProcessTree(ProcessTree tree, Node toBeReplaced, ProcessTree replacingTree) {
        if (tree.getRoot() == toBeReplaced) {
            return replacingTree;
        }

        for (Node node : replacingTree.getNodes()) {
            tree.addNode(node);
            node.setProcessTree(tree);
        }

        for (Edge edge : replacingTree.getEdges()) {
            tree.addEdge(edge);
        }

        ArrayList<Edge> edgesToBeRemoved = new ArrayList<>();
        for (Edge incomingEdge : toBeReplaced.getIncomingEdges()) {
            incomingEdge.setTarget(replacingTree.getRoot());
            edgesToBeRemoved.add(incomingEdge);
        }

        for (Edge edge : edgesToBeRemoved) {
            toBeReplaced.removeIncomingEdge(edge);
        }

        cutSubProcessTreeByNode(tree, toBeReplaced);

        Node currNode = replacingTree.getRoot();
        Block parent = getParent(currNode);
        while (canBeRolledUp(parent, currNode)) {
            rollUpBlock(tree, parent, currNode);

            currNode = parent;

            parent = getParent(currNode);
        }

        return tree;
    }

    public static Block getParent(Node node) {
        if (node.getIncomingEdges().isEmpty()) {
            return null;
        }

        return node.getParents().iterator().next();
    }

    public static boolean canBeRolledUp(Block parent, Node nodeToBeChecked) {
        if (parent == null) {
            return false;
        }

        if (!(parent instanceof Block.XorLoop) && parent.numChildren() == 1) {
            return true;
        }

        if (nodeToBeChecked instanceof AbstractBlock.XorLoop) {
            return false;
        }

        return parent.getClass() == nodeToBeChecked.getClass();
    }

    public static void rollUpBlock(ProcessTree tree, Block parent, Node nodeForRollUp) {
        int pos = 0;
        for (; pos < parent.numChildren(); pos++) {
            if (parent.getChildren().get(pos).equals(nodeForRollUp)) {
                break;
            }
        }

        if (nodeForRollUp instanceof Block) {
            Block blockForRollUp = (Block) nodeForRollUp;
            for (int i = 0; i < blockForRollUp.numChildren(); i++) {
                Edge newEdge = parent.addChildAt(blockForRollUp.getChildren().get(i), pos + i);
                tree.addEdge(newEdge);
                blockForRollUp.getChildren().get(i).addIncomingEdge(newEdge);
            }
        }

        removeNode(tree, nodeForRollUp);
    }

    public static void removeNode(ProcessTree tree, Node node) {
        node.getIncomingEdges().forEach(edge -> {
            tree.removeEdge(edge);
            edge.getSource().removeOutgoingEdge(edge);
        });

        if (node instanceof Block) {
            ((Block) node).getOutgoingEdges().forEach(edge -> {
                tree.removeEdge(edge);
                edge.getTarget().removeIncomingEdge(edge);
            });
        }

        tree.removeNode(node);
    }

}
