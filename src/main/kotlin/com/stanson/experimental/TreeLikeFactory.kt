package com.stanson.experimental

interface TreeLikeFactory<T> where T : TreeLike<T> {
    fun withType(type: NodeType): T
    fun fromPrototype(prototype: T): T
    fun fromPrototypeSubTree(subTree: T): T
}
