package com.stanson.experimental

data class TransformItem<T>(val name: String, val test: (T) -> Boolean, val action: (T) -> T?) where T : TreeLike<T>
