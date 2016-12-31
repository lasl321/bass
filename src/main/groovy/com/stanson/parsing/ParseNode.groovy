package com.stanson.parsing

import groovy.transform.CompileStatic

/**
 * This file provides a number of utilities to aid in the handling of parse
 * trees during the transformation process.
 */
enum ParseNodeType { NULL, ANY, ALL, NOT, PREDICATE, TRUE, FALSE }

/**
 * A node holds data and zero or more references to children.
 *
 * TODO: Allow the injection of a data comparator. Consider comparing two identical predicates that differ only
 * in their generated predicate '_id'. Currently this would evaluate to not-equal, though semantically they may
 * be the same.
 */
@CompileStatic
class ParseNode {
    /** Short-hand to determine what this node represents. */
    ParseNodeType type = ParseNodeType.NULL
    /** Holds a reference to whatever data is desired. */
    def data

    /** Backwards reference is maintained through addChild / removeChild methods */
    ParseNode parent = null

    /**
     * Collection of nodes forming a subtree from this node.
     * RD 26Feb2015 13:13:09 - I have changed this collection from a Set to a List. While
     *  adding a validation to check for duplicate predicate IDs, I realized that the parse node
     *  doesn't support duplicate children. We were seeing this exact behavior on prod, which
     *  means that it can happen, but the object that gets transformed will not reflect the data
     *  that was included. Since JSON has no notion of a set type, I've chosen to update this
     *  data structure to not use a set type either. Now, validations and other breakage can
     *  occur in response to bad data, rather than silently hiding it while confusing the user
     *  on the front end.
     */
    List<ParseNode> children = []

    ParseNode(ParseNodeType type = ParseNodeType.NULL, def data = null) {
        this.type = type
        this.data = data
    }

    /**
     * Convenience method to save some typing when adding multiple children. The provided children are
     * added in-order.
     * @param children
     * @return this
     */
    ParseNode addChildren(ParseNode... children) {
        children.each { ParseNode child -> this.addChild(child) }
        this
    }

    void removeAllChildren() {
        children.each { ParseNode child -> this.removeChild(child) }
    }

    /**
     * Add the provided node to this node's children, set this node as the
     * provided node's parent.
     */
    void addChild(ParseNode child) {
        if (child.parent) { child.parent.removeChild(child) }
        children << child
        child.parent = this
    }

    /**
     * If the specified child belongs to this node, renounce parenthood and
     * and kick that child to the curb.
     */
    void removeChild(ParseNode child) {
        children -= child
        child.parent = null
    }

    /**
     * Computes and returns the hashcode of the subtree formed by interpreting this node as the root.
     */
    int hashCode() {
        int result = 31
        result = 17 * result + (type ? type.hashCode() : 0)
        // Need '!= null' check as '0' is a valid, but falsy data value
        result = 17 * result + (data != null ? data.hashCode() : 0)
        result = 17 * result + children.hashCode()
        return result
    }

    /**
     * A == B IFF this node and all children (the subtree) are equal.
     */
    boolean equals(Object obj) {
        if (!(obj instanceof ParseNode)) { return false }
        ParseNode that = obj as ParseNode
        return this.type == that.type &&
                this.data == that.data &&
                this.children.size() == that.children.size() &&
                (this.children as Set<ParseNode>) == (that.children as Set<ParseNode>)
    }

    String toString() { toStringHelper('') }

    protected String toStringHelper(String indent) {
        String dataString = this.data ? "${this.data}" : 'NODATA'
        String result = "${indent} ${this.type} (${dataString})\n"
        result += this.children.collect {
            it.toStringHelper("${indent}>")
        }.join(",")
        result
    }

    /*********************************************************
    ** Utility methods follow: traversal, copying, pruning  **
    *********************************************************/

    /**
     * Make a duplicate of this tree.
     * Linked `data` elements are copied by reference. Users of a copy should take care to make defensive copies
     * of data objects if they need to be changed.
     */
    ParseNode copy() { treeCopy(this, true) }
    ParseNode shallowCopy() { treeCopy(this, false) }
    private ParseNode treeCopy(ParseNode node, Boolean deepCopy) {
        // RD 2015-07-02 09:20:30 The data copy here is fragile, I know
        def newData = node.data
        if (deepCopy) {
            if (node.data && node.data.getClass().isAssignableFrom(Map)) {
                // try making a single level map copy
                newData = ((Map) node.data).collectEntries { k, v -> [(k): v] }
            } else if (node.data && node.data.getClass().isAssignableFrom(List)) {
                // try making a list copy
                newData = ((List) node.data).collect()
            }
        }
        ParseNode result = new ParseNode(node.type, newData)
        node.children.each { child -> result.addChild(treeCopy(child, deepCopy)) }
        result
    }

    /**
     * Visit each node in the tree, pruning those that the filter
     * indicates should be pruned (by returning true).
     */
    void filter(Closure filter) { pruningLogic(filter, this) }
    private void pruningLogic(Closure filter, ParseNode node) {
        node.children.each { ParseNode child ->
            if (filter.call(child)) {
                node.removeChild(child)
            } else {
                pruningLogic(filter, child)
            }
        }
    }


    /**
     * While queue not empty, pop next, process it, append all it's children to
     * the queue for later processing.
     */
    void breadthFirstTraversal(Closure visitor) {
        Queue q = [] as Queue
        q.add(this)
        while (q.isEmpty() == false) {
            ParseNode curr = q.poll()
            visitor.call(curr)
            q.addAll(curr.children)
        }
    }

    /**
     * Process myself, then recurse for each child. This causes nodes to be processed while walking down
     * the tree.
     */
    void depthFirstPreTraversal(Closure visitor) { dftPre(this, visitor) }
    protected void dftPre(ParseNode node, Closure visitor) {
        visitor.call(node)
        node.children.each { child -> dftPre(child, visitor) }
    }

    /**
     * Recurse for each child, then process myself. This causes processing to start from the bottom of
     * the tree while walking back towards the root.
     */
    void depthFirstPostTraversal(Closure visitor) { dftPost(this, visitor) }
    protected void dftPost(ParseNode node, Closure visitor) {
        node.children.each { child -> dftPost(child, visitor) }
        visitor.call(node)
    }
}
