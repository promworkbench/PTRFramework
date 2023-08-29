package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.processmining.processtree.Node;

import java.util.HashSet;

public class ProcessTreeAndNode extends ProcessTreeNode {
    public ProcessTreeAndNode(ProcessTreeNode parent, XFactory xFactory, Node node) {
        super(parent, xFactory, node);
    }

    @Override
    public void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent) {
        boolean locallyClosed = false;
        if (includeSilent || !isSilent) {
            localTrace.add(eventCreated);
        }

        if (openChildren.isEmpty()) {
            openChildren = new HashSet<>(children);
        }

        if (closed) {
            openChildren.remove(childFired);
        }

        if (openChildren.isEmpty()) {
            localLog.add(localTrace);
            localTrace = xFactory.createTrace();
            locallyClosed = true;
        }

        if (parent != null) {
            parent.updateParent(this, eventCreated, locallyClosed, includeSilent, isSilent);
        }
    }
}