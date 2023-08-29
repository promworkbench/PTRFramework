package org.processmining.ptrframework.algorithms.treereplay.NodeType;

import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.processtree.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

public abstract class ProcessTreeNode {
    ProcessTreeNode parent;
    Collection<ProcessTreeNode> openChildren;
    LinkedList<ProcessTreeNode> children;
    XFactory xFactory;
    XLog localLog;
    XTrace localTrace;
    Node correspondingNode;


    public ProcessTreeNode(ProcessTreeNode parent, XFactory xFactory, Node correspondingNode) {
        this.parent = parent;
        openChildren = new HashSet<>();
        children = new LinkedList<>();
        this.xFactory = xFactory;
        localLog = xFactory.createLog();
        localTrace = xFactory.createTrace();
        this.correspondingNode = correspondingNode;
    }

    public abstract void updateParent(ProcessTreeNode childFired, XEvent eventCreated, boolean closed, boolean includeSilent, boolean isSilent);

    public void addChild(ProcessTreeNode child) {
        children.add(child);
    }

    public void closeAll() {
        if (!localTrace.isEmpty()) {
            localLog.add(localTrace);
            localTrace = xFactory.createTrace();
        }
        children.forEach(ProcessTreeNode::closeAll);
    }

    public XLog getLocalLog() {
        return localLog;
    }
}
