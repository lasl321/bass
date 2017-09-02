package com.stanson.experimental.parsing

import com.stanson.bass.NodeType

/**
 * Created by rdahlgren on 3/12/17.
 */
class BasicNodeFactory implements TreeLikeFactory<BasicNode> {

    Map<NodeType, BaseNodeType> typeMap = [
        (NodeType.NULL): BaseNodeType.NULL,
        (NodeType.ANY): BaseNodeType.ANY,
        (NodeType.ALL): BaseNodeType.ALL,
        (NodeType.NOT): BaseNodeType.NOT,
        (NodeType.PREDICATE): BaseNodeType.PREDICATE,
        (NodeType.TRUE): BaseNodeType.TRUE,
        (NodeType.FALSE): BaseNodeType.FALSE,
    ]

    /**
     * Return a new instance with the specified NodeType associated.
     *
     * @param type
     * @return
     */
    BasicNode withType(NodeType type) {
        return new BasicNode(typeMap.get(type))
    }

    /**
     * Create and return new `T` instance with the same type, data, and other identifying
     * attributes as the provided prototype. Note that tree-connections (parent, children)
     * should *NOT* be copied to the result.
     *
     * @param prototype
     * @return
     */
    BasicNode fromPrototype(BasicNode prototype) {
        return new BasicNode(prototype.parseNodeType, prototype.data)
    }

    /**
     * Create and return new `T` instance with the same type, data, and child nodes as the
     * provided subtree. Note that copies of all child nodes *should* be created and returned.
     *
     * @param prototype
     * @return copy of the provided subtree
     */
    BasicNode fromPrototypeSubtree(BasicNode subTree) {
        // process myself
        BasicNode result = fromPrototype(subTree)
        // then my children
        subTree.children.each { BasicNode child ->
            result.addChild(fromPrototypeSubtree(child))
        }
        return result
    }
}
