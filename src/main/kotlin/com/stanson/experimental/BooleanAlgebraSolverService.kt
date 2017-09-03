package com.stanson.experimental

import com.stanson.experimental.parsing.BasicNode

// lasl321 Add log4j logging
class BooleanAlgebraSolverService<T>(
        private val factory: TreeLikeFactory<T>,
        private var lookAhead: Int
) where T : TreeLike<T> {
    companion object {
        val CONSTANT_BOOL: List<NodeType> = listOf(NodeType.TRUE, NodeType.FALSE)
        val CONSTANT_BOOL_FLIP = mapOf(NodeType.TRUE to NodeType.FALSE, NodeType.FALSE to NodeType.TRUE)
        val COMPOSITES: List<NodeType> = listOf(NodeType.ANY, NodeType.ALL)
        val COMPOSITE_FLIP = mapOf(NodeType.ANY to NodeType.ALL, NodeType.ALL to NodeType.ANY)
    }

    private val canApplyTransform = listOf<Triple<String, (T) -> Boolean, (T) -> T?>>(
            Triple("DeMorgan\"s Law", this::doesDeMorgansLawApply, this::applyDeMorgansLaw),
            Triple("Degenerate Composite", this::isDegenerateComposite, this::collapseDegenerateComposite),
            Triple("Double Negative", this::doubleNegationIsPresent, this::collapseDoubleNegation),
            Triple("Idempotent Composite", this::isIdempotentComposite, this::collapseIdempotentComposite),
            Triple("Collapsible Composite", this::containsCollapsibleComposites, this::collapseComposite),
            Triple("Common Term Extraction", this::canExtractCommonTerm, this::extractCommonTerm),
            Triple("Term Distribution", this::canDistributeTerm, this::distributeTerm),
            Triple("Absorption 1 or 2", this::canAbsorbComposite, this::absorbComposite),
            Triple("Composite complement", this::containsComplement, this::simplifyComplement),
            Triple("Basic complement", this::containsBasicComplement, this::simplifyBasicComplement),
            Triple("Composite with constant", this::isCompositeWithConstant, this::simplifyCompositeWithConstant)
    )

    fun solve(input: T): T {
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

        return workingTree.root.getChildren().first()
    }

    private fun generatePermutations(result: MutableList<TransformedTree<T>>, depth: Int, currentDepth: Int, parentTree: TransformedTree<T>) {
        result.add(TransformedTree(
                parentTree.root,
                mutableListOf(Triple("identity", factory.fromPrototypeSubtree(parentTree.root), factory.fromPrototypeSubtree(parentTree.root)))))

        fun visitor(p: T) {
            canApplyTransform.forEach {
                val name = it.first
                val check = it.second
                val transform: (T) -> T? = it.third

                if (check(p)) {
                    val ancestorTree = factory.fromPrototypeSubtree(parentTree.root)

                    val transformedTree = TransformedTree(
                            createTransformedTree(factory.fromPrototypeSubtree(parentTree.root), p, transform),
                            parentTree.ancestors)

                    val currentTree = factory.fromPrototypeSubtree(transformedTree.root)
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

    private fun createTransformedTree(root: T, target: T, transform: (T) -> T?): T {
        val result = factory.fromPrototype(root)

        val children = root.getChildren()

        val transformedChildren = children.map { it ->
            createTransformedTree(it, target, transform)
        }

        result.addChildren(transformedChildren)

        return if (root == target) {
            transform(result)!!
        } else {
            result
        }
    }

    private fun countExpressions(tree: T): Int {
        return tree.getChildren().map { countExpressions(it) }.fold(1) { total, value -> total + value }
    }

    private fun calculateTreeDepth(tree: T): Int {
        return depthCalculatorRecursion(tree, 0)
    }

    private fun depthCalculatorRecursion(node: T, parentDepth: Int): Int {
        return if (node.getNodeType() == NodeType.PREDICATE) {
            1 + parentDepth
        } else {
            val childPathDepths = node.getChildren().map {
                depthCalculatorRecursion(it, parentDepth + 1)
            }
            childPathDepths.max() ?: 0
        }
    }

    private fun canAbsorbComposite(input: T): Boolean {
        if (input.getNodeType() in COMPOSITES && input.getChildren().size >= 2) {
            val oppositeComposites = input.getChildren().filter {
                it.getNodeType() == COMPOSITE_FLIP[input.getNodeType()] && it.getChildren().isNotEmpty()
            }
            val otherChildren = input.getChildren().filter { it !in oppositeComposites }.toSet()

            return oppositeComposites.any { otherChildren.intersect(it.getChildren().toSet()).isNotEmpty() }
        }
        return false
    }

    private fun absorbComposite(input: T): T {
        val oppositeComposites = input.getChildren().filter {
            it.getNodeType() == COMPOSITE_FLIP[input.getNodeType()] && it.getChildren().isNotEmpty()
        }

        val otherChildren = input.getChildren().filter { it !in oppositeComposites }.toSet()
        oppositeComposites.forEach {
            if (otherChildren.intersect(it.getChildren().toSet()).isNotEmpty()) {
                input.removeChild(it)
            }
        }
        return input
    }

    private fun canDistributeTerm(input: T): Boolean {
        return input.getNodeType() in COMPOSITES && input.getChildren().size > 1 &&
                input.getChildren().any { child -> child.getNodeType() == COMPOSITE_FLIP[input.getNodeType()] }
    }

    private fun distributeTerm(input: T): T {
        val inputChildren = input.getChildren()
        val oppositeCompositeChild = inputChildren.find { it.getNodeType() == COMPOSITE_FLIP[input.getNodeType()] }!!

        val otherChildren = inputChildren - oppositeCompositeChild

        val childrenOfOpposite = oppositeCompositeChild.getChildren().toList()

        val result = factory.withType(COMPOSITE_FLIP[input.getNodeType()]!!)

        val newTerms = childrenOfOpposite.map { oppositeChild ->
            val newParent = factory.withType(input.getNodeType())
            newParent.addChild(oppositeChild)

            otherChildren.forEach { otherChild ->
                newParent.addChild(factory.fromPrototypeSubtree(otherChild))
            }

            newParent
        }

        newTerms.forEach { result.addChild(it) }

        return result
    }

    private fun canExtractCommonTerm(input: T): Boolean {
        if (input.getNodeType() in COMPOSITES) {
            val oppositeCompositeChildren = input.getChildren().filter {
                it.getNodeType() == COMPOSITE_FLIP[input.getNodeType()]
            }

            if (oppositeCompositeChildren.size > 1) {
                val oppositeKidsChildren = oppositeCompositeChildren.map { it.getChildren() }

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
        val oppositeComposites = input.getChildren().filter { it.getNodeType() == COMPOSITE_FLIP[input.getNodeType()] }
        oppositeComposites.forEach { input.removeChild(it) }

        val setOfChildren: List<List<T>> = oppositeComposites.map { it.getChildren() }
        val commonTerms = mutableListOf<T>()
        commonTerms.addAll(setOfChildren[0])

        for (i in 1 until setOfChildren.size) {
            commonTerms.retainAll(setOfChildren[i])
        }

        // Get the common term (just take the first)
        val commonTerm = commonTerms.first()
        // Make the new composites
        val newOpposite = factory.withType(COMPOSITE_FLIP[input.getNodeType()]!!)

        val newSame = factory.withType(input.getNodeType())

        // Now add the opposite composite grandkids to the 'newSame' collection
        oppositeComposites.forEach {
            it.removeChild(commonTerm)
            // if this is a degenerate composite, just add the one remaining child
            if (it.getChildren().size == 1) {
                val firstChild = it.getChildren()[0]
                newSame.addChild(firstChild)
            } else if (it.getChildren().size > 1) {
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
        if (input.getNodeType() in COMPOSITES) {
            return input.getChildren().toSet().size != input.getChildren().size
        }
        return false
    }

    private fun collapseIdempotentComposite(input: T): T {
        val children = input.getChildren().toList()
        children.forEach { input.removeChild(it) }

        children.toSet().forEach { input.addChild(it) }
        return input
    }

    private fun containsCollapsibleComposites(input: T): Boolean {
        return input.getNodeType() in COMPOSITES && input.getChildren().any { it.getNodeType() == input.getNodeType() }
    }

    private fun collapseComposite(input: T): T {
        val redundantKids = input.getChildren().filter { it.getNodeType() == input.getNodeType() }
        redundantKids.forEach { input.removeChild(it) }

        val grandKids = redundantKids.flatMap { it.getChildren() }
        grandKids.forEach { input.addChild(it) }

        return input
    }

    private fun doubleNegationIsPresent(input: T): Boolean {
        return input.getNodeType() == NodeType.NOT &&
                input.getChildren().isNotEmpty() &&
                input.getChildren().first().getNodeType() == NodeType.NOT
    }

    private fun collapseDoubleNegation(input: T): T? {
        return if (input.getChildren().first().getChildren().isNotEmpty()) {
            input.getChildren().first().getChildren().first()
        } else {
            null
        }
    }

    private fun isDegenerateComposite(input: T): Boolean {
        return input.getNodeType() in COMPOSITES && input.getChildren().size < 2
    }


    private fun collapseDegenerateComposite(input: T): T? {
        return if (input.getChildren().isNotEmpty()) {
            input.getChildren().first()
        } else {
            null
        }
    }

    private fun doesDeMorgansLawApply(input: T): Boolean {
        val caseOne = input.getNodeType() == NodeType.NOT &&
                input.getChildren().first().getNodeType() in COMPOSITES &&
                input.getChildren().first().getChildren().size == 2

        val caseTwo = input.getNodeType() in COMPOSITES &&
                input.getChildren().size == 2 &&
                input.getChildren().all { it.getNodeType() == NodeType.NOT }

        return caseOne || caseTwo
    }

    private fun applyDeMorgansLaw(input: T): T {
        return if (input.getNodeType() in COMPOSITES) {
            val elementOne = input.getChildren()[0].getChildren()[0]
            val elementTwo = input.getChildren()[1].getChildren()[0]

            factory.withType(NodeType.NOT).addChild(
                    factory.withType(COMPOSITE_FLIP[input.getNodeType()]!!).addChildren(listOf(elementOne, elementTwo))
            )
        } else {
            val composite = input.getChildren()[0]
            val elementOne = composite.getChildren()[0]
            val elementTwo = composite.getChildren()[1]

            factory.withType(COMPOSITE_FLIP[composite.getNodeType()]!!).addChildren(listOf(
                    factory.withType(NodeType.NOT).addChild(elementOne),
                    factory.withType(NodeType.NOT).addChild(elementTwo)
            ))
        }
    }

    private fun containsBasicComplement(input: T): Boolean {
        val nots = input.getChildren().filter { it.getNodeType() == NodeType.NOT }

        return nots.any { it.getChildren().isNotEmpty() && it.getChildren().first().getNodeType() in CONSTANT_BOOL }
    }

    private fun simplifyBasicComplement(input: T): T {
        val readyForAbsorption: List<T> = input.getChildren().filter {
            it.getNodeType() == NodeType.NOT && it.getChildren().isNotEmpty() && it.getChildren().first().getNodeType() in CONSTANT_BOOL
        }

        readyForAbsorption.forEach {
            input.removeChild(it)
            input.addChild(factory.withType(CONSTANT_BOOL_FLIP[it.getChildren().first().getNodeType()]!!))
        }

        return input
    }

    private fun containsComplement(input: T): Boolean {
        if (input.getNodeType() in COMPOSITES) {
            val havesAndHaveNot: Map<Boolean, List<T>> = input.getChildren().groupBy { it.getNodeType() == NodeType.NOT }
            val negatedChildren: Set<T> = havesAndHaveNot.getOrDefault(true, listOf()).flatMap { p -> p.getChildren() }.toSet()
            val otherChildren: Set<T> = havesAndHaveNot.getOrDefault(false, listOf()).toSet()

            if (negatedChildren.intersect(otherChildren).isNotEmpty()) {
                return true
            }
        }

        return false
    }

    private fun simplifyComplement(input: T): T {
        input.getChildren().forEach { input.removeChild(it) }
        return if (input.getNodeType() == NodeType.ANY) {
            input.addChild(factory.withType(NodeType.TRUE))
        } else {
            input.addChild(factory.withType(NodeType.FALSE))
        }
    }

    private fun isCompositeWithConstant(input: T): Boolean {
        return (input.getNodeType() == NodeType.ANY && input.getChildren().any { it.getNodeType() == NodeType.TRUE }) ||
                (input.getNodeType() == NodeType.ALL && input.getChildren().any { it.getNodeType() == NodeType.FALSE })
    }

    private fun simplifyCompositeWithConstant(input: T): T {
        return if (input.getNodeType() == NodeType.ANY) {
            factory.withType(NodeType.TRUE)
        } else {
            factory.withType(NodeType.FALSE)
        }
    }

    private fun depthFirstTraversal(root: T, visitor: (T) -> Unit) {
        root.getChildren().forEach { depthFirstTraversal(it, visitor) }
        visitor(root)
    }

    fun prettyPrint(input: T): String {
        fun printer(node: T): String {
            // process myself, then each of my children
            var representation = "wtf"
            if (node.getNodeType() == NodeType.PREDICATE) {
                representation = ((node as BasicNode).data as String)[0].toString()
            } else if (node.getNodeType() == NodeType.FALSE) {
                representation = "F"
            } else if (node.getNodeType() == NodeType.TRUE) {
                representation = "T"
            } else if (node.getNodeType() == NodeType.NULL) {
                representation = "X"
            } else if (node.getNodeType() == NodeType.ANY) {
                representation = " + "
            } else if (node.getNodeType() == NodeType.ALL) {
                representation = " * "
            } else if (node.getNodeType() == NodeType.NOT) {
                representation = " ¬"
            }
            var result = ""
            if (node.getNodeType() == NodeType.NOT) {
                result += representation
            }
            if (node.getNodeType() in listOf(NodeType.ANY, NodeType.ALL, NodeType.NOT)) {
                result += ('(')
            } else if (node.getNodeType() in listOf(NodeType.NULL, NodeType.PREDICATE, NodeType.TRUE, NodeType.FALSE)) {
                result += representation
            }

            val childRepresentations = node.getChildren().map { printer(it) }
            result += childRepresentations.joinToString(representation)
            if (node.getNodeType() in listOf(NodeType.ANY, NodeType.ALL, NodeType.NOT)) {
                result += (')')
            }
            return result
        }
        return printer(input)
    }
}
