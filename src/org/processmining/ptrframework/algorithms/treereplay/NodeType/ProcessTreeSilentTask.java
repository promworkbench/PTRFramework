package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.processmining.processtree.Node;

public class ProcessTreeSilentTask extends ProcessTreeNode {
    public ProcessTreeSilentTask(ProcessTreeNode parent, XFactory xFactory, Node node) {
        super(parent, xFactory, node);
    }

    @Override
    public void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent) {
        eventCreated = xFactory.createEvent();
        new XExtendedEvent(eventCreated).setName(correspondingNode.getName());
        localTrace.add(eventCreated);
        localLog.add(localTrace);
        localTrace = xFactory.createTrace();

        if (parent != null) {
            parent.updateParent(this, eventCreated, true, includeSilent, true);
        }
    }
}
