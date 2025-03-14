package uk.ac.bris.cs.scotlandyard.ui.ai.tests;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;

/**
 * Includes all test for the actual game model
 */
public class AllTests {
    public static void main(String[] args) {
        DuplicatePruningTests.main();  //T1
        FilterDoubleOrSingleMovesTests.main();  //T2
        IrrelevantMovesTest.main();  //T3
        SecretPruningTests.main();  //T4
        System.out.println("All tests passed!");
    }
}


