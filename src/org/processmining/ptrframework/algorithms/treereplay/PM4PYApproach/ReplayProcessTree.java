package org.processmining.ptrframework.algorithms.treereplay.PM4PYApproach;

import java.util.LinkedList;

public class ReplayProcessTree {
    private final Operator operator;
    private final String label;
    private final boolean isSilent;
    private ReplayProcessTree parent;
    private LinkedList<ReplayProcessTree> children;

    public ReplayProcessTree(Operator operator, ReplayProcessTree parent, LinkedList<ReplayProcessTree> children, String label, boolean isSilent) {
        this.operator = operator;
        this.parent = parent;
        this.children = children != null ? children : new LinkedList<>();
        this.label = label;
        this.isSilent = isSilent;
    }

    @Override
    public int hashCode() {
        if (label != null) {
            return label.hashCode();
        } else if (children.isEmpty()) {
            return 37;
        } else {
            int h = 1337;

            for (int i = 0; i < children.size(); i++) {
                h += 41 * i * children.get(i).hashCode();
            }

            if (operator == Operator.SEQUENCE) {
                h = h * 13;
            } else if (operator == Operator.XOR) {
                h = h * 17;
            } else if (operator == Operator.OR) {
                h = h * 23;
            } else if (operator == Operator.PARALLEL) {
                h = h * 29;
            } else if (operator == Operator.LOOP) {
                h = h * 37;
            } else if (operator == Operator.INTERLEAVING) {
                h = h * 41;
            }

            return h % 268435456;
        }
    }

    public Operator getOperator() {
        return operator;
    }

    public ReplayProcessTree getParent() {
        return parent;
    }

    public void setParent(ReplayProcessTree parent) {
        this.parent = parent;
    }

    public LinkedList<ReplayProcessTree> getChildren() {
        return children;
    }

    public void setChildren(LinkedList<ReplayProcessTree> children) {
        this.children = children;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ReplayProcessTree) {
            if (label != null) {
                return label.equals(((ReplayProcessTree) o).label);
            } else if (children.isEmpty()) {
                return ((ReplayProcessTree) o).label == null && ((ReplayProcessTree) o).children.isEmpty();
            } else {
                if (operator.equals(((ReplayProcessTree) o).operator)) {
                    if (children.size() != ((ReplayProcessTree) o).children.size()) {
                        return false;
                    } else {
                        for (int i = 0; i < children.size(); i++) {
                            if (!children.get(i).equals(((ReplayProcessTree) o).children.get(i))) {
                                return false;
                            }
                        }
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public String getRepresentation() {
        if (operator != null) {
            StringBuilder representation = new StringBuilder(operator + "( ");
            for (int i = 0; i < children.size(); i++) {
                ReplayProcessTree child = children.get(i);
                if (child.children.isEmpty()) {
                    if (child.label != null) {
                        representation.append("'").append(child).append("'").append(i < children.size() - 1 ? ", " : "");
                    } else {
                        representation.append(child).append(i < children.size() - 1 ? ", " : "");
                    }
                } else {
                    representation.append(child).append(i < children.size() - 1 ? ", " : "");
                }
            }
            representation.append(" )");
            return representation.toString();
        } else if (label != null) {
            return label;
        } else {
            return "tau";
        }
    }

    @Override
    public String toString() {
        return getRepresentation();
    }

    public boolean isSilent() {
        return isSilent;
    }

    enum OperatorState {
        ENABLED,
        OPEN,
        CLOSED,
        FUTURE;

        public String toString() {
            switch (this) {
                case ENABLED:
                    return "Enabled";
                case OPEN:
                    return "Open";
                case CLOSED:
                    return "Closed";
                case FUTURE:
                    return "Future";
                default:
                    throw new IllegalStateException("Unexpected value: " + this);
            }
        }
    }
}
