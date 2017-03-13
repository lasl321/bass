package com.stanson.bass

/**
 * Implementors provide several means to construct new TreeLike instances.
 *
 * Created by rdahlgren on 3/12/17.
 */
interface TreeLikeFactory<T extends TreeLike> {
    /**
     * Return a new instance with the specified NodeType associated.
     *
     * @param type
     * @return
     */
    T withType(NodeType type)

    /**
     * Create and return new `T` instance with the same type, data, and other identifying
     * attributes as the provided prototype. Note that tree-connections (parent, children)
     * should *NOT* be copied to the result.
     *
     * @param prototype
     * @return
     */
    T fromPrototype(T prototype)


    /**
     * Create and return new `T` instance with the same type, data, and child nodes as the
     * provided subtree. Note that copies of all child nodes *should* be created and returned.
     *
     * @param prototype
     * @return copy of the provided subtree
     */
    T fromPrototypeSubtree(T subTree)
}
