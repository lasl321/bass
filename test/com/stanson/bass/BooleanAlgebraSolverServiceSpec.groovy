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
        // (A AND B) OR (A AND C) -> A AND (B OR C)
        ParseNode input = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B')
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                    new ParseNode(ParseNodeType.PREDICATE, 'A'),
                    new ParseNode(ParseNodeType.PREDICATE, 'C')
                )
        )

        ParseNode expected = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        when:
        ParseNode result = service.solve(input)
        then:
        result == expected
    }

    void 'simplifies Distributive Law (2nd Form)'() {
        given:
        // (A OR B) AND (A OR C) -> A OR (B AND C)
        ParseNode input = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                ),
        )

        ParseNode expected = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                )
        )
        when:
        ParseNode result = service.solve(input)
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
        )
        ParseNode orVersion = new ParseNode(ParseNodeType.ALL).addChildren(
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
                new ParseNode(ParseNodeType.PREDICATE, 'A'),
        )
        ParseNode expected = new ParseNode(ParseNodeType.PREDICATE, 'A')
        when:
        ParseNode andResult = service.solve(andVersion)
        ParseNode orResult = service.solve(orVersion)
        then:
        andResult == expected
        orResult == expected
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
        service.applyDeMorgansLaw(formOneA) == formOneB
        service.applyDeMorgansLaw(formOneB) == formOneA


        service.applyDeMorgansLaw(formTwoA) == formTwoB
        service.applyDeMorgansLaw(formTwoB) == formTwoA
    }

    void 'simplifies Associative Law'() {
        given:
        ParseNode formOne = new ParseNode(ParseNodeType.ANY).addChidren(
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

        ParseNode formTwo = new ParseNode(ParseNodeType.ALL).addChidren(
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
        resultOne == expected
        resultTwo == expected
    }
}
