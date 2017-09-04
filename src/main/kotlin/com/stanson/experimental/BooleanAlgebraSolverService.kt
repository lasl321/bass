package com.stanson.experimental

import com.stanson.experimental.parsing.BasicNode

class BooleanAlgebraSolverService<T>(private val factory: TreeLikeFactory<T>) where T : TreeLike<T> {
    companion object {
        val CONSTANT_BOOL = listOf(NodeType.TRUE, NodeType.FALSE)
        val CONSTANT_BOOL_FLIP = mapOf(NodeType.TRUE to NodeType.FALSE, NodeType.FALSE to NodeType.TRUE)
        val COMPOSITES = listOf(NodeType.ANY, NodeType.ALL)
        val COMPOSITE_FLIP = mapOf(NodeType.ANY to NodeType.ALL, NodeType.ALL to NodeType.ANY)
    }

    private val canApplyTransform = listOf<TransformItem<T>>(
            TransformItem("DeMorgan's Law", this::doesDeMorgansLawApply, this::applyDeMorgansLaw),
            TransformItem("Degenerate Composite", this::isDegenerateComposite, this::collapseDegenerateComposite),
            TransformItem("Double Negative", this::doubleNegationIsPresent, this::collapseDoubleNegation),
            TransformItem("Idempotent Composite", this::isIdempotentComposite, this::collapseIdempotentComposite),
            TransformItem("Collapsible Composite", this::containsCollapsibleComposites, this::collapseComposite),
            TransformItem("Common Term Extraction", this::canExtractCommonTerm, this::extractCommonTerm),
            TransformItem("Term Distribution", this::canDistributeTerm, this::distributeTerm),
            TransformItem("Absorption 1 or 2", this::canAbsorbComposite, this::absorbComposite),
            TransformItem("Composite complement", this::containsComplement, this::simplifyComplement),
            TransformItem("Basic complement", this::containsBasicComplement, this::simplifyBasicComplement),
            TransformItem("Composite with constant", this::isCompositeWithConstant, this::simplifyCompositeWithConstant)
    )

    fun solve(input: T, lookAhead: Int = 1): T {
        var workingTree = TransformedTree(factory.withType(NodeType.NULL).addChild(input), mutableListOf())

        var expressionCount = countExpressions(workingTree.root)
        var treeDepth = calculateTreeDepth(workingTree.root)


        val transformedTrees = mutableListOf<TransformedTree<T>>()
        var progressMade = true
        while (progressMade) {
            generatePermutations(transformedTrees, lookAhead, 1, workingTree)

            workingTree = if (transformedTrees.size == 1) {
                transformedTrees[0]
            } else {
                transformedTrees.minBy { calculateTreeDepth(it.root) + countExpressions(it.root) }!!
            }

            val newDepth = calculateTreeDepth(workingTree.root)
            val newExpressionCount = countExpressions(workingTree.root)
            progressMade = (newDepth < treeDepth) || (newExpressionCount < expressionCount)
            treeDepth = newDepth
            expressionCount = newExpressionCount
            transformedTrees.clear()
        }

        return workingTree.root.children.first()
    }

    private fun generatePermutations(result: MutableList<TransformedTree<T>>, depth: Int, currentDepth: Int, parentTree: TransformedTree<T>) {
        result.add(TransformedTree(
                parentTree.root,
                mutableListOf(Triple("identity", factory.fromPrototypeSubTree(parentTree.root), factory.fromPrototypeSubTree(parentTree.root)))))

        fun visitor(p: T) {
            canApplyTransform.forEach {
                val (name, check, transform) = it

                if (check(p)) {
                    val ancestorTree = factory.fromPrototypeSubTree(parentTree.root)

                    val transformedTree = TransformedTree(
                            createTransformedTree(factory.fromPrototypeSubTree(parentTree.root), p, transform)!!,
                            parentTree.ancestors)

                    val currentTree = factory.fromPrototypeSubTree(transformedTree.root)
                    transformedTree.ancestors.add(Triple(name, ancestorTree, currentTree))
                    result.add(transformedTree)

                    if (currentDepth < depth) {
                        generatePermutations(result, depth, currentDepth + 1, transformedTree)
                    }
                }
            }
        }

        depthFirstTraversal(parentTree.root, ::visitor)
    }

    private fun createTransformedTree(root: T, target: T, transform: (T) -> T?): T? {
        val result = factory.fromPrototype(root)

        val transformedChildren = root.children.map {
            createTransformedTree(it, target, transform)
        }

        result.addChildren(transformedChildren.filterNotNull())

        return if (root == target) {
            transform(result)
        } else {
            result
        }
    }

    private fun countExpressions(tree: T): Int {
        return tree.children.map { countExpressions(it) }.fold(1) { total, value -> total + value }
    }

    private fun calculateTreeDepth(tree: T): Int {
        return depthCalculatorRecursion(tree, 0)
    }

    private fun depthCalculatorRecursion(node: T, parentDepth: Int): Int {
        return if (node.nodeType == NodeType.PREDICATE) {
            1 + parentDepth
        } else {
            val childPathDepths = node.children.map {
                depthCalculatorRecursion(it, parentDepth + 1)
            }
            childPathDepths.max() ?: 0
        }
    }

    private fun canAbsorbComposite(input: T): Boolean {
        if (input.nodeType in COMPOSITES && input.children.size >= 2) {
            val oppositeComposites = input.children.filter {
                it.nodeType == COMPOSITE_FLIP[input.nodeType] && it.children.isNotEmpty()
            }
            val otherChildren = input.children.filter { it !in oppositeComposites }.toSet()

            return oppositeComposites.any { otherChildren.intersect(it.children.toSet()).isNotEmpty() }
        }
        return false
    }

    private fun absorbComposite(input: T): T {
        val oppositeComposites = input.children.filter {
            it.nodeType == COMPOSITE_FLIP[input.nodeType] && it.children.isNotEmpty()
        }

        val otherChildren = input.children.filter { it !in oppositeComposites }.toSet()
        oppositeComposites.forEach {
            if (otherChildren.intersect(it.children.toSet()).isNotEmpty()) {
                input.removeChild(it)
            }
        }
        return input
    }

    private fun canDistributeTerm(input: T): Boolean =
            input.nodeType in COMPOSITES && input.children.any { it.nodeType == COMPOSITE_FLIP[input.nodeType] }

    private fun distributeTerm(input: T): T {
        val inputChildren = input.children
        val oppositeCompositeChild = inputChildren.find { it.nodeType == COMPOSITE_FLIP[input.nodeType] }!!
        val otherChildren = inputChildren - oppositeCompositeChild
        val childrenOfOpposite = oppositeCompositeChild.children.toList()

        val result = factory.withType(COMPOSITE_FLIP[input.nodeType]!!)

        return result.apply {
            addChildren(childrenOfOpposite.map { childOfOpposite ->
                factory.withType(input.nodeType).apply {
                    addChild(childOfOpposite)
                    addChildren(otherChildren.map(factory::fromPrototypeSubTree))
                }
            })
        }
    }

    private fun canExtractCommonTerm(input: T): Boolean {
        if (input.nodeType in COMPOSITES) {
            val oppositeCompositeChildren = input.children.filter {
                it.nodeType == COMPOSITE_FLIP[input.nodeType]
            }

            if (oppositeCompositeChildren.size > 1) {
                val oppositeKidsChildren = oppositeCompositeChildren.map { it.children }

                val commonTerms = mutableListOf<T>()
                commonTerms.addAll(oppositeKidsChildren[0])
                for (i in 1 until oppositeKidsChildren.size) {
                    commonTerms.retainAll(oppositeKidsChildren[i])
                }
                return !commonTerms.isEmpty()
            }
        }
        return false
    }

    private fun extractCommonTerm(input: T): T {
        // Remove everything that's being pushed down a level
        val oppositeComposites = input.children.filter { it.nodeType == COMPOSITE_FLIP[input.nodeType] }
        oppositeComposites.forEach { input.removeChild(it) }

        val setOfChildren: List<List<T>> = oppositeComposites.map { it.children }
        val commonTerms = mutableListOf<T>()
        commonTerms.addAll(setOfChildren[0])

        for (i in 1 until setOfChildren.size) {
            commonTerms.retainAll(setOfChildren[i])
        }

        // Get the common term (just take the first)
        val commonTerm = commonTerms.first()
        // Make the new composites
        val newOpposite = factory.withType(COMPOSITE_FLIP[input.nodeType]!!)

        val newSame = factory.withType(input.nodeType)

        // Now add the opposite composite grandkids to the 'newSame' collection
        oppositeComposites.forEach {
            it.removeChild(commonTerm)
            // if this is a degenerate composite, just add the one remaining child
            if (it.children.size == 1) {
                val firstChild = it.children[0]
                newSame.addChild(firstChild)
            } else if (it.children.size > 1) {
                newSame.addChild(it)
            } // otherwise it's empty and should be ignored.
        }

        newOpposite.addChild(commonTerm) // common term goes here
        newOpposite.addChild(newSame) // all the kids added go under the new opposite

        // This is added to the input
        input.addChild(newOpposite)
        return input
    }

    private fun isIdempotentComposite(input: T): Boolean {
        if (input.nodeType in COMPOSITES) {
            return input.children.toSet().size != input.children.size
        }
        return false
    }

    private fun collapseIdempotentComposite(input: T): T {
        val children = input.children.toList()
        children.forEach { input.removeChild(it) }

        children.toSet().forEach { input.addChild(it) }
        return input
    }

    private fun containsCollapsibleComposites(input: T): Boolean {
        return input.nodeType in COMPOSITES && input.children.any { it.nodeType == input.nodeType }
    }

    private fun collapseComposite(input: T): T {
        val redundantKids = input.children.filter { it.nodeType == input.nodeType }
        redundantKids.forEach { input.removeChild(it) }

        val grandKids = redundantKids.flatMap { it.children }
        grandKids.forEach { input.addChild(it) }

        return input
    }

    private fun doubleNegationIsPresent(input: T): Boolean {
        return input.nodeType == NodeType.NOT &&
                input.children.isNotEmpty() &&
                input.children.first().nodeType == NodeType.NOT
    }

    private fun collapseDoubleNegation(input: T): T? {
        return if (input.children.first().children.isNotEmpty()) {
            input.children.first().children.first()
        } else {
            null
        }
    }

    private fun isDegenerateComposite(input: T): Boolean {
        return input.nodeType in COMPOSITES && input.children.size < 2
    }


    private fun collapseDegenerateComposite(input: T): T? {
        return if (input.children.isNotEmpty()) {
            input.children.first()
        } else {
            null
        }
    }

    private fun doesDeMorgansLawApply(input: T): Boolean {
        val caseOne = input.nodeType == NodeType.NOT &&
                input.children.first().nodeType in COMPOSITES &&
                input.children.first().children.size == 2

        val caseTwo = input.nodeType in COMPOSITES &&
                input.children.size == 2 &&
                input.children.all { it.nodeType == NodeType.NOT }

        return caseOne || caseTwo
    }

    private fun applyDeMorgansLaw(input: T): T {
        return if (input.nodeType in COMPOSITES) {
            val elementOne = input.children[0].children[0]
            val elementTwo = input.children[1].children[0]

            factory.withType(NodeType.NOT).addChild(
                    factory.withType(COMPOSITE_FLIP[input.nodeType]!!).addChildren(listOf(elementOne, elementTwo))
            )
        } else {
            val composite = input.children[0]
            val elementOne = composite.children[0]
            val elementTwo = composite.children[1]

            factory.withType(COMPOSITE_FLIP[composite.nodeType]!!).addChildren(listOf(
                    factory.withType(NodeType.NOT).addChild(elementOne),
                    factory.withType(NodeType.NOT).addChild(elementTwo)
            ))
        }
    }

    private fun containsBasicComplement(input: T): Boolean {
        val nots = input.children.filter { it.nodeType == NodeType.NOT }

        return nots.any { it.children.isNotEmpty() && it.children.first().nodeType in CONSTANT_BOOL }
    }

    private fun simplifyBasicComplement(input: T): T {
        val readyForAbsorption: List<T> = input.children.filter {
            it.nodeType == NodeType.NOT && it.children.isNotEmpty() && it.children.first().nodeType in CONSTANT_BOOL
        }

        readyForAbsorption.forEach {
            input.removeChild(it)
            input.addChild(factory.withType(CONSTANT_BOOL_FLIP[it.children.first().nodeType]!!))
        }

        return input
    }

    private fun containsComplement(input: T): Boolean {
        if (input.nodeType in COMPOSITES) {
            val havesAndHaveNot: Map<Boolean, List<T>> = input.children.groupBy { it.nodeType == NodeType.NOT }
            val negatedChildren = havesAndHaveNot.getOrDefault(true, listOf()).flatMap { p -> p.children }.toSet()
            val otherChildren = havesAndHaveNot.getOrDefault(false, listOf()).toSet()

            if (negatedChildren.intersect(otherChildren).isNotEmpty()) {
                return true
            }
        }

        return false
    }

    private fun simplifyComplement(input: T): T {
        input.children.toList().forEach(input::removeChild)

        return if (input.nodeType == NodeType.ANY) {
            input.addChild(factory.withType(NodeType.TRUE))
        } else {
            input.addChild(factory.withType(NodeType.FALSE))
        }
    }

    private fun isCompositeWithConstant(input: T): Boolean {
        return (input.nodeType == NodeType.ANY && input.children.any { it.nodeType == NodeType.TRUE }) ||
                (input.nodeType == NodeType.ALL && input.children.any { it.nodeType == NodeType.FALSE })
    }

    private fun simplifyCompositeWithConstant(input: T): T {
        return if (input.nodeType == NodeType.ANY) {
            factory.withType(NodeType.TRUE)
        } else {
            factory.withType(NodeType.FALSE)
        }
    }

    private fun depthFirstTraversal(root: T, visitor: (T) -> Unit) {
        root.children.forEach { depthFirstTraversal(it, visitor) }
        visitor(root)
    }

    fun prettyPrint(input: T): String {
        fun printer(node: T): String {
            val representation = when (node.nodeType) {
                NodeType.PREDICATE -> ((node as BasicNode).data as String)[0].toString()
                NodeType.FALSE -> "F"
                NodeType.TRUE -> "T"
                NodeType.NULL -> "X"
                NodeType.ANY -> " + "
                NodeType.ALL -> " * "
                NodeType.NOT -> " ¬"
            }

            val prefix = when (node.nodeType) {
                NodeType.ANY, NodeType.ALL -> "("
                NodeType.NOT -> "$representation("
                NodeType.NULL, NodeType.PREDICATE, NodeType.TRUE, NodeType.FALSE -> representation
            }

            val childRepresentations = node.children.joinToString(representation) { printer(it) }

            return when (node.nodeType) {
                NodeType.ANY, NodeType.ALL, NodeType.NOT -> prefix + childRepresentations + ")"
                else -> prefix + childRepresentations
            }
        }

        return printer(input)
    }
}
