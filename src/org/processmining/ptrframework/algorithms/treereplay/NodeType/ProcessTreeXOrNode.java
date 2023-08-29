package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.processmining.processtree.Node;

public class ProcessTreeXOrNode extends ProcessTreeNode {
    public ProcessTreeXOrNode(ProcessTreeNode parent, XFactory xFactory, Node node) {
        super(parent, xFactory, node);
    }

    @Override
    public void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent) {
        if (includeSilent || !isSilent) {
            localTrace.add(eventCreated);
        }

        if (closed) {
            localLog.add(localTrace);
            localTrace = xFactory.createTrace();
        }

        if (parent != null) {
            parent.updateParent(this, eventCreated, closed, includeSilent, isSilent);
        }
    }
}
