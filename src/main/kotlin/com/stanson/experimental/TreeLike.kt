package com.stanson.experimental

interface TreeLike<T> where T : TreeLike<T> {
    fun getNodeType(): NodeType
    fun getChildren(): List<T>
    fun removeChild(child: T): Unit
    fun addChildren(children: List<T>): T
    fun addChild(child: T): T
    fun dataEquivalent(other: T): Boolean
}

