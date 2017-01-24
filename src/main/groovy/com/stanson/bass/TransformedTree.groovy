package com.stanson.bass

/**
 * This class holds the root to a specified tree created by transforming the input along with
 * the list of applied transform names and trees that led to its creation.
 *
 * Created by rdahlgren on 3/12/17.
 */
final class TransformedTree<T extends TreeLike> {
    // transform name applied to the root yields the next ancestor (or this tree)
    List<Tuple> ancestors = []
    T root
}
