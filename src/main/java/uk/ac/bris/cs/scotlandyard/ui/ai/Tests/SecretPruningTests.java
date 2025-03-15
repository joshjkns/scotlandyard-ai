package uk.ac.bris.cs.scotlandyard.ui.ai.Tests;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

import java.util.ArrayList;

public class SecretPruningTests {

    public static void testHandlesDoubleAndSingleMoves() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,43));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,43));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,193, ScotlandYard.Ticket.SECRET, 44));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 40));
        answerArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,193, ScotlandYard.Ticket.SECRET, 44));
        ArrayList<Move> newArray = Filter.secretPruning(testArray);
        if (!answerArray.equals(newArray)){
            System.out.println("Test Failed: Cannot handle the processing of both single and double moves [T4.1]");
        }
    }

    public static void testSelectsBusAndTaxiFirst() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.BUS,65));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.BUS,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        ArrayList<Move> newArray = Filter.secretPruning(testArray);
        if (!newArray.equals(answerArray)){
            System.out.println("Test Failed: Failed to select a bus or taxi, before a secret and underground [T4.2]");
        }
    }

    public static void testSelectsSecretIfOnlySecret(){
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        ArrayList<Move> newArray = Filter.secretPruning(testArray);
        if (!newArray.equals(answerArray)){
            System.out.println("Test Failed: Failed to select a secret move, if that is the only option [T4.3]");
        }
    }

    public static void testUndergroundAsLastResort(){
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,65));
        answerArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,65));
        ArrayList<Move> newArray = Filter.secretPruning(testArray);
        if (!newArray.equals(answerArray)){
            System.out.println("Test Failed: Failed to select an underground move, if that is the only one transport for that move [T4.4]");
        }
    }

    public static void testEmptyInput() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray;
        answerArray = Filter.secretPruning(testArray);
        if (!answerArray.isEmpty()){
            System.out.println("Test Failed: Resulting moves array should be of length 0, as the input is of length 0 [T4.5]");
        }
    }

    public static void main() {
        testHandlesDoubleAndSingleMoves();
        testSelectsBusAndTaxiFirst();
        testSelectsSecretIfOnlySecret();
        testUndergroundAsLastResort();
        testEmptyInput();
    }
}
