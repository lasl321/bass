package com.stanson.bass

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
    /**
     * Required to compare nodes.
     */
    private final TreeLikeComparator<T> c

    /**
     * Used to make instances.
     */
    private final TreeLikeFactory<T> f


    private static final class TransformedTree {
        // transform name applied to the root yields the next ancestor (or this tree)
        List<Tuple2<String,T>> ancestors = []
        T root
    }

    static final List<TreeLikeType> CONSTANT_BOOL = [
            TreeLikeType.TRUE, TreeLikeType.FALSE
    ]

    static final Map<TreeLikeType, TreeLikeType> CONSTANT_BOOL_FLIP = [
            (TreeLikeType.TRUE): TreeLikeType.FALSE,
            (TreeLikeType.FALSE): TreeLikeType.TRUE,
    ]

    static final List<TreeLikeType> COMPOSITES = [
            TreeLikeType.OR, TreeLikeType.AND
    ]

    static final Map<TreeLikeType, TreeLikeType> COMPOSITE_FLIP = [
            (TreeLikeType.OR): TreeLikeType.AND,
            (TreeLikeType.AND): TreeLikeType.OR
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

    Integer lookAhead = 1

    BooleanAlgebraSolverService(Integer lookAhead, TreeLikeComparator<T> comparator, TreeLikeFactory<T> factory) {
        if (!comparator) { throw new NullPointerException('Comparator must be provided') }
        if (!factory) { throw new NullPointerException('Factory must be provided') }

        this.lookAhead = lookAhead
        this.c = comparator
        this.f = factory
    }

    /**
     * Given a tree, attempt to return the simplest equivalent representation.
     *
     * @param input
     * @return
     */
    T solve(T input) {
        // I wrap the input tree in a superfluous node here so that I don't need to do any weird gymnastics to have
        // a proper 'parent handle' when replacing children below. Consider the case when a transform should be applied
        // to the supplied input root - I would need some special case handling to detect the parentless node and update
        // the 'result' reference rather than keeping the logic uniform.
        T workingTreeRoot = f.newInstance(TreeLikeType.NULL, null).addChildren(input)
//        T workingTree = f.newInstance(TreeLikeType.NULL, null)
//        workingTree.addChildren(input)
        TransformedTree workingTree = new TransformedTree(root: workingTreeRoot)

        Integer expressionCount = countExpressions(workingTree.root)
        Integer treeDepth = calculateTreeDepth(workingTree.root)


        List<TransformedTree> transformedTrees = []
        Boolean progressMade = true
        while (progressMade) {
            generatePermutations(transformedTrees, lookAhead, 1, workingTree)
            if (transformedTrees.size() == 1) {
                workingTree = transformedTrees.get(0)
            } else {
                workingTree = transformedTrees.min { calculateTreeDepth(it.root) + countExpressions(it.root) }
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
            List<TransformedTree> result, Integer depth, Integer currentDepth, TransformedTree parentTree) {
        // add an unmodified version
        result.add(new TransformedTree(
                root: parentTree.root,
                ancestors: [new Tuple2<String, T>('identity', parentTree.root)]
        ))
        Closure visitor = { T node ->
            // For each possible transform, see if it applies to this node. If so, transform
            // the corresponding node on a copy tree and add the result to the permutation list
            canApplyTransform.each { Tuple t ->
                String name = (String) t.get(0)
                Closure<Boolean> check = (Closure<Boolean>) t.get(1)
                Closure<T> transform = (Closure<T>) t.get(2)
                if (check(node)) {
                    TransformedTree transformedTree = new TransformedTree(
                            ancestors: parentTree.ancestors.collect(),
                            root: createTransformedTree(parentTree.root, node, transform)
                    )
                    transformedTree.ancestors.add(new Tuple2<String,T>(name, parentTree.root))
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
    T createTransformedTree(T root, T target, Closure<T> transform) {
        T result = f.fromExistingInstance(root)
        result.children = root.children.collect()
        if (result && result.children) {
            result.children = result.children.collect { T child ->
                createTransformedTree(child, target, transform)
            }.findAll { it != null }
        }

        if (root == target) {
            // assume the transform handles child updates
            result = transform(result)
        }
        result
    }

    /**
     * Counts the number of nodes in the tree. Used as a measure of complexity.
     *
     * @param tree
     * @return
     */
    Integer countExpressions(T tree) {
        Integer count = 0
        Closure counter = { T node ->
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

        if (node.type == TreeLikeType.PREDICATE) {
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

            return oppositeComposites.any { T opposite ->
                otherChildren.intersect(opposite.children as Set<T>)
            }
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
        T oppositeCompositeChild = input.children.find { T child ->
            child.type == COMPOSITE_FLIP.get(input.type) }

        List<T> otherChildren = input.children.collect()
        otherChildren.remove(oppositeCompositeChild)

        List<T> childrenOfOpposite = oppositeCompositeChild.children

        T result = f.newInstance(COMPOSITE_FLIP.get(input.type), null)
        List<T> newTerms = childrenOfOpposite.collect { T oppositeChild ->
            T newParent = f.newInstance(input.type, null) //new T(input.type)
            newParent.addChildren(oppositeChild) // intentionally breaks parent link
            otherChildren.each {
                newParent.addChildren(f.fromExistingInstance(it)) // <-- copy is important here, otherwise parent link will break
            }
            newParent
        }
        result.addChildren(newTerms)

        result
    }

    /**
     * True if this node is a composite and more than one child of this node is of the opposite composite type
     * and those qualifying children share at least one common term.
     * @param input
     * @return
     */
    Boolean canExtractCommonTerm(T input) {
        if (input.type in COMPOSITES) {
            List<T> oppositeKids = input.children.findAll { T child -> child.type == COMPOSITE_FLIP.get(input.type) }
            if (oppositeKids.size() > 1) {
                // Get the children of each opposite kid as a Set, then check for a non-empty intersection between them
                List<Set<T>> oppositeKidsChildren = oppositeKids.collect { it.children as Set<T> }
                Set<T> intersection = oppositeKidsChildren.inject {
                    Set<T> intersection, Set<T> compositeEntries ->
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
    T extractCommonTerm(T input) {
        // Remove everything that's being pushed down a level
        List<T> oppositeComposites = input.children.findAll { T child -> child.type == COMPOSITE_FLIP.get(input.type) }
        oppositeComposites.each { input.removeChild(it) }

        // Get the common term (just take the first)
        T commonTerm = oppositeComposites.collect {
            it.children as Set<T>
        }.inject {
            Set<T> intersection, Set<T> compositeEntries -> intersection.intersect(compositeEntries)
        }.head()
        // Make the new composites
        T newOpposite = f.newInstance(COMPOSITE_FLIP.get(input.type), null)

        T newSame = f.newInstance(input.type, null)

        // Now add the opposite composite grandkids to the 'newSame' collection
        oppositeComposites.each {
            it.removeChild(commonTerm)
            // if this is a degenerate composite, just add the one remaining child
            if (it.children.size() == 1) {
                newSame.addChildren((T)it.children.head())
            } else if (it.children.size() > 1) {
                newSame.addChildren(it)
            } // otherwise it's empty and should be ignored.
        }

        newOpposite.addChildren(commonTerm) // common term goes here
        newOpposite.addChildren(newSame) // all the kids added go under the new opposite

        // This is added to the input
        input.addChildren(newOpposite)
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

        (children as Set<T>).each { input.addChildren(it) }
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
        grandKids.each { input.addChildren(it) }
        input
    }

    /**
     * True if a not contains a not.
     * @param input
     * @return
     */
    Boolean doubleNegationIsPresent(T input) {
        input.type == TreeLikeType.NOT &&
                input.children &&
                input.children.head() &&
                ((T)input.children.head()).type == TreeLikeType.NOT
    }

    T collapseDoubleNegation(T input) {
        ((T)input.children.head()).children ? (T)((T)input.children.head()).children.head() : null
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
        input.children ? ((T)input.children.head()) : null
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
        Boolean caseOne = input.type == TreeLikeType.NOT &&
                input.children &&
                ((T)input.children.head()).type in COMPOSITES &&
                ((T)input.children.head()).children.size() == 2
        // or if the input is a composite with two negated elements
        Boolean caseTwo = input.type in COMPOSITES &&
                input.children.size() == 2 &&
                input.children.every { T child -> child.type == TreeLikeType.NOT }
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
            T elementOne = ((T)input.children[0]).children[0] // first negation's child
            T elementTwo = ((T)input.children[1]).children[0] // second negation's child

            result = f.newInstance(TreeLikeType.NOT, null).addChildren(
                    f.newInstance(COMPOSITE_FLIP.get(input.type), null).addChildren(elementOne, elementTwo))
        } else { // Negation of composite with two -> Opposite composite of two negations
            T composite = (T) input.children[0]
            T elementOne = (T) composite.children[0] // negation's child's first element
            T elementTwo = (T) composite.children[1] // negation's child's second element

            result = f.newInstance(COMPOSITE_FLIP.get(composite.type), null).addChildren(
                    f.newInstance(TreeLikeType.NOT, null).addChildren(elementOne),
                    f.newInstance(TreeLikeType.NOT, null).addChildren(elementTwo),
            )
        }
        (T)result
    }

    Boolean containsBasicComplement(T input) {
        List<T> nots = input.children.findAll { T child -> child.type == TreeLikeType.NOT }

        nots.any { it.children && ((T)it.children.head()).type in CONSTANT_BOOL }
    }

    T simplifyBasicComplement(T input) {
        List<T> readyForAbsorption = input.children.findAll { T child ->
            child.type == TreeLikeType.NOT && child.children && ((T)child.children.head()).type in CONSTANT_BOOL
        }
        readyForAbsorption.each {
            input.removeChild(it)
            input.addChildren(
                    f.newInstance(CONSTANT_BOOL_FLIP.get(((T)it.children.head()).type), null)
            )
        }

        input
    }

    Boolean containsComplement(T input) {
        if (input.type in COMPOSITES) {
            Map<Boolean, List<T>> havesAndHaveNots = input.children.groupBy { T child -> child.type == TreeLikeType.NOT }
            Set<T> negatedChildren = havesAndHaveNots.get(true, []).collectMany { T p -> p.children } as Set
            Set<T> otherChildren = havesAndHaveNots.get(false, []) as Set

            if (negatedChildren.intersect(otherChildren)) { return true }
        }
        false
    }

    T simplifyComplement(T input) {
        input.children.each { input.removeChild(it) }
        // If we get here, it is known that the complement case exists. We just need to simplify it now
        if (input.type == TreeLikeType.OR) {
            input.addChildren(
                    f.newInstance(TreeLikeType.TRUE, null)
            )
        } else {
            input.addChildren(
                    f.newInstance(TreeLikeType.FALSE, null)
            )
        }
    }

    Boolean isCompositeWithConstant(T input) {
        (input.type == TreeLikeType.OR && input.children.find { T child -> child.type == TreeLikeType.TRUE }) ||
                (input.type == TreeLikeType.AND && input.children.find { T child -> child.type == TreeLikeType.FALSE })
    }

    T simplifyCompositeWithConstant(T input) {
        input.type == TreeLikeType.OR ?
                f.newInstance(TreeLikeType.TRUE, null) : f.newInstance(TreeLikeType.FALSE, null)
    }
}
