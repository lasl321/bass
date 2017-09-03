package com.stanson.experimental.parsing

import com.stanson.experimental.NodeType
import com.stanson.experimental.TreeLike

class BasicNode : TreeLike<BasicNode> {
    val type: BaseNodeType
    val data: Any?

    override val children: MutableList<BasicNode>

    private var parent: BasicNode? = null

    constructor(parseNodeType: BaseNodeType) : this(parseNodeType, null)

    constructor(type: BaseNodeType, data: Any?) {
        this.type = type
        this.data = data
        this.children = mutableListOf()
    }

    fun addChildren(vararg children: BasicNode): BasicNode {
        children.forEach { this.addChild(it) }
        return this
    }

    fun removeAllChildren() {
        children.forEach(this::removeChild)
    }

    override fun addChild(child: BasicNode): BasicNode {
        child.parent?.removeChild(child)
        children.add(child)
        child.parent = this
        return this
    }

    override fun removeChild(child: BasicNode) {
        children.remove(child)
        child.parent = null
    }

    override fun addChildren(children: List<BasicNode>): BasicNode {
        children.forEach { this.addChild(it) }
        return this
    }

    override fun dataEquivalent(other: BasicNode): Boolean {
        return this.data == other.data
    }

    override fun hashCode(): Int {
        var result = 31
        result = 17 * result + nodeType.hashCode()
        result = 17 * result + (data?.hashCode() ?: 0)
        result = 17 * result + children.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BasicNode) {
            return false
        }

        return this.nodeType == other.nodeType &&
                this.data == other.data &&
                this.children.size == other.children.size &&
                (this.children.toSet()) == (other.children.toSet())
    }

    override fun toString(): String {
        return toStringHelper("")
    }

    private fun toStringHelper(indent: String): String {
        val dataString = this.data?.toString() ?: "NODATA"

        return "$indent $nodeType ($dataString)\n" + children.joinToString(",") {
            it.toStringHelper("$indent>")
        }
    }

    override val nodeType: NodeType
        get() {
            return when (type) {
                BaseNodeType.NULL -> NodeType.NULL
                BaseNodeType.ANY -> NodeType.ANY
                BaseNodeType.ALL -> NodeType.ALL
                BaseNodeType.NOT -> NodeType.NOT
                BaseNodeType.PREDICATE -> NodeType.PREDICATE
                BaseNodeType.TRUE -> NodeType.TRUE
                BaseNodeType.FALSE -> NodeType.FALSE
            }
        }
}
