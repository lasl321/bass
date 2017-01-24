package com.stanson.bass

import com.stanson.parsing.BasicNode
import com.stanson.parsing.BasicNodeFactory
import com.stanson.parsing.ParseNodeType
import groovy.util.logging.Log4j
import org.apache.log4j.Level

/**
 * Driver program for development purposes.
 *
 * Created by rdahlgren on 12/25/16.
 */
@Log4j
class BassDriver {
    static void main(String[] args) {
        BassDriver driver = new BassDriver()
        driver.driverLogic()

    }

    void driverLogic() {
        log.getRootLogger().setLevel(Level.INFO)
        log.info('Starting...')
        BooleanAlgebraSolverService bass = new BooleanAlgebraSolverService<BasicNode>(
                lookAhead: 3, factory: new BasicNodeFactory())
        // AB + BC(B + C)
        // Expect B(A + C)
        BasicNode easy = new BasicNode(ParseNodeType.ANY).addChildren(
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
        log.info('Simplifying easy case: ' + bass.prettyPrint(easy))
        BasicNode easyResult = bass.solve(easy)
        log.info('Easy result is: ' + bass.prettyPrint(easyResult))
        // ((A * B) + ((B * C) + (B * C * D) + (B * D * E)))
        // Simplifies to (B * (A + C + (D * E)))
        BasicNode medium = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(ParseNodeType.ANY).addChildren(
                        new BasicNode(ParseNodeType.ALL).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        ),
                        new BasicNode(ParseNodeType.ALL).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'C'),
                                new BasicNode(ParseNodeType.PREDICATE, 'D'),
                        ),
                        new BasicNode(ParseNodeType.ALL).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'B'),
                                new BasicNode(ParseNodeType.PREDICATE, 'D'),
                                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                        ),
                ),
        )
        log.info('Simplifying medium case: ' + bass.prettyPrint(medium))
        BasicNode mediumResult = bass.solve(medium)
        log.info('Medium result: ' + bass.prettyPrint(mediumResult))
        // ((A * B * F) + (A * C * F) + (A * C) + (A * D) + (A * B * C) + (B * A) + E + F + ((E + F) * (F + E)))
        BasicNode hard = new BasicNode(ParseNodeType.ANY).addChildren(
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'F'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                        new BasicNode(ParseNodeType.PREDICATE, 'F'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'D'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.PREDICATE, 'B'),
                        new BasicNode(ParseNodeType.PREDICATE, 'A'),
                ),
                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                new BasicNode(ParseNodeType.PREDICATE, 'F'),
                new BasicNode(ParseNodeType.ALL).addChildren(
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                                new BasicNode(ParseNodeType.PREDICATE, 'F'),
                        ),
                        new BasicNode(ParseNodeType.ANY).addChildren(
                                new BasicNode(ParseNodeType.PREDICATE, 'F'),
                                new BasicNode(ParseNodeType.PREDICATE, 'E'),
                        ),
                )
        )
        log.info('Simplifying hard case: ' + bass.prettyPrint(hard))
        BasicNode hardResult = bass.solve(hard)
        log.info('Hard result: ' + bass.prettyPrint(hardResult))
        log.info('Complete')
    }


}
