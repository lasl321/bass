package com.stanson.bass

/**
 * Created by rdahlgren on 2/2/17.
 */
interface TreeLikeFactory<T extends TreeLike> {
    /**
     * Given a TreeLike instance, return a new instance of the same type and with identical data. The children
     * and parent links should not be copied.
     * @param other
     * @return A new instance with the same type and data as the provided instance
     */
    T fromExistingInstance(T other)

    /**
     *
     * @param type
     * @param data
     * @return
     */
    T newInstance(TreeLikeType type, Object data)
}
