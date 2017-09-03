package com.stanson.experimental

interface TreeLike<T> where T : TreeLike<T> {
    val nodeType: NodeType
    val children: List<T>
    fun addChild(child: T): T
    fun addChildren(children: List<T>): T
    fun removeChild(child: T)
    fun dataEquivalent(other: T): Boolean
}

