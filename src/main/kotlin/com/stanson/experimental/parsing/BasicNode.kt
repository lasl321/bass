package com.stanson.experimental.parsing

import com.stanson.bass.NodeType
import com.stanson.experimental.TreeLike
import groovy.transform.CompileStatic

class BasicNode(val parseNodeType: BaseNodeType, val data: Any) : TreeLike<BasicNode> {
    var parent: BasicNode

    //var  children:List<BasicNode> = listOf()

    

     fun addChildren(vararg children : BasicNode) :BasicNode{
        children.forEach { this.addChild(it) }
        this
    }

    fun removeAllChildren() :Unit{
        children.forEach {  this.removeChild(it) }
    }

     fun addChild( child:BasicNode) :BasicNode{
        if (child.parent) { child.parent.removeChild(child) }
        children  child
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
        result = 17 * result + (nodeType ? nodeType.hashCode() : 0)
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
        return this.nodeType == that.nodeType &&
                this.data == that.data &&
                this.children.size() == that.children.size() &&
                (this.children as Set<BasicNode>) == (that.children as Set<BasicNode>)
    }

    String toString() { toStringHelper('') }

    protected String toStringHelper(String indent) {
        String dataString = this.data ? "${this.data}" : 'NODATA'
        String result = "${indent} ${this.nodeType} (${dataString})\n"
        result += this.children.collect {
            it.toStringHelper("${indent}>")
        }.join(",")
        result
    }

    NodeType getNodeType() {
        // NULL, ANY, ALL, NOT, PREDICATE, TRUE, FALSE
        [
                (BaseNodeType.NULL)     : NodeType.NULL,
                (BaseNodeType.ANY)      : NodeType.ANY,
                (BaseNodeType.ALL)      : NodeType.ALL,
                (BaseNodeType.NOT)      : NodeType.NOT,
                (BaseNodeType.PREDICATE): NodeType.PREDICATE,
                (BaseNodeType.TRUE)     : NodeType.TRUE,
                (BaseNodeType.FALSE)    : NodeType.FALSE,
        ].get(this.parseNodeType)
    }
}
