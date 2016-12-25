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
            [this.&isIdempotentComposite, this.&collapseIdempotentComposite] as Tuple2,
            [this.&containsCollapsibleComposites, this.&collapseComposite] as Tuple2,
            [this.&canExtractCommonTerm, this.&extractCommonTerm] as Tuple2,
    ]

    Integer lookAhead = 1
    /**
     * Given a ParseNode tree, attempt to return the simplest equivalent representation.
     *
     * @param input
     * @return
     */
    ParseNode solve(ParseNode input) {
        // I wrap the input tree in a superfluous node here so that I don't need to do any weird gymnastics to have
        // a proper 'parent handle' when replacing children below. Consider the case when a transform should be applied
        // to the supplied input root - I would need some special case handling to detect the parentless node and update
        // the 'result' reference rather than keeping the logic uniform.

        ParseNode workingTree = new ParseNode(ParseNodeType.NULL).addChildren(input)

        Integer expressionCount = countExpressions(workingTree)
        Integer treeDepth = calculateTreeDepth(workingTree)


        List<ParseNode> transformedTrees = []
        Boolean progressMade = true
        while (progressMade) {
            log.info('Generating permutations.')
            generatePermutations(transformedTrees, lookAhead, 1, workingTree)
            if (transformedTrees.size() == 1) {
                log.info('Single result set.')
                workingTree = transformedTrees.get(0)
            } else {
                log.info('Finding minimal result from the result list')

                workingTree = transformedTrees.min {
                    calculateTreeDepth(it) + countExpressions(it)
                }
            }

            Integer newDepth = calculateTreeDepth(workingTree)
            Integer newExpressionCount = countExpressions(workingTree)
            progressMade = (newDepth < treeDepth) || (newExpressionCount < expressionCount)
            treeDepth = newDepth
            expressionCount = newExpressionCount
            transformedTrees.clear()
        }
        workingTree.children.head()
    }

    /**
     * Descend up to `depth` creating all possible permutations of applicable transforms.
     *
     * @param result Stores generated permutations
     * @param depth Desired recursion depth. 2 or 3 should suffice.
     * @param currentDepth Stack-storage of the current depth
     * @param root Root of the tree to generate permutations for
     */
    void generatePermutations(List<ParseNode> result, Integer depth, Integer currentDepth, ParseNode root) {
        result.add(root)
        Closure visitor = { ParseNode p ->
            // For each possible transform, see if it applies to this node. If so, transform
            // the corresponding node on a copy tree and add the result to the permutation list
            canApplyTransform.each { Closure<Boolean> check, Closure<ParseNode> transform ->
                if (check(p)) {
                    ParseNode transformedTree = createTransformedTree(root, p, transform)
                    result.add(transformedTree)
                    if (currentDepth < depth) { // recurse if desired
                        // traverse from the root of the transformed tree
                        generatePermutations(result, depth, currentDepth + 1, transformedTree)
                    }
                }
            }
        }
        root.depthFirstPostTraversal(visitor)
    }

    /**
     * Helper method to generate a tree copy that transforms the target node when encountered.
     * @param root
     * @param target
     * @param transform
     * @return
     */
    ParseNode createTransformedTree(ParseNode root, ParseNode target, Closure<ParseNode> transform) {
        ParseNode result = new ParseNode(root.type, root.data)
        root.children.each { ParseNode child ->
            ParseNode childCopy = createTransformedTree(child, target, transform)
            if (child == target) {
                childCopy = transform(childCopy)
            }
            if (childCopy) { // a pruning could have occurred
                result.addChild(childCopy)
            } else {
                result.removeChild(target)
            }
        }
        result
    }

    /**
     * Counts the number of nodes in the tree. Used as a measure of complexity.
     *
     * @param tree
     * @return
     */
    Integer countExpressions(ParseNode tree) {
        Integer count = 0
        Closure counter = { ParseNode node ->
            count++
        }
        tree.breadthFirstTraversal(counter)
        count
    }

    /**
     * Determines how deep a tree goes.
     *
     * @param tree
     * @return
     */
    Integer calculateTreeDepth(ParseNode tree) {
        depthCalculatorRecursion(tree, 0)
    }

    /**
     * Recursive helper.
     *
     * @param node
     * @param parentDepth
     * @return
     */
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

    /**
     * True if this node is a composite and more than one child of this node is of the opposite composite type
     * and those qualifying children share at least one common term.
     * @param input
     * @return
     */
    Boolean canExtractCommonTerm(ParseNode input) {
        if (input.type in COMPOSITES) {
            List<ParseNode> oppositeKids = input.children.findAll { it.type == COMPOSITE_FLIP.get(input.type) }
            if (oppositeKids.size() > 1) {
                // Get the children of each opposite kid as a Set, then check for a non-empty intersection between them
                List<Set<ParseNode>> oppositeKidsChildren = oppositeKids.collect { it.children as Set<ParseNode> }
                Set<ParseNode> intersection = oppositeKidsChildren.inject {
                    Set<ParseNode> intersection, Set<ParseNode> compositeEntries ->
                        intersection.intersect(compositeEntries)
                }
                return !intersection.isEmpty()
            }
        }
        false
    }

    /**
     * Extracts a common term from a node identified as qualifying for this transform.
     * @param input
     * @return
     */
    ParseNode extractCommonTerm(ParseNode input) {
        // Remove everything that's being pushed down a level
        List<ParseNode> oppositeCompositeKids = input.children.findAll { it.type == COMPOSITE_FLIP.get(input.type) }
        oppositeCompositeKids.each { input.removeChild(it) }

        // Get the common term (just take the first)
        ParseNode commonTerm = oppositeCompositeKids.collect {
            it.children as Set<ParseNode>
        }.inject {
            Set<ParseNode> intersection, Set<ParseNode> compositeEntries -> intersection.intersect(compositeEntries)
        }.head()
        // Make the new composites
        ParseNode newOpposite = new ParseNode(COMPOSITE_FLIP.get(input.type))
        newOpposite.addChild(commonTerm) // common term goes here
        input.addChild(newOpposite) // this is attached to the input

        ParseNode newSame = new ParseNode(input.type)

        // Now add the opposite composite grandkids to the 'newSame' collection
        List<ParseNode> remainingGrandkids = oppositeCompositeKids.collectMany {
            it.children.findAll { it != commonTerm }
        }
        remainingGrandkids.each { newSame.addChild(it) }
        newOpposite.addChild(newSame) // all the kids added go under the new opposite
        input
    }

    /**
     * True if this node is a composite and contains equivalent children.
     * @param input
     * @return
     */
    Boolean isIdempotentComposite(ParseNode input) {
        input.type in COMPOSITES &&
                input.children &&
                input.children.every { it == input.children.head() }
    }

    ParseNode collapseIdempotentComposite(ParseNode input) {
        input.children.head()
    }

    /**
     * True if this node is a composite and contains children of the same composite type.
     *
     * @param input
     * @return
     */
    Boolean containsCollapsibleComposites(ParseNode input) {
        // Input is a composite and contains children of the same composite type
        input.type in COMPOSITES && input.children.findAll { it.type == input.type }
    }

    ParseNode collapseComposite(ParseNode input) {
        List<ParseNode> redundantKids = input.children.findAll { it.type == input.type }
        List<ParseNode> grandKids = redundantKids.collectMany { it.children }
        grandKids.each { input.addChild(it) }
        input
    }

    /**
     * True if a not contains a not.
     * @param input
     * @return
     */
    Boolean doubleNegationIsPresent(ParseNode input) {
        input.type == ParseNodeType.NOT &&
                input.children &&
                input.children.head() &&
                input.children.head().type == ParseNodeType.NOT
    }

    ParseNode collapseDoubleNegation(ParseNode input) {
        input.children.head().children ? input.children.head().children.head() : null
    }

    /**
     * True for composites containing fewer than two children.
     * @param input
     * @return
     */
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
            ParseNode elementOne = input.children[0].children[0] // first negation's child
            ParseNode elementTwo = input.children[1].children[0] // second negation's child

            result = new ParseNode(ParseNodeType.NOT).addChildren(
                    new ParseNode(COMPOSITE_FLIP.get(input.type)).addChildren(elementOne, elementTwo)
            )
        } else { // Negation of composite with two -> Opposite composite of two negations
            ParseNode composite = input.children[0]
            ParseNode elementOne = composite.children[0] // negation's child's first element
            ParseNode elementTwo = composite.children[1] // negation's child's second element

            result = new ParseNode(COMPOSITE_FLIP.get(composite.type)).addChildren(
                    new ParseNode(ParseNodeType.NOT).addChildren(elementOne),
                    new ParseNode(ParseNodeType.NOT).addChildren(elementTwo)
            )
        }
        result
    }
}
