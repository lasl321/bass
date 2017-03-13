package com.stanson.parsing

import com.stanson.bass.NodeType
import com.stanson.bass.TreeLike
import groovy.transform.CompileStatic

enum ParseNodeType { NULL, ANY, ALL, NOT, PREDICATE, TRUE, FALSE }

/**
 * A node holds data and zero or more references to children.
 *
 * TODO: Allow the injection of a data comparator. Consider comparing two identical predicates that differ only
 * in their generated predicate '_id'. Currently this would evaluate to not-equal, though semantically they may
 * be the same.
 */
@CompileStatic
class BasicNode implements TreeLike<BasicNode> {
    /** Short-hand to determine what this node represents. */
    ParseNodeType parseNodeType = ParseNodeType.NULL
    /** Holds a reference to whatever data is desired. */
    def data

    /** Backwards reference is maintained through addChild / removeChild methods */
    BasicNode parent = null

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
    List<BasicNode> children = []

    BasicNode(ParseNodeType type = ParseNodeType.NULL, def data = null) {
        this.parseNodeType = type
        this.data = data
    }

    /**
     * Convenience method to save some typing when adding multiple children. The provided children are
     * added in-order.
     * @param children
     * @return this
     */
    BasicNode addChildren(BasicNode... children) {
        children.each { BasicNode child -> this.addChild(child) }
        this
    }

    void removeAllChildren() {
        children.each { BasicNode child -> this.removeChild(child) }
    }

    /**
     * Add the provided node to this node's children, set this node as the
     * provided node's parent.
     */
    BasicNode addChild(BasicNode child) {
        if (child.parent) { child.parent.removeChild(child) }
        children << child
        child.parent = this
        this
    }

    /**
     * If the specified child belongs to this node, renounce parenthood and
     * and kick that child to the curb.
     */
    void removeChild(BasicNode child) {
        children -= child
        child.parent = null
    }

    /**
     * Adds all provided children to this instance and returns this instance.
     *
     * All added children should have their parent link set to this instance.
     *
     * @param children
     * @return this instance
     * @throws NullPointerException if children == null || children.any { it == null }
     */
    @Override
    BasicNode addChildren(List<BasicNode> children) {
        children.each { BasicNode child -> this.addChild(child) }
        this
    }

    /**
     * True if the data associated with this instance and the other instance are considered
     * to be equivalent enough to be interchangeable.
     *
     * @return true if this.data == other.data
     */
    @Override
    Boolean dataEquivalent(BasicNode other) {
        return this.data == other.data
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
        if (!(obj instanceof BasicNode)) { return false }
        BasicNode that = obj as BasicNode
        return this.type == that.type &&
                this.data == that.data &&
                this.children.size() == that.children.size() &&
                (this.children as Set<BasicNode>) == (that.children as Set<BasicNode>)
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

    NodeType getType() {
        // NULL, ANY, ALL, NOT, PREDICATE, TRUE, FALSE
        [
                (ParseNodeType.NULL): NodeType.NULL,
                (ParseNodeType.ANY): NodeType.ANY,
                (ParseNodeType.ALL): NodeType.ALL,
                (ParseNodeType.NOT): NodeType.NOT,
                (ParseNodeType.PREDICATE): NodeType.PREDICATE,
                (ParseNodeType.TRUE): NodeType.TRUE,
                (ParseNodeType.FALSE): NodeType.FALSE,
        ].get(this.parseNodeType)
    }
}
