package com.stanson.bass

import com.stanson.parsing.BasicNode
import com.stanson.parsing.BasicNodeFactory
import com.stanson.parsing.BaseNodeType
import spock.lang.*

/**
 * Exercises the solver service.
 *
 * Created by rdahlgren on 12/23/16.
 */
class BooleanAlgebraSolverServiceSpec extends Specification {
    BooleanAlgebraSolverService service
    TreeLikeFactory<BasicNode> factory = new BasicNodeFactory()

    void setup() {
        service = new BooleanAlgebraSolverService<BasicNode>(factory: factory)
    }

    void 'simplifies Distributive Law'() {
        given:
        // (A AND B) OR (A AND C) OR (A AND D) OR E -> E OR (A AND (B OR C OR D))
        BasicNode input = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C')
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D')
                ),
        )


        BasicNode expected = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                    new BasicNode(BaseNodeType.PREDICATE, 'A'),
                    new BasicNode(BaseNodeType.ANY).addChildren(
                            new BasicNode(BaseNodeType.PREDICATE, 'B'),
                            new BasicNode(BaseNodeType.PREDICATE, 'C'),
                            new BasicNode(BaseNodeType.PREDICATE, 'D'),
                    )
                )
        )
        expect:
        service.prettyPrint(service.extractCommonTerm(input)) == service.prettyPrint(expected)
    }

    void 'Distributive Law, second case'() {
        // This test is based on a failure I saw during development.
        given:
        // D + AB + BC(B + C)
        // Factoring out B
        //  D + B(A + C(B + C))
        BasicNode input = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'D'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        )
                ),
        )
        BasicNode expected = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'D'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                    new BasicNode(BaseNodeType.PREDICATE, 'B'),
                    new BasicNode(BaseNodeType.ANY).addChildren(
                            new BasicNode(BaseNodeType.PREDICATE, 'A'),
                            new BasicNode(BaseNodeType.ALL).addChildren(
                                    new BasicNode(BaseNodeType.PREDICATE, 'C'),
                                    new BasicNode(BaseNodeType.ANY).addChildren(
                                            new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                            new BasicNode(BaseNodeType.PREDICATE, 'C'),
                                    )
                            )
                    )
                )
        )
        expect:
        service.extractCommonTerm(input) == expected
    }

    void 'simplifies Distributive Law (2nd Form)'() {
        given:
        // (A OR B) AND (A OR C) AND (A OR D) -> A OR (B AND C AND D)
        BasicNode input = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                ),
        )

        BasicNode expected = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                )
            )
        )
        when:
        BasicNode result = service.extractCommonTerm(input)
        then:
        result == expected
    }

    void 'simplifies Involution Law'() {
        given:
        // NOT NOT A -> A
        BasicNode input = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                )
        )

        BasicNode expected = new BasicNode(BaseNodeType.PREDICATE, 'A')
        when:
        BasicNode result = service.solve(input)
        then:
        result == expected
    }

    void 'simplifies Idempotent Law'() {
        given:
        BasicNode andVersion = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
        )
        BasicNode orVersion = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
        )
        BasicNode expectedAnd = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B')
        )
        BasicNode expectedOr = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B')
        )
        expect:
        service.solve(andVersion) == expectedAnd
        service.solve(orVersion) == expectedOr
    }

    void 'identifies cases for DeMorgans Law'() {
        given:
        // NOT (A AND B)
        BasicNode demorgansCaseOneA = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B)
        BasicNode demorgansCaseOneB = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) OR (NOT B)
        BasicNode demorgansCaseTwoA = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) AND (NOT B)
        BasicNode demorgansCaseTwoB = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode noMatch = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
        )
        expect:
        service.doesDeMorgansLawApply(demorgansCaseOneA)
        service.doesDeMorgansLawApply(demorgansCaseOneB)
        service.doesDeMorgansLawApply(demorgansCaseTwoA)
        service.doesDeMorgansLawApply(demorgansCaseTwoB)
        !service.doesDeMorgansLawApply(noMatch)
    }


    void 'properly applies DeMorgans Law'() {
        given:
        // NOT (A AND B) -> (NOT A) OR (NOT B)
        BasicNode formOneA = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode formOneB = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B) -> (NOT A) AND (NOT B)
        BasicNode formTwoA = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode formTwoB = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B')
                )
        )
        expect:
        service.applyDeMorgansLaw(factory.fromPrototypeSubtree(formOneA)) == formOneB
        service.applyDeMorgansLaw(factory.fromPrototypeSubtree(formOneB)) == formOneA


        service.applyDeMorgansLaw(factory.fromPrototypeSubtree(formTwoA)) == formTwoB
        service.applyDeMorgansLaw(factory.fromPrototypeSubtree(formTwoB)) == formTwoA
    }

    void 'simplifies Associative Law'() {
        given:
        BasicNode formOne = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C')
                )
        )
        BasicNode expectedOne = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.PREDICATE, 'C')
        )

        BasicNode formTwo = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C')
                )
        )
        BasicNode expectedTwo = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.PREDICATE, 'C')
        )

        when:
        BasicNode resultOne = service.solve(formOne)
        BasicNode resultTwo = service.solve(formTwo)
        then:
        resultOne == expectedOne
        resultTwo == expectedTwo
    }

    void 'should calculate tree depth'() {
        given:
        BasicNode caseOne = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'Depth 3')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.ALL).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'Depth 4')
                        )
                )
        )
        BasicNode caseTwo = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'Depth 3')
                )
        )
        expect:
        service.calculateTreeDepth(caseOne) == 4
        service.calculateTreeDepth(caseTwo) == 3

    }

    void 'should reduce degenerate composites'() {
        given:
        BasicNode predicate = new BasicNode(BaseNodeType.PREDICATE, 'A')
        BasicNode degenerateAnd = new BasicNode(BaseNodeType.ALL).addChildren(factory.fromPrototypeSubtree(predicate))
        BasicNode degenerateOr = new BasicNode(BaseNodeType.ANY).addChildren(factory.fromPrototypeSubtree(predicate))
        BasicNode negationIsOK = new BasicNode(BaseNodeType.NOT).addChildren(factory.fromPrototypeSubtree(predicate))
        expect:
        service.solve(degenerateAnd) == predicate
        service.solve(degenerateOr) == predicate
        service.solve(negationIsOK) == negationIsOK
    }

    void 'should count tree expressions'() {
        given:
        BasicNode caseOne = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.ALL).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B')
                        )
                )
        )
        BasicNode caseTwo = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.countExpressions(caseOne) == 6
        service.countExpressions(caseTwo) == 3

    }


    void 'should identify cases where a term may be distributed'() {
        given:
        // A(B + C + D) -> AB + AC + AD
        BasicNode case1 = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                )
        )
        // A + BCD      -> (A+B)(A+C)(A+D)
        BasicNode case2 = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + C    -> A + B + C
        BasicNode case3 = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.PREDICATE, 'C'),
        )
        // ABC          -> ABC
        BasicNode case4 = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.PREDICATE, 'C'),
        )
        // AB(C + D)    -> ABC + ABD
        BasicNode case5 = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + CD   -> (A + B + C)(A + B + D)
        BasicNode case6 = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                )
        )
        expect:
        service.canDistributeTerm(case1)
        service.canDistributeTerm(case2)
        !service.canDistributeTerm(case3)
        !service.canDistributeTerm(case4)
        service.canDistributeTerm(case5)
        service.canDistributeTerm(case6)
    }

    void 'should cleverly use distributive properties'() {
        given:
        BasicNode input = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        )
                )
        )
        BasicNode expected = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                )
        )
        service.lookAhead = 2
        expect:
        service.solve(input) == expected
    }

    void 'should identify absorption 1 and 2 cases'() {
        given:
        BasicNode absorption1Case = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode absorption2Case = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode shouldFail = new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        )
                )

        BasicNode shouldAlsoFail = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        )
                )
        )
        expect:
        service.canAbsorbComposite(absorption1Case)
        service.canAbsorbComposite(absorption2Case)
        !service.canAbsorbComposite(shouldFail)
        !service.canAbsorbComposite(shouldAlsoFail)
    }

    void 'should handle absorption 1 and 2 cases'() {
        given:
        BasicNode absorption1Case = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode predicateA = new BasicNode(BaseNodeType.PREDICATE, 'A')

        BasicNode absorption2Case = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                )
        )
        expect:
        service.absorbComposite(absorption1Case) == new BasicNode(BaseNodeType.ALL).addChildren(predicateA)
        service.absorbComposite(absorption2Case) == new BasicNode(BaseNodeType.ANY).addChildren(predicateA)
    }

    void 'should handle advanced absorption cases'() {
        given:
        BasicNode caseOne = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),

                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),

                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'E'),

                ),
        )
        BasicNode expectedOne = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B')
        )

        BasicNode caseTwo = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),

                ),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),

                ),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'E'),

                ),
        )
        BasicNode expectedTwo = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B')
        )
        expect:
        service.absorbComposite(caseOne) == expectedOne
        service.absorbComposite(caseTwo) == expectedTwo
    }

    void 'should simplify composite complements'() {
        given:
        BasicNode andNegation = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                )
        )
        BasicNode orNegation = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.NOT).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.solve(andNegation) == new BasicNode(BaseNodeType.FALSE)
        service.solve(orNegation) == new BasicNode(BaseNodeType.TRUE)
    }

    void 'should simplify basic complements'() {
        given:
        BasicNode trueNegation = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.TRUE)
        )
        BasicNode falseNegation = new BasicNode(BaseNodeType.NOT).addChildren(
                new BasicNode(BaseNodeType.FALSE)
        )
        expect:
        service.solve(trueNegation) == new BasicNode(BaseNodeType.FALSE)
        service.solve(falseNegation) == new BasicNode(BaseNodeType.TRUE)
    }

    void 'should simplify composites containing true or false'() {
        given:
        BasicNode andCase = new BasicNode(BaseNodeType.ALL).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.FALSE),
                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                new BasicNode(BaseNodeType.PREDICATE, 'D'),
        )
        BasicNode orCase = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.PREDICATE, 'A'),
                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                new BasicNode(BaseNodeType.TRUE),
                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                new BasicNode(BaseNodeType.PREDICATE, 'D'),
        )
        expect:
        service.solve(andCase) == new BasicNode(BaseNodeType.FALSE)
        service.solve(orCase) == new BasicNode(BaseNodeType.TRUE)
    }
}
