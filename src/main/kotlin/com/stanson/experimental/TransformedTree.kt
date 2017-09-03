package com.stanson.experimental

class TransformedTree<T>(val root: T, var ancestors: MutableList<Triple<String, T, T>>) where T : TreeLike<T>
