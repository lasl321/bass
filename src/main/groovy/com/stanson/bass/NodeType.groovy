package com.stanson.bass

/**
 * ParseNodeType is a flag used to indicate how the associated node should be interpreted. Specific
 * details on what each value is intended to represent can be found alongside the value declaration
 * below.
 *
 * Created by rdahlgren on 3/12/17.
 */
enum NodeType {
    /**
     * Used to indicate this node should not be considered part of the data being modeled by the tree.
     * For instance, in some instances it is helpful to have a "handle" node at the top of the tree. In
     * order to maintain the existing semantic structure of the tree, this new root handle should have
     * type 'NULL'.
     */
    NULL,
    /**
     * Indicates a composite node (a node understood to have children) where the children should be
     * combined using 'or' logic.
     */
    ANY,
    /**
     * Indicates a composite node (a node understood to have children) where the children should be
     * combined using 'and' logic.
     */
    ALL,
    /**
     * Indicates a node containing a single child whose value should be negated.
     */
    NOT,
    /**
     * Indicates a node whose value is computed at using some expression.
     */
    PREDICATE,
    /**
     * This node is equivalent to the constant value 'true'.
     */
    TRUE,
    /**
     * This node is equivalent to the constant value 'false'.
     */
    FALSE
}
