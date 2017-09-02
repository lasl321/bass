package com.stanson.experimental

class TransformedTree<T> where T : TreeLike<T> {
    val ancestors: List<Triple<String, T, T>> = listOf()
    val root: T? = null
}
