package com.stanson.bass

import com.stanson.parsing.ParseNode
import com.stanson.parsing.ParseNodeType
import spock.lang.*

/**
 * Exercises the solver service.
 *
 * Created by rdahlgren on 12/23/16.
 */
class BooleanAlgebraSolverServiceSpec extends Specification {
    BooleanAlgebraSolverService service

    void setup() {
        service = new BooleanAlgebraSolverService()
    }

    void 'simplifies Distributive Law'() {
        given:
        // (A AND B) OR (A AND C) OR (A AND D) OR E -> E OR (A AND (B OR C OR D))
        ParseNode input = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C')
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D')
                ),
        )


        ParseNode expected = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                    new ParseNode(ParseNodeType.PREDICATE, 'A'),
                    new ParseNode(ParseNodeType.ANY).addChildren(
                            new ParseNode(ParseNodeType.PREDICATE, 'B'),
                            new ParseNode(ParseNodeType.PREDICATE, 'C'),
                            new ParseNode(ParseNodeType.PREDICATE, 'D'),
                    )
                )
        )
        expect:
        service.extractCommonTerm(input) == expected
    }

    void 'Distributive Law, second case'() {
        // This test is based on a failure I saw during development.
        given:
        // D + AB + BC(B + C)
        // Factoring out B
        //  D + B(A + C(B + C))
        ParseNode input = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'D'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        )
                ),
        )
        ParseNode expected = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'D'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                    new ParseNode(ParseNodeType.PREDICATE, 'B'),
                    new ParseNode(ParseNodeType.ANY).addChildren(
                            new ParseNode(ParseNodeType.PREDICATE, 'A'),
                            new ParseNode(ParseNodeType.ALL).addChildren(
                                    new ParseNode(ParseNodeType.PREDICATE, 'C'),
                                    new ParseNode(ParseNodeType.ANY).addChildren(
                                            new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                            new ParseNode(ParseNodeType.PREDICATE, 'C'),
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
        ParseNode input = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                ),
        )

        ParseNode expected = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                )
            )
        )
        when:
        ParseNode result = service.extractCommonTerm(input)
        then:
        result == expected
    }

    void 'simplifies Involution Law'() {
        given:
        // NOT NOT A -> A
        ParseNode input = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                )
        )

        ParseNode expected = new ParseNode(ParseNodeType.PREDICATE, 'A')
        when:
        ParseNode result = service.solve(input)
        then:
        result == expected
    }

    void 'simplifies Idempotent Law'() {
        given:
        ParseNode andVersion = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
        )
        ParseNode orVersion = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
        )
        ParseNode expectedAnd = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B')
        )
        ParseNode expectedOr = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B')
        )
        expect:
        service.solve(andVersion) == expectedAnd
        service.solve(orVersion) == expectedOr
    }

    void 'identifies cases for DeMorgans Law'() {
        given:
        // NOT (A AND B)
        ParseNode demorgansCaseOneA = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B)
        ParseNode demorgansCaseOneB = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) OR (NOT B)
        ParseNode demorgansCaseTwoA = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // (NOT A) AND (NOT B)
        ParseNode demorgansCaseTwoB = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        ParseNode noMatch = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
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
        ParseNode formOneA = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        ParseNode formOneB = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        // NOT (A OR B) -> (NOT A) AND (NOT B)
        ParseNode formTwoA = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        ParseNode formTwoB = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                )
        )
        expect:
        service.applyDeMorgansLaw(formOneA.shallowCopy()) == formOneB
        service.applyDeMorgansLaw(formOneB.shallowCopy()) == formOneA


        service.applyDeMorgansLaw(formTwoA.shallowCopy()) == formTwoB
        service.applyDeMorgansLaw(formTwoB.shallowCopy()) == formTwoA
    }

    void 'simplifies Associative Law'() {
        given:
        ParseNode formOne = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C')
                )
        )
        ParseNode expectedOne = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.PREDICATE, 'C')
        )

        ParseNode formTwo = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C')
                )
        )
        ParseNode expectedTwo = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.PREDICATE, 'C')
        )

        when:
        ParseNode resultOne = service.solve(formOne)
        ParseNode resultTwo = service.solve(formTwo)
        then:
        resultOne == expectedOne
        resultTwo == expectedTwo
    }

    void 'should calculate tree depth'() {
        given:
        ParseNode caseOne = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'Depth 3')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.ALL).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'Depth 4')
                        )
                )
        )
        ParseNode caseTwo = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'Depth 3')
                )
        )
        expect:
        service.calculateTreeDepth(caseOne) == 4
        service.calculateTreeDepth(caseTwo) == 3

    }

    void 'should reduce degenerate composites'() {
        given:
        ParseNode predicate = new ParseNode(ParseNodeType.PREDICATE, 'A')
        ParseNode degenerateAnd = new ParseNode(ParseNodeType.ALL).addChildren(predicate.shallowCopy())
        ParseNode degenerateOr = new ParseNode(ParseNodeType.ANY).addChildren(predicate.shallowCopy())
        ParseNode negationIsOK = new ParseNode(ParseNodeType.NOT).addChildren(predicate.shallowCopy())
        expect:
        service.solve(degenerateAnd) == predicate
        service.solve(degenerateOr) == predicate
        service.solve(negationIsOK) == negationIsOK
    }

    void 'should count tree expressions'() {
        given:
        ParseNode caseOne = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                ),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.ALL).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B')
                        )
                )
        )
        ParseNode caseTwo = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.countExpressions(caseOne) == 6
        service.countExpressions(caseTwo) == 3

    }


    void 'should identify cases where a term may be distributed'() {
        given:
        // A(B + C + D) -> AB + AC + AD
        ParseNode case1 = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + BCD      -> (A+B)(A+C)(A+D)
        ParseNode case2 = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + C    -> A + B + C
        ParseNode case3 = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.PREDICATE, 'C'),
        )
        // ABC          -> ABC
        ParseNode case4 = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.PREDICATE, 'C'),
        )
        // AB(C + D)    -> ABC + ABD
        ParseNode case5 = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                )
        )
        // A + B + CD   -> (A + B + C)(A + B + D)
        ParseNode case6 = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
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
        ParseNode input = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        )
                )
        )
        ParseNode expected = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        service.lookAhead = 2
        expect:
        service.solve(input) == expected
    }

    void 'should identify absorption 1 and 2 cases'() {
        given:
        ParseNode absorption1Case = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        ParseNode absorption2Case = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        ParseNode shouldFail = new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        )
                )

        ParseNode shouldAlsoFail = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
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
        ParseNode absorption1Case = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )

        ParseNode predicateA = new ParseNode(ParseNodeType.PREDICATE, 'A')

        ParseNode absorption2Case = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        expect:
        service.absorbComposite(absorption1Case) == new ParseNode(ParseNodeType.ALL).addChildren(predicateA)
        service.absorbComposite(absorption2Case) == new ParseNode(ParseNodeType.ANY).addChildren(predicateA)
    }

    void 'should handle advanced absorption cases'() {
        given:
        ParseNode caseOne = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),

                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),

                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'E'),

                ),
        )
        ParseNode expectedOne = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B')
        )

        ParseNode caseTwo = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),

                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),

                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'E'),

                ),
        )
        ParseNode expectedTwo = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B')
        )
        expect:
        service.absorbComposite(caseOne) == expectedOne
        service.absorbComposite(caseTwo) == expectedTwo
    }

    void 'should simplify composite complements'() {
        given:
        ParseNode andNegation = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        ParseNode orNegation = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.NOT).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A')
                )
        )
        expect:
        service.solve(andNegation) == new ParseNode(ParseNodeType.FALSE)
        service.solve(orNegation) == new ParseNode(ParseNodeType.TRUE)
    }

    void 'should simplify basic complements'() {
        given:
        ParseNode trueNegation = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.TRUE)
        )
        ParseNode falseNegation = new ParseNode(ParseNodeType.NOT).addChildren(
                new ParseNode(ParseNodeType.FALSE)
        )
        expect:
        service.solve(trueNegation) == new ParseNode(ParseNodeType.FALSE)
        service.solve(falseNegation) == new ParseNode(ParseNodeType.TRUE)
    }

    void 'should simplify composites containing true or false'() {
        given:
        ParseNode andCase = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.FALSE),
                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                new ParseNode(ParseNodeType.PREDICATE, 'D'),
        )
        ParseNode orCase = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                new ParseNode(ParseNodeType.TRUE),
                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                new ParseNode(ParseNodeType.PREDICATE, 'D'),
        )
        expect:
        service.solve(andCase) == new ParseNode(ParseNodeType.FALSE)
        service.solve(orCase) == new ParseNode(ParseNodeType.TRUE)
    }
}
