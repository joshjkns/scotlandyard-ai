package uk.ac.bris.cs.scotlandyard.ui.ai.tests;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.Filter;

import java.util.ArrayList;

public class FilterDoubleOrSingleMovesTests {

    public static void testFilterSingleMoves() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 65));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        ArrayList<Move> newArray = Filter.doubleOrSingleFilter(testArray,true);
        try {
            assert (newArray == testArray);
        } catch (Exception e) {
            System.out.println("Test Failed: testFilterSingleMoves " + e);
        }
    }

    public static void testFilterDoubleMoves() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 40));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 65));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 65));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        ArrayList<Move> newArray = Filter.doubleOrSingleFilter(testArray,false);
        try {
            assert (newArray == testArray);
        } catch (Exception e) {
            System.out.println("Test Failed: testFilterDoubleMoves " + e);
        }
    }

    public static void testFilterNoMoves() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray1 = Filter.doubleOrSingleFilter(testArray,false);
        ArrayList<Move> answerArray2 = Filter.doubleOrSingleFilter(testArray,true);
        try {
            assert (answerArray1.size() == 0);
        } catch (Exception e) {
            System.out.println("Test Failed: Should return no moves " + e);
        }
        try {
            assert (answerArray1.size() == 0);
        } catch (Exception e) {
            System.out.println("Test Failed: Should return no moves " + e);
        }
    }

    public static void main() {
        testFilterDoubleMoves();
        testFilterSingleMoves();
    }
}
