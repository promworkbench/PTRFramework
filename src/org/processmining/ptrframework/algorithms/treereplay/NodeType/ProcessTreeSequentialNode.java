package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.processmining.processtree.Node;

public class ProcessTreeSequentialNode extends ProcessTreeNode {


    public ProcessTreeSequentialNode(ProcessTreeNode parent, XFactory xFactory, Node node) {
        super(parent, xFactory, node);
    }

    @Override
    public void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent) {
        boolean locallyClosed = false;
        if (includeSilent || !isSilent) {
            localTrace.add(eventCreated);
        }

        if (children.indexOf(childFired) == children.size() - 1 && closed) {
            localLog.add(localTrace);
            localTrace = xFactory.createTrace();
            locallyClosed = true;
        }

        if (parent != null) {
            parent.updateParent(this, eventCreated, locallyClosed, includeSilent, isSilent);
        }
    }
}
