package org.processmining.ptrframework.utils;

import org.processmining.processtree.*;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.ProcessTreeImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Modifier class for the PtR framework to separate candidates.
 */
public class ProcessTreeModifier {
    private static final String identifierName = "Candidate And";
    private HashMap<Block, HashSet<Node>> parallelCandidatesOfFather;

    /**
     * Gets the identifier label for the parallel father.
     *
     * @return Identifier label of a parallel father.
     */
    public static String getIdentifierName() {
        return identifierName;
    }

    /**
     * Applies the tree modification to the input tree returning a copy.
     *
     * @param tree The tree to be modified.
     * @return A tree where candidates are labeled and split.
     */
    public ProcessTree apply(ProcessTree tree) {
        init();
        ProcessTree copy = new ProcessTreeImpl(tree);
        startIdentifyingCandidates(copy);
        combineCandidates(copy);
        return copy;
    }

    /**
     * (Re-)Sets the candidates being parallel for each father.
     */
    public void init() {
        parallelCandidatesOfFather = new HashMap<>();
    }

    /**
     * Combines all candidates identified before.
     *
     * @param tree The tree on which candidates where identified before.
     */
    public void combineCandidates(ProcessTree tree) {
        parallelCandidatesOfFather.forEach((key, value) -> combineCandidatesOnOneLevel(tree, key, value));

    }

    /**
     * Sets a subset of children in parallel separating it from the remaining structure.
     *
     * @param tree     The tree to be modified.
     * @param father   The old father.
     * @param children The children to be set in parallel with a new father.
     */
    public void combineCandidatesOnOneLevel(ProcessTree tree, Block father, HashSet<Node> children) {
        // New father is inserted.
        AbstractBlock.And connectingFather = new AbstractBlock.And(identifierName);
        tree.addNode(connectingFather);
        connectingFather.setProcessTree(tree);

        // New father is connected to the old father.
        Edge edge = father.addChild(connectingFather);
        connectingFather.addIncomingEdge(edge);
        tree.addEdge(edge);

        // All children are set to be children of the new father.
        for (Node child : children) {
            for (Edge incomingEdge : child.getIncomingEdges()) {
                incomingEdge.setSource(connectingFather);
                connectingFather.addOutgoingEdge(incomingEdge);
                father.removeOutgoingEdge(incomingEdge);
            }
        }
    }

    /**
     * Starting function for candidate identification.
     *
     * @param tree The tree to be checked.
     */
    public void startIdentifyingCandidates(ProcessTree tree) {
        Node root = tree.getRoot();

        if (identifyFlower(root) || identifyLoop(root)) {
            labelCandidate(root);
        } else {
            identifyCandidates(root);
        }
    }

    /**
     * Labels a node with the candidate label.
     *
     * @param node Node to be labeled.
     */
    private void labelCandidate(Node node) {
        node.setName("Candidate");
    }

    /**
     * Identifies all candidates according to the candidate check that occur with a shared parallel node as father.
     *
     * @param node Node to recurse on.
     */
    public void identifyCandidates(Node node) {
        if (!(node instanceof Block)) {
            return;
        }

        // Checks if the current block is the parallel father that has at least two children, which are candidates.
        HashSet<Node> candidates = new HashSet<>();
        if (node instanceof Block.And) {
            ((Block) node).getChildren().stream().filter(this::candidateCheck).forEach(child -> {
                labelCandidate(child);
                candidates.add(child);
            });

            if (candidates.size() > 1) {
                if (candidates.size() != ((Block.And) node).getChildren().size()) {
                    parallelCandidatesOfFather.put((Block) node, candidates);
                } else {
                    node.setName(identifierName);
                }
            }
        }

        // Recursion through the tree
        for (Node child : ((Block) node).getChildren()) {
            if (!candidates.contains(child)) {
                identifyCandidates(child);
            }
        }
    }

    /**
     * Checks candidates whether they have a candidate property, i.e., if they are a flower or loop structure.
     *
     * @param node Node to be checked.
     * @return If the node is a candidate.
     */
    public boolean candidateCheck(Node node) {
        return identifyFlower(node) || identifyLoop(node);
    }

    /**
     * Checks if a node is a flower structure. Only recognizes structures such as x(τ, ⟲(a,τ)) for an activity.
     *
     * @param node Node to be checked, if it is a flower structure.
     * @return Whether the node is a flower structure.
     */
    public boolean identifyFlower(Node node) {
        if (!(node instanceof Block.Xor)) {
            return false;
        }

        List<Node> children = ((Block.Xor) node).getChildren();
        if (children.size() != 2 || !(children.get(0) instanceof Task.Automatic)) {
            return false;
        }

        return identifyLoop(children.get(1));
    }

    /**
     * Checks if a node is a loop structure. Only recognizes structures such as ⟲(a,τ) for an activity.
     *
     * @param node Node to be checked, if it is a loop structure.
     * @return Whether the node is a loop structure.
     */
    public boolean identifyLoop(Node node) {
        if (!(node instanceof Block.XorLoop)) {
            return false;
        }

        List<Node> children = ((Block.XorLoop) node).getChildren();
        return children.size() == 3 && children.get(0) instanceof Task.Manual && children.get(1) instanceof Task.Automatic && children.get(2) instanceof Task.Automatic;
    }
}
