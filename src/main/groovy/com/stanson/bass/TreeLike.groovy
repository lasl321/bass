package com.stanson.bass

/**
 * TreeLike is the basic interface required for integrating with the BASS library. A TreeLike is a basic
 * recursive data type having child links along with some basic functionality for managing
 * these. Additionally a comparison method is provided to compare only the data associated with two TreeLike
 * instances (as opposed to comparing the data and the parent/child links).
 *
 * Standard equals and hashCode behavior are expected to be present.
 *
 * Created by rdahlgren on 12/25/16.
 */
interface TreeLike<T extends TreeLike<T>> {
    /**
     * Return the node type associated with this instance.
     *
     * @return
     */
    NodeType getType()

    /**
     * Retrieve a list of the children of this node. This method should never return null.
     *
     * All returned children should have a parent of this instance.
     *
     * @return List of children, possibly empty
     */
    List<T> getChildren()

    /**
     * Remove the provided child if it was associated with this instance.
     * Should set the parent link of the removed child to 'null'.
     *
     * @param child
     */
    void removeChild(T child)

    /**
     * Adds all provided children to this instance and returns this instance.
     *
     * All added children should have their parent link set to this instance.
     *
     * @param children
     * @return this instance
     * @throws NullPointerException if children == null || children.any { it == null }
     */
    T addChildren(List<T> children)

    /**
     * Add provided child to this instance and returns this instance.
     *
     * All added children should have their parent link set to this instance.
     *
     * @param children
     * @return this instance
     * @throws NullPointerException if child == null
     */
    T addChild(T child)

    /**
     * True if the data associated with this instance and the other instance are considered
     * to be equivalent enough to be interchangeable.
     *
     * @return true if this node has similar data to the other
     */
    Boolean dataEquivalent(T other)
}
