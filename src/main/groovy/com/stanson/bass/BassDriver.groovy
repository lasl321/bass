package com.stanson.bass

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
        BooleanAlgebraSolverService bass = new BooleanAlgebraSolverService()
        BassDriver driver = new BassDriver()
        driver.configureLogging()
        driver.driverLogic(bass)
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

    void driverLogic(BooleanAlgebraSolverService bass) {
        log.info('Starting...')
        log.info('Complete')
    }
}
