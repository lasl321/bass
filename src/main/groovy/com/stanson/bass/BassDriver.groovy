package com.stanson.bass

import com.stanson.parsing.ParseNode
import com.stanson.parsing.ParseNodeType
import groovy.util.logging.Log4j
import org.apache.log4j.Appender
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Layout
import org.apache.log4j.Level
import org.apache.log4j.PatternLayout

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
        log.info('Starting...')
        BooleanAlgebraSolverService bass = new BooleanAlgebraSolverService(lookAhead: 3)
        // AB + BC(B + C)
        // Expect B(A + C)
        ParseNode easy = new ParseNode(ParseNodeType.ANY).addChildren(
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
        log.info('Simplifying easy case: ' + prettyPrint(easy))
        ParseNode easyResult = bass.solve(easy)
        log.info('Easy result is: ' + prettyPrint(easyResult))
        // (A AND B) OR ((B AND C) OR (B AND C AND D) OR (B AND D AND E))
        // Simplifies to B AND (A OR C OR D OR E)
        ParseNode medium = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                ),
                new ParseNode(ParseNodeType.ANY).addChildren(
                        new ParseNode(ParseNodeType.ALL).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        ),
                        new ParseNode(ParseNodeType.ALL).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'C'),
                                new ParseNode(ParseNodeType.PREDICATE, 'D'),
                        ),
                        new ParseNode(ParseNodeType.ALL).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'B'),
                                new ParseNode(ParseNodeType.PREDICATE, 'D'),
                                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                        ),
                ),
        )
        log.info('Simplifying medium case: ' + prettyPrint(medium))
        ParseNode mediumResult = bass.solve(medium)
        log.info('Medium result: ' + prettyPrint(mediumResult))
        // ((A * B * F) + (A * C * F) + (A * C) + (A * D) + (A * B * C) + (B * A) + E + F + ((E + F) * (F + E)))
        ParseNode hard = new ParseNode(ParseNodeType.ANY).addChildren(
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'F'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                        new ParseNode(ParseNodeType.PREDICATE, 'F'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'D'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'C'),
                ),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.PREDICATE, 'B'),
                        new ParseNode(ParseNodeType.PREDICATE, 'A'),
                ),
                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                new ParseNode(ParseNodeType.PREDICATE, 'F'),
                new ParseNode(ParseNodeType.ALL).addChildren(
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                                new ParseNode(ParseNodeType.PREDICATE, 'F'),
                        ),
                        new ParseNode(ParseNodeType.ANY).addChildren(
                                new ParseNode(ParseNodeType.PREDICATE, 'F'),
                                new ParseNode(ParseNodeType.PREDICATE, 'E'),
                        ),
                )
        )
        log.info('Simplifying hard case: ' + prettyPrint(hard))
        ParseNode hardResult = bass.solve(hard)
        log.info('Hard result: ' + prettyPrint(hardResult))
        log.info('Complete')
    }

    String prettyPrint(ParseNode input) {
        Closure printer
        printer = { ParseNode node ->
            // process myself, then each of my children
            String representation = 'wtf'
            if (node.type == ParseNodeType.PREDICATE) {
                representation = (node.data == null) ? 'O' : node.data.toString()[0]
            } else if (node.type == ParseNodeType.NULL) {
                representation = 'X'
            } else if (node.type == ParseNodeType.ANY) {
                representation = ' + '
            } else if (node.type == ParseNodeType.ALL) {
                representation = ' * '
            } else if (node.type == ParseNodeType.NOT) {
                representation = ' ¬'
            }
            String result = ''
            if (node.type == ParseNodeType.NOT) {
                result += (representation)
            }
            if (node.type in [ParseNodeType.ANY, ParseNodeType.ALL, ParseNodeType.NOT]) {
                result += ('(')
            } else if (node.type in [ParseNodeType.NULL, ParseNodeType.PREDICATE]) {

                result += ("$representation")
            }

            List<String> childRepresentations = node.children.collect { printer(it) }
            result += (childRepresentations.join(representation))
            if (node.type in [ParseNodeType.ANY, ParseNodeType.ALL, ParseNodeType.NOT]) {
                result += (')')
            }
            return result
        }
        printer(input)
    }
}
