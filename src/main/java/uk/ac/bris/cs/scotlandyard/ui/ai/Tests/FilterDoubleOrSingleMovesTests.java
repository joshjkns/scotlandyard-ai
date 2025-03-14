package uk.ac.bris.cs.scotlandyard.ui.ai.Tests;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.Recources.Filter;

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
        if (!newArray.equals(answerArray)) {
            System.out.println("Test Failed: testFilterSingleMoves [T2.1]");
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
        if (!newArray.equals(answerArray)) {
            System.out.println("Test Failed: testFilterDoubleMoves [T2.2]");
        }
    }

    public static void testEmptyInput() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray1 = Filter.doubleOrSingleFilter(testArray,false);
        ArrayList<Move> answerArray2 = Filter.doubleOrSingleFilter(testArray,true);
        if (!answerArray1.isEmpty() || !answerArray2.isEmpty()) {
            System.out.println("Test Failed: Should return no values [T2.3]");
        }
    }

    public static void main() {
        testFilterSingleMoves();
        testFilterDoubleMoves();
        testEmptyInput();
    }
}
