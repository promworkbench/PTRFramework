package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

public enum Operator {
    SEQUENCE,
    XOR,
    PARALLEL,
    LOOP,
    OR,
    INTERLEAVING;

    public String toString() {
        switch (this) {
            case SEQUENCE:
                return "->";
            case XOR:
                return "X";
            case PARALLEL:
                return "+";
            case LOOP:
                return "*";
            case OR:
                return "O";
            case INTERLEAVING:
                return "<>";
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
