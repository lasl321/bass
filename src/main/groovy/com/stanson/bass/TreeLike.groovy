package com.stanson.bass

/**
 * Defines the behavior of a tree-like thing.
 *
 * Created by rdahlgren on 12/25/16.
 */
interface TreeLike<T extends TreeLike<T>> {
    /**
     * Retrieve the type of this node.
     *
     * @return
     */
    TreeLikeType getType()
    /**
     * Retrieve a list of the children of this node. This method should never return null.
     *
     * @return List of children, possibly empty
     */
    List<T> getChildren()

    /**
     * Replace the children of this instance. This method should not attempt to update parent references
     * @param newChildren
     */
    void setChildren(List<T> newChildren)

    /**
     * Get the parent of this instance - may be null in the case of a root node.
     *
     * @return
     */
    T getParent()

    /**
     * Remove child at the specified index
     *
     * @param index indicates which child in the children collection
     * @return removed child if found
     * @throws IndexOutOfBoundsException if index < 0 || index >= children.size()
     */
    T removeChild(Integer index)

    /**
     * Remove the provided child if it was associated with this instance.
     *
     * @param child
     */
    void removeChild(T child)

    /**
     * Adds one or more children to this instance and returns this instance.
     *
     * @param children
     * @return this instance
     * @throws NullPointerException if children == null || children.any { it == null }
     */
    T addChildren(T... children)
    T addChildren(Iterable<T> children)

    /**
     * Visit each node in the subtree rooted at 'this' in a depth-first (pre order) manner.
     * The provided closure should expect a single parameter of type T
     * @param visitor
     */
    void depthFirstPreTraversal(Closure visitor)


    /**
     * Visit each node in the subtree rooted at 'this' in a depth-first (post order) manner.
     * The provided closure should expect a single parameter of type T
     * @param visitor
     */
    void depthFirstPostTraversal(Closure visitor)

    /**
     * Perform a depth first traversal calling the visitor closure on each encountered node.
     * @param visitor
     */
    void breadthFirstTraversal(Closure visitor)
}
