package com.stanson.bass

import com.stanson.parsing.BasicNode
import com.stanson.parsing.BasicNodeFactory
import com.stanson.parsing.ParseNodeType
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
        BasicNode input = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C')
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D')
                ),
        )


        BasicNode expected = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                    new BasicNode(ParseNodeType.PREDICATE, 'A'),
                    new BasicNode(ParseNodeType.ANY).addChildren(
                            new BasicNode(ParseNodeType.PREDICATE, 'B'),
                            new BasicNode(ParseNodeType.PREDICATE, 'C'),
                            new BasicNode(ParseNodeType.PREDICATE, 'D'),
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
        BasicNode input = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'D'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        )
                ),
        )
        BasicNode expected = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'D'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                    new BasicNode(ParseNodeType.PREDICATE, 'B'),
                    new BasicNode(ParseNodeType.ANY).addChildren(
                            new BasicNode(ParseNodeType.PREDICATE, 'A'),
                            new BasicNode(ParseNodeType.ALL).addChildren(
                                    new BasicNode(ParseNodeType.PREDICATE, 'C'),
                                    new BasicNode(ParseNodeType.ANY).addChildren(
                                            new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                            new BasicNode(ParseNodeType.PREDICATE, 'C'),
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
        BasicNode input = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
                ),
        )

        BasicNode expected = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
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
        BasicNode input = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                )
        )

        BasicNode expected = new BasicNode(ParseNodeType.PREDICATE, 'A')
        when:
        BasicNode result = service.solve(input)
        then:
        result == expected
    }

    void 'simplifies Idempotent Law'() {
        given:
        BasicNode andVersion = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
        )
        BasicNode orVersion = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
        )
        BasicNode expectedAnd = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B')
        )
        BasicNode expectedOr = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B')
        )
        expect:
        service.solve(andVersion) == expectedAnd
        service.solve(orVersion) == expectedOr
    }

    void 'identifies cases for DeMorgans Law'() {
        given:
        // NOT (A AND B)
        BasicNode demorgansCaseOneA = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B)
        BasicNode demorgansCaseOneB = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) OR (NOT B)
        BasicNode demorgansCaseTwoA = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) AND (NOT B)
        BasicNode demorgansCaseTwoB = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode noMatch = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
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
        BasicNode formOneA = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode formOneB = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B) -> (NOT A) AND (NOT B)
        BasicNode formTwoA = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        BasicNode formTwoB = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B')
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
        BasicNode formOne = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C')
                )
        )
        BasicNode expectedOne = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.PREDICATE, 'C')
        )

        BasicNode formTwo = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C')
                )
        )
        BasicNode expectedTwo = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.PREDICATE, 'C')
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
        BasicNode caseOne = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'Depth 3')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.ALL).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'Depth 4')
                        )
                )
        )
        BasicNode caseTwo = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'Depth 3')
                )
        )
        expect:
        service.calculateTreeDepth(caseOne) == 4
        service.calculateTreeDepth(caseTwo) == 3

    }

    void 'should reduce degenerate composites'() {
        given:
        BasicNode predicate = new BasicNode(ParseNodeType.PREDICATE, 'A')
        BasicNode degenerateAnd = new BasicNode(ParseNodeType.ALL).addChildren(factory.fromPrototypeSubtree(predicate))
        BasicNode degenerateOr = new BasicNode(ParseNodeType.ANY).addChildren(factory.fromPrototypeSubtree(predicate))
        BasicNode negationIsOK = new BasicNode(ParseNodeType.NOT).addChildren(factory.fromPrototypeSubtree(predicate))
        expect:
        service.solve(degenerateAnd) == predicate
        service.solve(degenerateOr) == predicate
        service.solve(negationIsOK) == negationIsOK
    }

    void 'should count tree expressions'() {
        given:
        BasicNode caseOne = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                ),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.ALL).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B')
                        )
                )
        )
        BasicNode caseTwo = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.countExpressions(caseOne) == 6
        service.countExpressions(caseTwo) == 3

    }


    void 'should identify cases where a term may be distributed'() {
        given:
        // A(B + C + D) -> AB + AC + AD
        BasicNode case1 = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + BCD      -> (A+B)(A+C)(A+D)
        BasicNode case2 = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + C    -> A + B + C
        BasicNode case3 = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.PREDICATE, 'C'),
        )
        // ABC          -> ABC
        BasicNode case4 = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.PREDICATE, 'C'),
        )
        // AB(C + D)    -> ABC + ABD
        BasicNode case5 = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + CD   -> (A + B + C)(A + B + D)
        BasicNode case6 = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
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
        BasicNode input = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        )
                )
        )
        BasicNode expected = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        service.lookAhead = 2
        expect:
        service.solve(input) == expected
    }

    void 'should identify absorption 1 and 2 cases'() {
        given:
        BasicNode absorption1Case = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode absorption2Case = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode shouldFail = new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        )
                )

        BasicNode shouldAlsoFail = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
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
        BasicNode absorption1Case = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        BasicNode predicateA = new BasicNode(ParseNodeType.PREDICATE, 'A')

        BasicNode absorption2Case = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        expect:
        service.absorbComposite(absorption1Case) == new BasicNode(ParseNodeType.ALL).addChildren(predicateA)
        service.absorbComposite(absorption2Case) == new BasicNode(ParseNodeType.ANY).addChildren(predicateA)
    }

    void 'should handle advanced absorption cases'() {
        given:
        BasicNode caseOne = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),

                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),

                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'E'),

                ),
        )
        BasicNode expectedOne = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B')
        )

        BasicNode caseTwo = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),

                ),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),

                ),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'E'),

                ),
        )
        BasicNode expectedTwo = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B')
        )
        expect:
        service.absorbComposite(caseOne) == expectedOne
        service.absorbComposite(caseTwo) == expectedTwo
    }

    void 'should simplify composite complements'() {
        given:
        BasicNode andNegation = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        BasicNode orNegation = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.NOT).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.solve(andNegation) == new BasicNode(ParseNodeType.FALSE)
        service.solve(orNegation) == new BasicNode(ParseNodeType.TRUE)
    }

    void 'should simplify basic complements'() {
        given:
        BasicNode trueNegation = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.TRUE)
        )
        BasicNode falseNegation = new BasicNode(ParseNodeType.NOT).addChildren(
                new BasicNode(ParseNodeType.FALSE)
        )
        expect:
        service.solve(trueNegation) == new BasicNode(ParseNodeType.FALSE)
        service.solve(falseNegation) == new BasicNode(ParseNodeType.TRUE)
    }

    void 'should simplify composites containing true or false'() {
        given:
        BasicNode andCase = new BasicNode(ParseNodeType.ALL).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.FALSE),
                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                new BasicNode(ParseNodeType.PREDICATE, 'D'),
        )
        BasicNode orCase = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.PREDICATE, 'A'),
                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                new BasicNode(ParseNodeType.TRUE),
                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                new BasicNode(ParseNodeType.PREDICATE, 'D'),
        )
        expect:
        service.solve(andCase) == new BasicNode(ParseNodeType.FALSE)
        service.solve(orCase) == new BasicNode(ParseNodeType.TRUE)
    }
}
