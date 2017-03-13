package com.stanson.parsing

/**
 * Defines the behavior of a tree-like thing.
 *
 * Created by rdahlgren on 12/25/16.
 */
interface TreeLike {
    /**
     * Retrieve a list of the children of this node. This method should never return null.
     *
     * @return List of children, possibly empty
     */
    List<TreeLike> getChildren()

    /**
     * Get the parent of this instance - may be null.
     *
     * @return
     */
    TreeLike getParent()

    /**
     * Remove child at the specified index
     *
     * @param index indicates which child in the children collection
     * @return removed child if found
     * @throws IndexOutOfBoundsException if index < 0 || index >= children.size()
     */
    TreeLike removeChild(Integer index)

    /**
     * Remove the provided child if it was associated with this instance.
     *
     * @param child
     */
    void removeChild(TreeLike child)

    /**
     * Adds one or more children to this instance and returns this instance.
     *
     * @param children
     * @return this instance
     * @throws NullPointerException if children == null || children.any { it == null }
     */
    TreeLike addChildren(TreeLike... children)
}
