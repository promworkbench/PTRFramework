package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

import org.processmining.ptrframework.utils.Pair;

import java.util.LinkedList;

public class SGASearchResult {
    private LinkedList<Pair<Object, Object>> alignment;

    public SGASearchResult() {
    }

    public LinkedList<Pair<Object, Object>> getAlignment() {
        return alignment;
    }

    public void setAlignment(LinkedList<Pair<Object, Object>> alignment) {
        this.alignment = alignment;
    }
}
