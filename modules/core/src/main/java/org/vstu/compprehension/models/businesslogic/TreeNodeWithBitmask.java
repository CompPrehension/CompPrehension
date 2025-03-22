package org.vstu.compprehension.models.businesslogic;

public interface TreeNodeWithBitmask extends Comparable<TreeNodeWithBitmask> {

    String getName();

    long getBitmask();

    long getSubTreeBitmask();

    int getSortOrder();

    default int compareTo(TreeNodeWithBitmask other) {
        // comparing sort-order
        if (this.getSortOrder() != other.getSortOrder()) {
            return this.getSortOrder() - other.getSortOrder();
        }

        // comparing name
        return this.getName().compareTo(other.getName());
    }
}
