package com.stanson.bass

import com.stanson.parsing.ParseNode
import com.stanson.parsing.ParseNodeType
import groovy.util.logging.Log4j
import groovy.transform.CompileStatic

/**
 * Attempts to simplify provided boolean logic structures.
 *
 * Created by rdahlgren on 12/23/16.
 */
@Log4j
@CompileStatic
class BooleanAlgebraSolverService {
    static final List<ParseNodeType> COMPOSITES = [
            ParseNodeType.ANY, ParseNodeType.ALL
    ]

    static final Map<ParseNodeType, ParseNodeType> COMPOSITE_FLIP = [
            (ParseNodeType.ANY): ParseNodeType.ALL,
            (ParseNodeType.ALL): ParseNodeType.ANY
    ]

    private final List<Tuple2<Closure<Boolean>, Closure<ParseNode>>> canApplyTransform = [
            [this.&doesDeMorgansLawApply, this.&applyDeMorgansLaw] as Tuple2,
            [this.&isDegenerateComposite, this.&collapseDegenerateComposite] as Tuple2,
            [this.&doubleNegationIsPresent, this.&collapseDoubleNegation] as Tuple2,
            [this.&isIdempotentComposite, this.&collapseIdempotentComposite] as Tuple2

    ]
    /**
     * Given a ParseNode tree, attempt to return the simplest equivalent representation.
     *
     * @param input
     * @return
     */
    ParseNode solve(ParseNode input) {
        // First we will try a greedy search. Basically anything that simplifies the tree will be considered a good
        // step. Note that this will not yield optimized solutions. Sometimes the optimal solution requires taking
        // a step back in order to take two steps forward. The next version will try a two or three step lookahead.

        // Our heuristic right now is that anything reducing the depth or the number of expressions is a win, with
        // preference given to depth reduction
        // I wrap the input tree in a superfluous node here so that I don't need to do any weird gymnastics to have
        // a proper 'parent handle' when replacing children below. Consider the case when a transform should be applied
        // to the supplied input root - I would need some special case handling to detect the parentless node and update
        // the 'result' reference rather than keeping the logic uniform.

        ParseNode workingTree = new ParseNode(ParseNodeType.NULL).addChildren(input)

        Integer expressionCount = countExpressions(workingTree)
        Integer treeDepth = calculateTreeDepth(workingTree)


        Boolean progressMade = true
        while (progressMade) {
            canApplyTransform.each { Closure check, Closure transform ->
                workingTree.depthFirstPostTraversal { ParseNode parent ->
                    for (int i = 0; i < parent.children.size(); i++) {
                        ParseNode child = parent.children[i]
                        if (check(child)) {
                            parent.children[i] = transform(child)
                        }
                    }
                }
            }
            Integer newDepth = calculateTreeDepth(workingTree)
            Integer newExpressionCount = countExpressions(workingTree)
            progressMade = (newDepth < treeDepth) || (newExpressionCount < expressionCount)
            treeDepth = newDepth
            expressionCount = newExpressionCount
        }
        workingTree.children.head()
    }

    Integer countExpressions(ParseNode tree) {
        Integer count = 0
        Closure counter = { ParseNode node ->
            count++
        }
        tree.breadthFirstTraversal(counter)
        count
    }

    Integer calculateTreeDepth(ParseNode tree) {
        depthCalculatorRecursion(tree, 0)
    }

    private Integer depthCalculatorRecursion(ParseNode node, Integer parentDepth) {

        if (node.type == ParseNodeType.PREDICATE) {
            return 1 + parentDepth
        } else {
            List<Integer> childPathDepths = node.children.collect {
                depthCalculatorRecursion(it, parentDepth + 1)
            }
            return childPathDepths.max() ?: 0
        }
    }

    Boolean isIdempotentComposite(ParseNode input) {
        input.type in COMPOSITES &&
                input.children &&
                input.children.every { it == input.children.head() }
    }

    ParseNode collapseIdempotentComposite(ParseNode input) {
        input.children.head()
    }

    Boolean doubleNegationIsPresent(ParseNode input) {
        return input.type == ParseNodeType.NOT &&
                input.children &&
                input.children.head() &&
                input.children.head().type == ParseNodeType.NOT
    }

    ParseNode collapseDoubleNegation(ParseNode input) {
        input.children.head().children ? input.children.head().children.head() : null
    }

    Boolean isDegenerateComposite(ParseNode input) {
        input.type in COMPOSITES && input.children.size() < 2
    }

    ParseNode collapseDegenerateComposite(ParseNode input) {
        input.children ? input.children.head() : null
    }

    /**
     * Returns true if the subtree rooted at the provided ParseNode appears to be a case in which
     * DeMorgan's Law can be applied.
     *
     * @param input
     * @return
     */
    Boolean doesDeMorgansLawApply(ParseNode input) {
        // If the input is a negation with a composite containing two elements
        Boolean caseOne = input.type == ParseNodeType.NOT &&
                input.children.head() &&
                input.children.head().type in COMPOSITES &&
                input.children.head().children.size() == 2
        // or if the input is a composite with two negated elements
        Boolean caseTwo = input.type in COMPOSITES &&
                input.children.size() == 2 &&
                input.children.every { it.type == ParseNodeType.NOT }
        caseOne || caseTwo
    }

    /**
     * Transforms the provided input according to DeMorgan's Law.
     * @param input
     * @return
     */
    ParseNode applyDeMorgansLaw(ParseNode input) {
        ParseNode result
        // We can assume DeMorgan's does apply here, so the case determination is simple
        if (input.type in COMPOSITES) { // Composite of two negated elements -> negation of opposite composite with two
            result = new ParseNode(ParseNodeType.NOT).addChildren(
                    new ParseNode(COMPOSITE_FLIP.get(input.type)).addChildren(*input.children.collect { it.children.head() })
            )
        } else { // Negation of composite with two -> Opposite composite of two negations
            result = new ParseNode(COMPOSITE_FLIP.get(input.children.head().type)).addChildren(
                    *(input.children.head().children.collect { ParseNode child ->
                        new ParseNode(ParseNodeType.NOT).addChildren(child)
                    })
            )
        }
        result
    }
}
