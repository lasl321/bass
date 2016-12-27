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
    private static final class TransformedTree {
        // transform name applied to the root yields the next ancestor (or this tree)
        List<Tuple2<String,ParseNode>> ancestors = []
        ParseNode root
    }
    static final List<ParseNodeType> COMPOSITES = [
            ParseNodeType.ANY, ParseNodeType.ALL
    ]

    static final Map<ParseNodeType, ParseNodeType> COMPOSITE_FLIP = [
            (ParseNodeType.ANY): ParseNodeType.ALL,
            (ParseNodeType.ALL): ParseNodeType.ANY
    ]

    private final List<Tuple> canApplyTransform = [
            new Tuple('DeMorgan\'s Law', this.&doesDeMorgansLawApply, this.&applyDeMorgansLaw),
            new Tuple('Degenerate Composite', this.&isDegenerateComposite, this.&collapseDegenerateComposite),
            new Tuple('Double Negative', this.&doubleNegationIsPresent, this.&collapseDoubleNegation),
            new Tuple('Idempotent Composite', this.&isIdempotentComposite, this.&collapseIdempotentComposite),
            new Tuple('Collapsible Composite', this.&containsCollapsibleComposites, this.&collapseComposite),
            new Tuple('Common Term Extraction', this.&canExtractCommonTerm, this.&extractCommonTerm),
            new Tuple('Term Distribution', this.&canDistributeTerm, this.&distributeTerm),
            new Tuple('Absorption 1 or 2', this.&canAbsorbComposite, this.&absorbComposite),
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

        TransformedTree workingTree = new TransformedTree(
                root: new ParseNode(ParseNodeType.NULL).addChildren(input),
        )

        Integer expressionCount = countExpressions(workingTree.root)
        Integer treeDepth = calculateTreeDepth(workingTree.root)


        List<TransformedTree> transformedTrees = []
        Boolean progressMade = true
        while (progressMade) {
            log.debug('Working on ' + BassDriver.prettyPrint(workingTree.root))
            generatePermutations(transformedTrees, lookAhead, 1, workingTree)
            if (transformedTrees.size() == 1) {
                workingTree = transformedTrees.get(0)
            } else {
                workingTree = transformedTrees.min { calculateTreeDepth(it.root) + countExpressions(it.root) }
            }

            log.info('Selected this permutation: ' + BassDriver.prettyPrint(workingTree.root))
            log.debug('Ancestry of this permutation is:')
            workingTree.ancestors.each { String transformName, ParseNode oldRoot ->
                log.debug("Applied ${transformName} to ${BassDriver.prettyPrint(oldRoot)}")
            }
            Integer newDepth = calculateTreeDepth(workingTree.root)
            Integer newExpressionCount = countExpressions(workingTree.root)
            progressMade = (newDepth < treeDepth) || (newExpressionCount < expressionCount)
            treeDepth = newDepth
            expressionCount = newExpressionCount
            transformedTrees.clear()
        }
        workingTree.root.children.head()
    }

    /**
     * Descend up to `depth` creating all possible permutations of applicable transforms.
     *
     * @param result Stores generated permutations
     * @param depth Desired recursion depth. 2 or 3 should suffice.
     * @param currentDepth Stack-storage of the current depth
     * @param root Root of the tree to generate permutations for
     */
    void generatePermutations(List<TransformedTree> result, Integer depth, Integer currentDepth, TransformedTree parentTree) {
        // add an unmodified version
        result.add(new TransformedTree(
                root: parentTree.root,
                ancestors: [new Tuple2<String, ParseNode>('identity', parentTree.root)]
        ))
        Closure visitor = { ParseNode p ->
            // For each possible transform, see if it applies to this node. If so, transform
            // the corresponding node on a copy tree and add the result to the permutation list
            canApplyTransform.each { Tuple t ->
                String name = (String) t.get(0)
                Closure<Boolean> check = (Closure<Boolean>) t.get(1)
                Closure<ParseNode> transform = (Closure<ParseNode>) t.get(2)
                if (check(p)) {
                    TransformedTree transformedTree = new TransformedTree(
                            ancestors: parentTree.ancestors.collect(),
                            root: createTransformedTree(parentTree.root, p, transform)
                    )
                    transformedTree.ancestors.add(new Tuple2<String,ParseNode>(name, parentTree.root))
                    result.add(transformedTree)
                    if (currentDepth < depth) { // recurse if desired
                        // traverse from the root of the transformed tree
                        generatePermutations(result, depth, currentDepth + 1, transformedTree)
                    }
                }
            }
        }
        parentTree.root.depthFirstPostTraversal(visitor)
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
     * True if this input is a composite containing two elements, one of which is a composite of the opposite type
     * containing the other element.
     *
     * @param input
     * @return
     */
    Boolean canAbsorbComposite(ParseNode input) {
        if (input.type in COMPOSITES && input.children.size() == 2) {
            ParseNode oppositeComposite = input.children.find {
                it.type == COMPOSITE_FLIP.get(input.type)
            }
            if (oppositeComposite) {
                List<ParseNode> copiedChildren = input.children.collect()
                copiedChildren.removeElement(oppositeComposite)
                ParseNode otherChild = copiedChildren.head()
                if (otherChild in oppositeComposite.children) {
                    return true
                }
            }
        }
        return false
    }

    ParseNode absorbComposite(ParseNode input) {
        ParseNode oppositeComposite = input.children.find {
            it.type == COMPOSITE_FLIP.get(input.type)
        }
        input.removeChild(oppositeComposite)
        input
    }

    /**
     * True if this node is a composite containing at least one two children and at least one of them is a composite of
     * the opposite type
     * @param input
     * @return
     */
    Boolean canDistributeTerm(ParseNode input) {
        input.type in COMPOSITES && input.children.size() > 1 &&
                input.children.find { it.type == COMPOSITE_FLIP.get(input.type) }
    }

    ParseNode distributeTerm(ParseNode input) {
        // Select an opposite composite child to distribute
        ParseNode oppositeCompositeChild = input.children.find { it.type == COMPOSITE_FLIP.get(input.type) }

        List<ParseNode> otherChildren = input.children.collect()
        otherChildren.remove(oppositeCompositeChild)

        List<ParseNode> childrenOfOpposite = oppositeCompositeChild.children

        ParseNode result = new ParseNode(COMPOSITE_FLIP.get(input.type))
        List<ParseNode> newTerms = childrenOfOpposite.collect { ParseNode oppositeChild ->
            ParseNode newParent = new ParseNode(input.type)
            newParent.addChild(oppositeChild) // intentionally breaks parent link
            otherChildren.each {
                newParent.addChild(it.shallowCopy()) // <-- copy is important here, otherwise parent link will break
            }
            newParent
        }
        newTerms.each { result.addChild(it) }

        result
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
        List<ParseNode> oppositeComposites = input.children.findAll { it.type == COMPOSITE_FLIP.get(input.type) }
        oppositeComposites.each { input.removeChild(it) }

        // Get the common term (just take the first)
        ParseNode commonTerm = oppositeComposites.collect {
            it.children as Set<ParseNode>
        }.inject {
            Set<ParseNode> intersection, Set<ParseNode> compositeEntries -> intersection.intersect(compositeEntries)
        }.head()
        // Make the new composites
        ParseNode newOpposite = new ParseNode(COMPOSITE_FLIP.get(input.type))

        ParseNode newSame = new ParseNode(input.type)

        // Now add the opposite composite grandkids to the 'newSame' collection
        oppositeComposites.each {
            it.removeChild(commonTerm)
            // if this is a degenerate composite, just add the one remaining child
            if (it.children.size() == 1) {
                newSame.addChild(it.children.head())
            } else if (it.children.size() > 1) {
                newSame.addChild(it)
            } // otherwise it's empty and should be ignored.
        }

        newOpposite.addChild(commonTerm) // common term goes here
        newOpposite.addChild(newSame) // all the kids added go under the new opposite

        // This is added to the input
        input.addChild(newOpposite)
        input
    }

    /**
     * True if this node is a composite and contains equivalent children.
     * @param input
     * @return
     */
    Boolean isIdempotentComposite(ParseNode input) {
        if (input.type in COMPOSITES) {
            Integer uniqueChildCount = (input.children as Set).size()
            return uniqueChildCount != input.children.size()
        }
        return false
    }

    ParseNode collapseIdempotentComposite(ParseNode input) {
        // Remove all children, add them back in via a set
        List<ParseNode> children = input.children
        children.each { input.removeChild(it) }

        (children as Set<ParseNode>).each { input.addChild(it) }
        input
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
