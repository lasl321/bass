package com.stanson.bass

import com.stanson.parsing.ParseNode

/**
 * Attempts to simplify provided boolean logic structures.
 *
 * Created by rdahlgren on 12/23/16.
 */
class BooleanAlgebraSolverService {
    /**
     * Given a ParseNode tree, attempt to return the simplest equivalent representation.
     *
     * @param input
     * @return
     */
    ParseNode solve(ParseNode input) {
        null
    }

    /**
     * Returns true if the subtree rooted at the provided ParseNode appears to be a case in which
     * DeMorgan's Law can be applied.
     *
     * @param input
     * @return
     */
    Boolean doesDeMorgansLawApply(ParseNode input) {

    }

    /**
     * Transforms the provided input according to DeMorgan's Law.
     * @param input
     * @return
     */
    ParseNode applyDeMorgansLaw(ParseNode input) {
        null
    }
}
