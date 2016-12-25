package com.stanson.bass

import groovy.util.logging.Log4j

/**
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
        // todo
    }

    void driverLogic(BooleanAlgebraSolverService bass) {
        log.info('Starting...')
        log.info('Complete')
    }
}
