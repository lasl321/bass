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
        driver.configureLogging()
        driver.driverLogic()
    }

    void configureLogging() {
        log.level = Level.INFO
        String pattern = '%d{YYYY-MM-dd HH:mm:ss.SSSS} %c >> %m\n'
        // NOTE: Using groovy bean-style initialization (pattern: pattern or layout:consoleLayout) will
        // break these classes. Doing so fails to call the 'activateOptions' internal helper. Poor form
        // log4j!
        Layout consoleLayout = new PatternLayout(pattern)
        Appender console = new ConsoleAppender(consoleLayout)
        log.getRootLogger().addAppender(console)

    }

    void driverLogic() {
        log.info('Starting...')
        BooleanAlgebraSolverService bass = new BooleanAlgebraSolverService(lookAhead: 3)
        // (A OR B) AND (A OR C) AND (A OR D)
        // Expect a result akin to: A AND (B OR C OR D)
        ParseNode easy = new ParseNode(ParseNodeType.ALL).addChildren(
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
                        new ParseNode(ParseNodeType.PREDICATE, 'D')
                )
        )
        log.info('Simplifying easy case: ' + prettyPrint(easy))
        ParseNode easyResult = bass.solve(easy)
        log.info('Complete. Result is: ')
        log.info(prettyPrint(easyResult))
        // (A AND B) OR ((B AND C) OR (C AND D) OR (D AND E))
//        ParseNode medium
        // (A AND B) OR (B AND C) OR (A AND C) OR (A AND D) OR (B AND C) OR (B AND D) OR ((E OR F) AND (G OR H))
//        ParseNode hard
        log.info('Complete')
    }

    String prettyPrint(ParseNode input) {
        String line = '—'
        StringBuffer buffer = new StringBuffer()
        Closure printer
        printer = { Integer lines, ParseNode node ->
            // process myself, then each of my children
            String representation
            if (node.type == ParseNodeType.PREDICATE) {
                representation = node.data == null ? 'O' : node.data.toString()[0]
            } else if (node.type == ParseNodeType.NULL) {
                representation = 'X'
            } else if (node.type == ParseNodeType.ANY) {
                representation = '+'
            } else if (node.type == ParseNodeType.ALL) {
                representation = '*'
            } else if (node.type == ParseNodeType.NOT) {
                representation = '¬'
            }
            buffer.append('\n' + (line * lines))
            buffer.append(" $representation ")

            node.children.each {
                printer(lines + 2, it)
            }
        }
        printer(0, input)
        buffer
    }
}
