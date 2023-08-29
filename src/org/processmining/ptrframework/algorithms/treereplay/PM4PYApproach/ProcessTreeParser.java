package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task;

import java.util.HashMap;
import java.util.LinkedList;

public class ProcessTreeParser {
    HashMap<ReplayProcessTree, Node> replayProcessTreeToOriginalNode;

    public ReplayProcessTree parseAndTranslateProcessTree(ProcessTree tree) {
        replayProcessTreeToOriginalNode = new HashMap<>();
        Node root = tree.getRoot();
        return translateNode(root, null);
    }

    private ReplayProcessTree translateNode(Node node, ReplayProcessTree parent) {
        ReplayProcessTree result = null;

        if (node instanceof Block.Seq || node instanceof Block.Xor || node instanceof Block.And) {
            result = translateRegularNode((Block) node, parent);
        } else if (node instanceof Block.XorLoop) {
            result = translateLoopNode((Block.XorLoop) node, parent);
        } else if (node instanceof Task.Manual) {
            result = new ReplayProcessTree(null, parent, null, node.getName(), false);
        } else if (node instanceof Task.Automatic) {
            result = new ReplayProcessTree(null, parent, null, node.getName(), true);
        }

        replayProcessTreeToOriginalNode.put(result, node);
        return result;
    }


    private ReplayProcessTree translateRegularNode(Block node, ReplayProcessTree parent) {
        Operator op = null;
        if (node instanceof Block.Seq) {
            op = Operator.SEQUENCE;
        } else if (node instanceof Block.Xor) {
            op = Operator.XOR;
        } else if (node instanceof Block.And) {
            op = Operator.PARALLEL;
        }

        ReplayProcessTree result = new ReplayProcessTree(op, parent, null, node.getName(), false);

        LinkedList<ReplayProcessTree> children = new LinkedList<>();
        for (Node child : node.getChildren()) {
            children.add(translateNode(child, result));
        }

        result.setChildren(children);
        return result;
    }

    private ReplayProcessTree translateLoopNode(Block.XorLoop node, ReplayProcessTree parent) {
        Node doChild = node.getChildren().get(0);
        Node redoChild = node.getChildren().get(1);
        Node exitChild = node.getChildren().get(2);

//        if (!(exitChild instanceof Task.Automatic)) {
        ReplayProcessTree superParent = new ReplayProcessTree(Operator.SEQUENCE, parent, null, "Seq-" + node.getName(), false);

        ReplayProcessTree subParent = new ReplayProcessTree(Operator.LOOP, superParent, null, "Loop-" + node.getName(), false);
        LinkedList<ReplayProcessTree> subChildren = new LinkedList<>();
        subChildren.add(translateNode(doChild, subParent));
        subChildren.add(translateNode(redoChild, subParent));
        subParent.setChildren(subChildren);

        LinkedList<ReplayProcessTree> superChildren = new LinkedList<>();
        superChildren.add(subParent);
        superChildren.add(translateNode(exitChild, superParent));
        superParent.setChildren(superChildren);

        return superParent;
    }

    public HashMap<ReplayProcessTree, Node> getReplayProcessTreeToOriginalNode() {
        return replayProcessTreeToOriginalNode;
    }
}
