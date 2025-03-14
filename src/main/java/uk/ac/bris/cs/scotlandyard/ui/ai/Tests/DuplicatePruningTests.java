package uk.ac.bris.cs.scotlandyard.ui.ai.Tests;

import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import uk.ac.bris.cs.scotlandyard.ui.ai.Recources.Filter;

import java.util.ArrayList;

public class DuplicatePruningTests {

    public static void testSingleMoveDuplicates() {
        ArrayList<Move> testArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,193));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.UNDERGROUND,193));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,190));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,188));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170));
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (newArray.size() != 4) {
            System.out.println("Test Failed: testSingleMoveDuplicates [T1.1]");
        }

//        try {
//            assert (newArray.size() == 4);
//        } catch (Exception e) {
//            System.out.println("Test Failed: testSingleMoveDuplicates " + e);
//        }
    }

    public static void testDoubleMoveDuplicates() {
        ArrayList<Move> testArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 65));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,190, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,188, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 30));
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (newArray.size() != 4) {
            System.out.println("Test Failed: testDoubleMoveDuplicates [T1.2]");
        }


//        try {
//            assert (newArray.size() == 4);
//        } catch (Exception e) {
//            System.out.println("Test Failed: testDoubleMoveDuplicates " + e);
//        }
    }

    public static void testMixedDuplicates() {
        ArrayList<Move> testArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,170, ScotlandYard.Ticket.SECRET, 40));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 65));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,65));
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (newArray.size() != 3) {
            System.out.println("Test Failed: testMixedDuplicates [T1.3]");
        }


//        try {
//            assert (newArray.size() == 3);
//        } catch (Exception e) {
//            System.out.println("Test Failed: testMixedDuplicates " + e);
//        }
    }

    public static void testCorrectPiece() {
        ArrayList<Move> testArray = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.TAXI,193));
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.TAXI,193));
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.UNDERGROUND,193));
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.BUS,190));
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.TAXI,188));
            testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(i),181, ScotlandYard.Ticket.BUS,170));
        }
        if (!Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0)).isEmpty()) {
            System.out.println("Test Failed: [MRX] testCorrectPieces [T1.4]");
        }
        if (Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(1)).size() != 4){
            System.out.println("Test Failed: [RED] testCorrectPieces [T1.4]");
        }
        if (!Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(5)).isEmpty()){
            System.out.println("Test Failed: [YELLOW] testCorrectPieces [T1.4]");
        }


//        try {
//            assert(Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0)).isEmpty());
//        } catch (Exception e) {
//            System.out.println("Test Failed: [MRX] testCorrectPieces " + e);
//        }
//        try {
//            assert(Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(1)).size() == 4);
//        } catch (Exception e) {
//            System.out.println("Test Failed: [RED] testCorrectPieces " + e);
//        }
//        try {
//            assert(Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(5)).isEmpty());
//        } catch (Exception e) {
//            System.out.println("Test Failed: [YELLOW] testCorrectPieces " + e);
//        }

    }

    public static void testLengthTwoBothDuplicates() {
        ArrayList<Move> testArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,192, ScotlandYard.Ticket.SECRET, 44));
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (newArray.size() != 1){
            System.out.println("Test Failed: testLengthTwoBothDuplicates [T1.5]");
        }

//        try {
//            assert(newArray.size() == 1);
//        } catch (Exception e) {
//            System.out.println("Test Failed: testLengthTwoBothDuplicates " + e);
//        }
    }

    public static void testAllDuplicates() {
        ArrayList<Move> testArray = new ArrayList<>();
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,192, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,191, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,190, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,189, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.DoubleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,188, ScotlandYard.Ticket.SECRET, 44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,44));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,44));
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (newArray.size() != 1){
            System.out.println("Test Failed: testAllDuplicates [T1.6]");
        }

//        try {
//            assert(newArray.size() == 1);
//        } catch (Exception e) {
//            System.out.println("Test Failed: testAllDuplicates " + e);
//        }
    }

    public static void testEmptyInput() {
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> newArray = Filter.duplicatePruning(testArray,ScotlandYard.ALL_PIECES.get(0));
        if (!newArray.isEmpty()) {
            System.out.println("Test Failed: Should return empty array [T1.7]");
        }
    }

    public static void main() {
        testSingleMoveDuplicates();
        testDoubleMoveDuplicates();
        testMixedDuplicates();
        testCorrectPiece();
        testLengthTwoBothDuplicates();
        testAllDuplicates();
        testEmptyInput();
    }
}
