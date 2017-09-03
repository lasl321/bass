package com.stanson.experimental.parsing

import com.stanson.experimental.NodeType
import com.stanson.experimental.TreeLikeFactory

class BasicNodeFactory : TreeLikeFactory<BasicNode> {
    private val typeMap = mapOf(
            NodeType.NULL to BaseNodeType.NULL,
            NodeType.ANY to BaseNodeType.ANY,
            NodeType.ALL to BaseNodeType.ALL,
            NodeType.NOT to BaseNodeType.NOT,
            NodeType.PREDICATE to BaseNodeType.PREDICATE,
            NodeType.TRUE to BaseNodeType.TRUE,
            NodeType.FALSE to BaseNodeType.FALSE
    )

    override fun withType(type: NodeType): BasicNode {
        return BasicNode(typeMap[type]!!, null)
    }

    override fun fromPrototype(prototype: BasicNode): BasicNode {
        return BasicNode(prototype.parseNodeType, prototype.data)
    }

    override fun fromPrototypeSubtree(subTree: BasicNode): BasicNode {
        val result = fromPrototype(subTree)
        subTree.getChildren().forEach {
            result.addChild(fromPrototypeSubtree(it))
        }
        return result
    }
}
