package com.stanson.experimental.parsing

import com.stanson.experimental.NodeType
import com.stanson.experimental.TreeLike

class BasicNode : TreeLike<BasicNode> {
    val type: BaseNodeType
    val data: Any?

    private var parent: BasicNode? = null
    private val children: MutableList<BasicNode>

    constructor(parseNodeType: BaseNodeType) : this(parseNodeType, null)

    constructor(type: BaseNodeType, data: Any?) {
        this.type = type
        this.data = data
        this.children = mutableListOf<BasicNode>()
    }

    override fun getChildren(): List<BasicNode> {
        return children
    }

    fun addChildren(vararg children: BasicNode): BasicNode {
        children.forEach { this.addChild(it) }
        return this
    }

    fun removeAllChildren() {
        getChildren().forEach { this.removeChild(it) }
    }

    override fun addChild(child: BasicNode): BasicNode {
        if (child.parent != null) {
            child.parent!!.removeChild(child)
        }
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
        result = 17 * result + getNodeType().hashCode()
        result = 17 * result + (data?.hashCode() ?: 0)
        result = 17 * result + getChildren().hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BasicNode) {
            return false
        }

        return this.getNodeType() == other.getNodeType() &&
                this.data == other.data &&
                this.children.size == other.children.size &&
                (this.children.toSet()) == (other.children.toSet())
    }

    override fun toString(): String {
        return toStringHelper("")
    }

    private fun toStringHelper(indent: String): String {
        val dataString = if (this.data != null) {
            "${this.data}"
        } else {
            "NODATA"
        }

        var result = "$indent ${this.getNodeType()} ($dataString)\n"
        result += this.children.joinToString(",") {
            it.toStringHelper("$indent>")
        }

        return result
    }

    override fun getNodeType(): NodeType {
        val m = mapOf(
                BaseNodeType.NULL to NodeType.NULL,
                BaseNodeType.ANY to NodeType.ANY,
                BaseNodeType.ALL to NodeType.ALL,
                BaseNodeType.NOT to NodeType.NOT,
                BaseNodeType.PREDICATE to NodeType.PREDICATE,
                BaseNodeType.TRUE to NodeType.TRUE,
                BaseNodeType.FALSE to NodeType.FALSE)

        return m[type]!!
    }
}
