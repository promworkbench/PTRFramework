package org.processmining.ptrframework.utils;

import java.util.*;

public class PartitionSetCreator<T> {

    private final Set<Set<Set<T>>> parts;//the partitions that are created
    private final Set<Set<T>> pow;//the power set of the input set
    private final Collection<T> base;//the base set

    public PartitionSetCreator(Collection<T> base) {
        this.base = base;
        this.pow = powerSet(new HashSet<>(base));
        if (pow.size() > 1) {
            //remove the empty set if it's not the only entry in the power set
            pow.remove(new HashSet<T>());
        }
        this.parts = new HashSet<>();
    }

    /**
     * Calculation is in this method.
     */
    public Set<Set<Set<T>>> findAllPartitions() {
        //find part sets for every entry in the power set
        for (Set<T> set : pow) {
            Set<Set<T>> current = new HashSet<>();
            current.add(set);
            findPartSets(current);
        }

        //return all partitions that were found
        return parts;
    }

    /**
     * Finds all partition sets for the given input and adds them to parts (global variable).
     */
    private void findPartSets(Set<Set<T>> current) {
        int maxLen = base.size() - deepSize(current);
        if (maxLen == 0) {
            //the current partition is full -> add it to parts
            parts.add(current);
            //no more can be added to current -> stop the recursion
        } else {
            //for all possible lengths
            for (int i = 1; i <= maxLen; i++) {
                //for every entry in the power set
                for (Set<T> set : pow) {
                    if (set.size() == i) {
                        //the set from the power set has the searched length
                        if (!anyInDeepSet(set, current)) {
                            //none of set is in current
                            Set<Set<T>> next = new HashSet<>(current);
                            next.add(set);
                            //next = current + set
                            findPartSets(next);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a power set from the base set.
     */
    private Set<Set<T>> powerSet(Collection<T> base) {
        Set<Set<T>> pset = new HashSet<>();
        if (base.isEmpty()) {
            pset.add(new HashSet<>());
            return pset;
        }
        List<T> list = new ArrayList<>(base);
        T head = list.get(0);
        Set<T> rest = new HashSet<>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            pset.add(newSet);
            pset.add(set);
        }

        return pset;
    }

    /**
     * The summed upsize of all sub-sets
     */
    private int deepSize(Set<Set<T>> set) {
        int deepSize = 0;
        for (Set<T> s : set) {
            deepSize += s.size();
        }
        return deepSize;
    }

    /**
     * Checks whether any of set is in any of the sub-sets of current
     */
    private boolean anyInDeepSet(Set<T> set, Set<Set<T>> current) {
        boolean containing = false;

        for (Set<T> s : current) {
            for (T item : set) {
                containing |= s.contains(item);
            }
        }

        return containing;
    }
}
