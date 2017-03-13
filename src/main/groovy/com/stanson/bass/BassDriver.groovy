package com.stanson.bass

import com.stanson.parsing.BasicNode
import com.stanson.parsing.BasicNodeFactory
import com.stanson.parsing.BaseNodeType
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
        BasicNode easy = new BasicNode(BaseNodeType.ANY).addChildren(
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
        log.info('Simplifying easy case: ' + bass.prettyPrint(easy))
        BasicNode easyResult = bass.solve(easy)
        log.info('Easy result is: ' + bass.prettyPrint(easyResult))
        // ((A * B) + ((B * C) + (B * C * D) + (B * D * E)))
        // Simplifies to (B * (A + C + (D * E)))
        BasicNode medium = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                ),
                new BasicNode(BaseNodeType.ANY).addChildren(
                        new BasicNode(BaseNodeType.ALL).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        ),
                        new BasicNode(BaseNodeType.ALL).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'C'),
                                new BasicNode(BaseNodeType.PREDICATE, 'D'),
                        ),
                        new BasicNode(BaseNodeType.ALL).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'B'),
                                new BasicNode(BaseNodeType.PREDICATE, 'D'),
                                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                        ),
                ),
        )
        log.info('Simplifying medium case: ' + bass.prettyPrint(medium))
        BasicNode mediumResult = bass.solve(medium)
        log.info('Medium result: ' + bass.prettyPrint(mediumResult))
        // ((A * B * F) + (A * C * F) + (A * C) + (A * D) + (A * B * C) + (B * A) + E + F + ((E + F) * (F + E)))
        BasicNode hard = new BasicNode(BaseNodeType.ANY).addChildren(
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'F'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                        new BasicNode(BaseNodeType.PREDICATE, 'F'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'D'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'C'),
                ),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.PREDICATE, 'B'),
                        new BasicNode(BaseNodeType.PREDICATE, 'A'),
                ),
                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                new BasicNode(BaseNodeType.PREDICATE, 'F'),
                new BasicNode(BaseNodeType.ALL).addChildren(
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                                new BasicNode(BaseNodeType.PREDICATE, 'F'),
                        ),
                        new BasicNode(BaseNodeType.ANY).addChildren(
                                new BasicNode(BaseNodeType.PREDICATE, 'F'),
                                new BasicNode(BaseNodeType.PREDICATE, 'E'),
                        ),
                )
        )
        log.info('Simplifying hard case: ' + bass.prettyPrint(hard))
        BasicNode hardResult = bass.solve(hard)
        log.info('Hard result: ' + bass.prettyPrint(hardResult))
        log.info('Complete')
    }


}
