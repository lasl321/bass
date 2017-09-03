package com.stanson.experimental.parsing

import com.stanson.experimental.NodeType
import com.stanson.experimental.TreeLikeFactory

class BasicNodeFactory : TreeLikeFactory<BasicNode> {
    override fun withType(type: NodeType): BasicNode {
        return BasicNode(getNodeType(type))
    }

    override fun fromPrototype(prototype: BasicNode): BasicNode {
        return BasicNode(prototype.type, prototype.data)
    }

    override fun fromPrototypeSubTree(subTree: BasicNode): BasicNode {
        val result = fromPrototype(subTree)
        subTree.children.forEach {
            result.addChild(fromPrototypeSubTree(it))
        }
        return result
    }

    private fun getNodeType(type: NodeType): BaseNodeType {
        return when (type) {
            NodeType.NULL -> BaseNodeType.NULL
            NodeType.ANY -> BaseNodeType.ANY
            NodeType.ALL -> BaseNodeType.ALL
            NodeType.NOT -> BaseNodeType.NOT
            NodeType.PREDICATE -> BaseNodeType.PREDICATE
            NodeType.TRUE -> BaseNodeType.TRUE
            NodeType.FALSE -> BaseNodeType.FALSE
        }
    }
}
