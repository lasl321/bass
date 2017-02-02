package com.stanson.bass

/**
 * Indicates the semantic parseNodeType of a node.
 *
 * Created by rdahlgren on 2/2/17.
 */
enum TreeLikeType {
    NULL, /* Unspecified parseNodeType. Used to mark some root nodes. May or may not have children */
    OR, /* Indicates a OR parseNodeType node. Children should be combined using the OR operator */
    AND, /* Indicates an AND parseNodeType node. Children should be combined using the AND operator */
    NOT, /* Indicates the children (usually one) should be negated */
    PREDICATE, /* Indicates the node represents a condition that will evaluate to a boolean value. Should not have children */
    TRUE, /* The node is true. Should not contain children */
    FALSE /* The node is false. Should not contain children */
}
