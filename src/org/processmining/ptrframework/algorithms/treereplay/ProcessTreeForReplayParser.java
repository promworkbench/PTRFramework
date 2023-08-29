package org.processmining.ptrframework.algorithms.treereplay;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;
import org.processmining.ptrframework.algorithms.treereplay.NodeType.*;
import org.processmining.ptrframework.utils.Pair;

import java.util.HashMap;
import java.util.LinkedList;

public class ProcessTreeForReplayParser {

    public Pair<ProcessTreeNode, HashMap<Node, ProcessTreeNode>> translate(ProcessTree tree) {
        XFactory xFactory = new XFactoryNaiveImpl();
        LinkedList<Node> remainingNodes = new LinkedList<>();

        Node root = tree.getRoot();
        ProcessTreeNode resultRoot = translateToProcessTreeNode(null, xFactory, root);
        HashMap<Node, ProcessTreeNode> toParents = new HashMap<>();
        HashMap<Node, ProcessTreeNode> nodeToProcessTreeNode = new HashMap<>();

        if (root instanceof Block) {
            ((Block) root).getChildren().forEach(node -> {
                remainingNodes.add(node);
                toParents.put(node, resultRoot);
            });
        }
        nodeToProcessTreeNode.put(root, resultRoot);

        while (!remainingNodes.isEmpty()) {
            Node currNode = remainingNodes.removeFirst();

            ProcessTreeNode currProcessTreeNode = translateToProcessTreeNode(toParents.get(currNode), xFactory, currNode);
            if (currNode instanceof Block) {
                ((Block) currNode).getChildren().forEach(node -> {
                    remainingNodes.add(node);
                    toParents.put(node, currProcessTreeNode);
                });
            }
            nodeToProcessTreeNode.put(currNode, currProcessTreeNode);

            toParents.get(currNode).addChild(currProcessTreeNode);
        }

        return new Pair<>(resultRoot, nodeToProcessTreeNode);
    }

    public ProcessTreeNode translateToProcessTreeNode(ProcessTreeNode father, XFactory xFactory, Node node) {
        if (node instanceof Block.Seq) {
            return new ProcessTreeSequentialNode(father, xFactory, node);
        } else if (node instanceof Block.And) {
            return new ProcessTreeAndNode(father, xFactory, node);
        } else if (node instanceof Block.Xor) {
            return new ProcessTreeXOrNode(father, xFactory, node);
        } else if (node instanceof Block.XorLoop) {
            return new ProcessTreeLoopNode(father, xFactory, node);
        } else if (node instanceof Task.Manual) {
            return new ProcessTreeTask(father, xFactory, node);
        } else if (node instanceof Task.Automatic) {
            return new ProcessTreeSilentTask(father, xFactory, node);
        }

        return null;
    }
}
