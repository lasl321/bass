package com.stanson.bass

/**
 * Created by rdahlgren on 2/2/17.
 */
interface TreeLikeComparator<T extends TreeLike> {
    /**
     * Should return true if the node l has data that is semantically equivalent to the data of node r.
     * This method should consider two nodes with the same data to be equivalent even if their subtrees or
     * parents are different.
     * This method should safely handle nulls.
     * @param l
     * @param r
     * @return
     */
    Boolean areCongruent(T l, T r)

    /**
     * Should return true if the two nodes are both congruent and have identical subtrees and parents.
     * @param l
     * @param r
     * @return
     */
    Boolean areIdentical(T l, T r)
}
