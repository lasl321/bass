package com.stanson.bass

import com.stanson.parsing.BasicNode
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log4j
import groovy.transform.CompileStatic

/**
 * Attempts to simplify provided boolean logic structures.
 *
 * Created by rdahlgren on 12/23/16.
 */
@Log4j
@CompileStatic
class BooleanAlgebraSolverService<T extends TreeLike> {


    static final List<NodeType> CONSTANT_BOOL = [
            NodeType.TRUE, NodeType.FALSE
    ]

    static final Map<NodeType, NodeType> CONSTANT_BOOL_FLIP = [
            (NodeType.TRUE): NodeType.FALSE,
            (NodeType.FALSE): NodeType.TRUE,
    ]

    static final List<NodeType> COMPOSITES = [
            NodeType.ANY, NodeType.ALL
    ]

    static final Map<NodeType, NodeType> COMPOSITE_FLIP = [
            (NodeType.ANY): NodeType.ALL,
            (NodeType.ALL): NodeType.ANY
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
            new Tuple('Composite complement', this.&containsComplement, this.&simplifyComplement),
            new Tuple('Basic complement', this.&containsBasicComplement, this.&simplifyBasicComplement),
            new Tuple('Composite with constant', this.&isCompositeWithConstant, this.&simplifyCompositeWithConstant),
    ]

    TreeLikeFactory<T> factory

    Integer lookAhead = 1
    /**
     * Given the root of an expression tree, attempt to return the simplest equivalent representation.
     *
     * @param input
     * @return
     */
    T solve(T input) {
        // I wrap the input tree in a superfluous node here so that I don't need to do any weird gymnastics to have
        // a proper 'parent handle' when replacing children below. Consider the case when a transform should be applied
        // to the supplied input root - I would need some special case handling to detect the parentless node and update
        // the 'result' reference rather than keeping the logic uniform.

        TransformedTree<T> workingTree = new TransformedTree(root: factory.withType(NodeType.NULL).addChild(input))

        Integer expressionCount = countExpressions(workingTree.root)
        Integer treeDepth = calculateTreeDepth(workingTree.root)


        List<TransformedTree<T>> transformedTrees = []
        Boolean progressMade = true
        while (progressMade) {
            log.debug('Working on this case: ' + prettyPrint(workingTree.root))
            generatePermutations(transformedTrees, lookAhead, 1, workingTree)

            if (transformedTrees.size() == 1) {
                workingTree = transformedTrees.get(0)
            } else {
                workingTree = transformedTrees.min { calculateTreeDepth(it.root) + countExpressions(it.root) }
            }

            log.debug('Selected this permutation: ' + prettyPrint(workingTree.root))
            log.debug('Ancestry of this permutation is:')
            workingTree.ancestors.each { String transformName, T oldRoot, T newRoot ->
                log.debug("Applying ${transformName} to ${prettyPrint(oldRoot)} yielding ${prettyPrint(newRoot)}")
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
    void generatePermutations(
            List<TransformedTree<T>> result, Integer depth, Integer currentDepth,
            TransformedTree<T> parentTree
    ) {
        // add an unmodified version in the event that the input is the best choice
        result.add(new TransformedTree(
                root: parentTree.root,
                ancestors: [
                        new Tuple('identity',
                                factory.fromPrototypeSubtree(parentTree.root),
                                factory.fromPrototypeSubtree(parentTree.root)
                        )
                ]
        ))
        Closure visitor = { T p ->
            // For each possible transform, see if it applies to this node. If so, transform
            // the corresponding node on a copy tree and add the result to the permutation list
            canApplyTransform.each { Tuple t ->
                String name = (String) t.get(0)
                Closure<Boolean> check = (Closure<Boolean>) t.get(1)
                Closure<T> transform = (Closure<T>) t.get(2)
                if (check(p)) {
                    T ancestorTree = factory.fromPrototypeSubtree(parentTree.root)

                    TransformedTree<T> transformedTree = new TransformedTree(
                            ancestors: parentTree.ancestors.collect(),
                            root: createTransformedTree(
                                    factory.fromPrototypeSubtree(parentTree.root), p, transform
                            )
                    )
                    T currentTree = factory.fromPrototypeSubtree(transformedTree.root)
                    transformedTree.ancestors.add(new Tuple(name, ancestorTree, currentTree))
                    result.add(transformedTree)
                    if (currentDepth < depth) { // recurse if desired
                        // traverse from the root of the transformed tree
                        generatePermutations(result, depth, currentDepth + 1, transformedTree)
                    }
                }
            }
        }
        depthFirstTraversal(parentTree.root, visitor)
    }

    /**
     * Helper method to generate a tree copy that transforms the target node when encountered.
     *
     * @param root subtree input
     * @param target the node being targeted for the provided transform
     * @param transform logic to apply the specified transformation
     * @return updated tree with the specified target having been transformed
     */
    T createTransformedTree(T root, T target, Closure<T> transform) {
        T result = factory.fromPrototype(root)

        List<T> children = root.children.collect()

        List<T> transformedChildren = children.collect { T child ->
            createTransformedTree(child, target, transform)
        }.findAll { it != null }

        result.addChildren(transformedChildren)

        if (root == target) {
            // assume the transform handles child updates
            result = (T) transform(result)
        }

        result
    }

    /**
     * Counts the number of nodes in the tree. Used as a measure of complexity.
     *
     * @param tree
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Integer countExpressions(T tree) {
        (tree.children.collect { countExpressions(it) }).inject(1) { Integer sum, Integer x -> sum + x }
    }

    /**
     * Determines how deep a tree goes.
     *
     * @param tree
     * @return
     */
    Integer calculateTreeDepth(T tree) {
        depthCalculatorRecursion(tree, 0)
    }

    /**
     * Recursive helper.
     *
     * @param node
     * @param parentDepth
     * @return
     */
    private Integer depthCalculatorRecursion(T node, Integer parentDepth) {
        if (node.type == NodeType.PREDICATE) {
            return 1 + parentDepth
        } else {
            List<Integer> childPathDepths = node.children.collect {
                depthCalculatorRecursion(it, parentDepth + 1)
            }
            return childPathDepths.max() ?: 0
        }
    }

    /**
     * True if this input is a composite containing a composite of the opposite type
     * containing another element.
     *
     * @param input
     * @return
     */
    Boolean canAbsorbComposite(T input) {
        if (input.type in COMPOSITES && input.children.size() >= 2) {
            List<T> oppositeComposites = input.children.findAll { T child ->
                child.type == COMPOSITE_FLIP.get(input.type) && child.children
            }
            Set<T> otherChildren = input.children.findAll { !(it in oppositeComposites) } as Set<T>

            return oppositeComposites.any { T opposite -> otherChildren.intersect(opposite.children as Set<T>) }
        }
        return false
    }

    T absorbComposite(T input) {
        List<T> oppositeComposites = input.children.findAll { T child ->
            child.type == COMPOSITE_FLIP.get(input.type) && child.children
        }

        Set<T> otherChildren = input.children.findAll { !(it in oppositeComposites) } as Set<T>
        oppositeComposites.each { T opposite ->
            if (otherChildren.intersect(opposite.children as Set<T>)) {
                input.removeChild(opposite)
            }
        }
        input
    }

    /**
     * True if this node is a composite containing at least one two children and at least one of them is a composite of
     * the opposite type
     * @param input
     * @return
     */
    Boolean canDistributeTerm(T input) {
        input.type in COMPOSITES && input.children.size() > 1 &&
                input.children.find { T child -> child.type == COMPOSITE_FLIP.get(input.type) }
    }

    T distributeTerm(T input) {
        // Select an opposite composite child to distribute
        List<T> inputChildren = input.children
        // just pull the first opposite composite child
        T oppositeCompositeChild = inputChildren.find { T child -> child.type == COMPOSITE_FLIP.get(input.type) }

        List<T> otherChildren = inputChildren.collect() - oppositeCompositeChild // A

        List<T> childrenOfOpposite = oppositeCompositeChild.children.collect() // B, C

        T result = factory.withType(COMPOSITE_FLIP.get(input.type))

        List<T> newTerms = childrenOfOpposite.collect { T oppositeChild ->
            T newParent = factory.withType(input.type)
            newParent.addChild(oppositeChild) // intentionally breaks parent link
            otherChildren.each { T otherChild ->
                // add a copy of these in to prevent breaking the existing parent links
                newParent.addChild(factory.fromPrototypeSubtree(otherChild))
            }
            newParent
        }

        newTerms.each { result.addChild(it) }

        result
    }

    /**
     * True if this node is a composite and more than one child of this node is of the opposite composite type
     * and those qualifying opposite-type children share at least one common term.
     * @param input
     * @return
     */
    Boolean canExtractCommonTerm(T input) {
        if (input.type in COMPOSITES) {
            List<T> oppositeCompositeChildren = input.children.findAll { T child ->
                child.type == COMPOSITE_FLIP.get(input.type) }

            if (oppositeCompositeChildren.size() > 1) {
                // Get the children of each opposite kid, then check for a non-empty intersection between them
                List<List<T>> oppositeKidsChildren = oppositeCompositeChildren.collect { T ok -> ok.children.collect() }

                List<T> commonTerms = oppositeKidsChildren[0]
                for (int i = 1; i < oppositeKidsChildren.size(); ++i) {
                    commonTerms.retainAll(oppositeKidsChildren[i])
                }
                return !commonTerms.isEmpty()
            }
        }
        false
    }

    /**
     * Extracts a common term from a node identified as qualifying for this transform.
     * @param input
     * @return
     */
    T extractCommonTerm(T input) {
        // Remove everything that's being pushed down a level
        List<T> oppositeComposites = input.children.findAll { T child -> child.type == COMPOSITE_FLIP.get(input.type) }
        oppositeComposites.each { input.removeChild(it) }

        List<List<T>> setOfChildren = oppositeComposites.collect { it.children.collect() } // don't modify the input children
        List<T> commonTerms = setOfChildren[0]
        for (int i = 1; i < setOfChildren.size(); ++i) {
            commonTerms.retainAll(setOfChildren[i])
        }
        // Get the common term (just take the first)
        T commonTerm = commonTerms.head()
        // Make the new composites
        T newOpposite = factory.withType(COMPOSITE_FLIP.get(input.type))

        T newSame = factory.withType(input.type)

        // Now add the opposite composite grandkids to the 'newSame' collection
        oppositeComposites.each {
            it.removeChild(commonTerm)
            // if this is a degenerate composite, just add the one remaining child
            if (it.children.size() == 1) {
                T firstChild = it.children[0]
                newSame.addChild(firstChild)
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
    Boolean isIdempotentComposite(T input) {
        if (input.type in COMPOSITES) {
            Integer uniqueChildCount = (input.children as Set).size()
            return uniqueChildCount != input.children.size()
        }
        return false
    }

    T collapseIdempotentComposite(T input) {
        // Remove all children, add them back in via a set
        List<T> children = input.children
        children.each { input.removeChild(it) }

        (children as Set<T>).each { input.addChild(it) }
        input
    }

    /**
     * True if this node is a composite and contains children of the same composite type.
     *
     * @param input
     * @return
     */
    Boolean containsCollapsibleComposites(T input) {
        // Input is a composite and contains children of the same composite type
        input.type in COMPOSITES && input.children.findAll { T child -> child.type == input.type }
    }

    T collapseComposite(T input) {
        List<T> redundantKids = input.children.findAll { T child -> child.type == input.type }
        redundantKids.each { input.removeChild(it) }
        List<T> grandKids = redundantKids.collectMany { it.children }
        grandKids.each { input.addChild(it) }
        input
    }

    /**
     * True if a not contains a not.
     * @param input
     * @return
     */
    Boolean doubleNegationIsPresent(T input) {
        input.type == NodeType.NOT &&
                input.children &&
                input.children.head() &&
                input.children.head().type == NodeType.NOT
    }

    T collapseDoubleNegation(T input) {
        input.children.head().children ? input.children.head().children.head() : null
    }

    /**
     * True for composites containing fewer than two children.
     * @param input
     * @return
     */
    Boolean isDegenerateComposite(T input) {
        input.type in COMPOSITES && input.children.size() < 2
    }


    T collapseDegenerateComposite(T input) {
        input.children ? input.children.head() : null
    }

    /**
     * Returns true if the subtree rooted at the provided T appears to be a case in which
     * DeMorgan's Law can be applied.
     *
     * @param input
     * @return
     */
    Boolean doesDeMorgansLawApply(T input) {
        // If the input is a negation with a composite containing two elements
        Boolean caseOne = input.type == NodeType.NOT &&
                input.children &&
                input.children.head().type in COMPOSITES &&
                input.children.head().children.size() == 2
        // or if the input is a composite with two negated elements
        Boolean caseTwo = input.type in COMPOSITES &&
                input.children.size() == 2 &&
                input.children.every { T child -> child.type == NodeType.NOT }
        caseOne || caseTwo
    }

    /**
     * Transforms the provided input according to DeMorgan's Law.
     * @param input
     * @return
     */
    T applyDeMorgansLaw(T input) {
        T result
        // We can assume DeMorgan's does apply here, so the case determination is simple
        if (input.type in COMPOSITES) { // Composite of two negated elements -> negation of opposite composite with two
            T elementOne = input.children[0].children[0] // first negation's child
            T elementTwo = input.children[1].children[0] // second negation's child

            result = factory.withType(NodeType.NOT).addChild(
                    factory.withType(COMPOSITE_FLIP.get(input.type)).addChildren([elementOne, elementTwo])
            )
        } else { // Negation of composite with two -> Opposite composite of two negations
            T composite = input.children[0]
            T elementOne = composite.children[0] // negation's child's first element
            T elementTwo = composite.children[1] // negation's child's second element

            result = factory.withType(COMPOSITE_FLIP.get(composite.type)).addChildren([
                    factory.withType(NodeType.NOT).addChild(elementOne),
                    factory.withType(NodeType.NOT).addChild(elementTwo)
            ])
        }
        result
    }

    Boolean containsBasicComplement(T input) {
        List<T> nots = input.children.findAll { T child -> child.type == NodeType.NOT }

        nots.any { it.children && it.children.head().type in CONSTANT_BOOL }
    }

    T simplifyBasicComplement(T input) {
        List<T> readyForAbsorption = input.children.findAll { T child ->
            child.type == NodeType.NOT && child.children && child.children.head().type in CONSTANT_BOOL
        }
        readyForAbsorption.each {
            input.removeChild(it)
            input.addChild(
                    factory.withType(CONSTANT_BOOL_FLIP.get(it.children.head().type))
            )
        }

        input
    }

    Boolean containsComplement(T input) {
        if (input.type in COMPOSITES) {
            Map<Boolean, List<T>> havesAndHaveNots = input.children.groupBy { T child -> child.type == NodeType.NOT }
            Set<T> negatedChildren = havesAndHaveNots.get(true, []).collectMany { T p -> p.children } as Set
            Set<T> otherChildren = havesAndHaveNots.get(false, []) as Set

            if (negatedChildren.intersect(otherChildren)) { return true }
        }
        false
    }

    T simplifyComplement(T input) {
        input.children.each { input.removeChild(it) }
        // If we get here, it is known that the complement case exists. We just need to simplify it now
        if (input.type == NodeType.ANY) {
            input.addChild(factory.withType(NodeType.TRUE))
        } else {
            input.addChild(factory.withType(NodeType.FALSE))
        }
    }

    Boolean isCompositeWithConstant(T input) {
        (input.type == NodeType.ANY && input.children.find { T child -> child.type == NodeType.TRUE }) ||
                (input.type == NodeType.ALL && input.children.find { T child -> child.type == NodeType.FALSE })
    }

    T simplifyCompositeWithConstant(T input) {
        input.type == NodeType.ANY ? factory.withType(NodeType.TRUE) : factory.withType(NodeType.FALSE)
    }

    static void depthFirstTraversal(T root, Closure visitor) {
        root.children.each { child -> depthFirstTraversal(child, visitor) }
        visitor.call(root)
    }

    String prettyPrint(T input) {
        Closure printer
        printer = { T node ->
            // process myself, then each of my children
            String representation = 'wtf'
            if (node.type == NodeType.PREDICATE) {
                representation = ((String)((BasicNode)node).data)[0]
            } else if (node.type == NodeType.FALSE) {
                representation = 'F'
            } else if (node.type == NodeType.TRUE) {
                representation = 'T'
            } else if (node.type == NodeType.NULL) {
                representation = 'X'
            } else if (node.type == NodeType.ANY) {
                representation = ' + '
            } else if (node.type == NodeType.ALL) {
                representation = ' * '
            } else if (node.type == NodeType.NOT) {
                representation = ' ¬'
            }
            String result = ''
            if (node.type == NodeType.NOT) {
                result += (representation)
            }
            if (node.type in [NodeType.ANY, NodeType.ALL, NodeType.NOT]) {
                result += ('(')
            } else if (node.type in [NodeType.NULL, NodeType.PREDICATE, NodeType.TRUE, NodeType.FALSE]) {
                result += ("$representation")
            }

            List<String> childRepresentations = node.children.collect { printer(it) }
            result += (childRepresentations.join(representation))
            if (node.type in [NodeType.ANY, NodeType.ALL, NodeType.NOT]) {
                result += (')')
            }
            return result
        }
        printer(input)
    }
}
