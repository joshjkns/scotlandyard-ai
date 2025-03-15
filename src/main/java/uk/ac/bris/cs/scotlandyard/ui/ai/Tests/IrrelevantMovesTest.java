package uk.ac.bris.cs.scotlandyard.ui.ai.Tests;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.collect.*;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public class IrrelevantMovesTest {

    private static Map<Integer,Double> dijkstraMap;

    public static void setDijkstraMap(){
        dijkstraMap = new HashMap<>();
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;
        try {
            defaultGraph = readGraph(Resources.toString(Resources.getResource("graph.txt"), StandardCharsets.UTF_8));
        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }

        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        detectivesList.add(new Player(ScotlandYard.ALL_PIECES.get(1),defaultDetectiveTickets(),45));
        detectivesList.add(new Player(ScotlandYard.ALL_PIECES.get(2),defaultDetectiveTickets(),20));
        detectivesList.add(new Player(ScotlandYard.ALL_PIECES.get(3),defaultDetectiveTickets(),4));
        detectivesList.add(new Player(ScotlandYard.ALL_PIECES.get(4),defaultDetectiveTickets(),5));
        detectivesList.add(new Player(ScotlandYard.ALL_PIECES.get(5),defaultDetectiveTickets(),6));
        Board.GameState newGamestate = factory.build(new GameSetup(defaultGraph,ScotlandYard.STANDARD24MOVES), new Player(ScotlandYard.ALL_PIECES.get(0),defaultMrXTickets(),1), ImmutableList.copyOf(detectivesList));
        dijkstraMap = Dijkstra.dijkstraFunction(newGamestate.getSetup().graph,1);
    }

    public static void testRemovesIrrelevant(Map<Integer,Double> dijkstraMap){
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(1),45, ScotlandYard.Ticket.SECRET,46));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(1),45, ScotlandYard.Ticket.TAXI,76));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(1),45, ScotlandYard.Ticket.SECRET,60));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(2),34, ScotlandYard.Ticket.SECRET,59));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(5),3, ScotlandYard.Ticket.TAXI,5));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(2),4, ScotlandYard.Ticket.SECRET,3));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(3),6, ScotlandYard.Ticket.TAXI,7));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(4),11, ScotlandYard.Ticket.SECRET,15));
        answerArray = Filter.filterIrrelevantMoves(testArray, ScotlandYard.ALL_PIECES.get(1),dijkstraMap);
        if (answerArray.size() != 1){
            System.out.println("Test Failed: Did not remove the irrelevant moves , and non-Red moves [T3.1]");
        }

//        try{
//            assert (answerArray.size() == 1);
//        } catch (AssertionError e) {
//            System.out.println("Test Failed: RemovingIrrelevantMoves " + e);
//        }
    }

    public static void testKeepsRelevant(Map<Integer,Double> dijkstraMap){
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray = new ArrayList<>();
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(1),45, ScotlandYard.Ticket.TAXI,46));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(1),45, ScotlandYard.Ticket.SECRET,58));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(2),34, ScotlandYard.Ticket.SECRET,59));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(5),3, ScotlandYard.Ticket.TAXI,5));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(2),4, ScotlandYard.Ticket.SECRET,3));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(3),6, ScotlandYard.Ticket.TAXI,7));
        testArray.add(new Move.SingleMove(ScotlandYard.ALL_PIECES.get(4),11, ScotlandYard.Ticket.SECRET,15));
        answerArray = Filter.filterIrrelevantMoves(testArray, ScotlandYard.ALL_PIECES.get(5),dijkstraMap);
        if (answerArray.size() != 1){
            System.out.println("Test Failed: Removed moves from the piece provided [T3.2]");
        }
        answerArray = Filter.filterIrrelevantMoves(testArray, ScotlandYard.ALL_PIECES.get(1),dijkstraMap);
        if (answerArray.size() != 2){
            System.out.println("Test Failed: Removed moves that were less than two values off the best move [T3.2]");
        }

//        try{
//            assert (answerArray.size() == testArray.size());
//        } catch (AssertionError e) {
//            System.out.println("Test Failed: Removed moves, that weren't of piece provided " + e);
//        }
//        answerArray = Filter.filterIrrelevantMoves(testArray, ScotlandYard.ALL_PIECES.get(1),dijkstraMap);
//        try{
//            assert (answerArray.size() == testArray.size());
//        } catch (AssertionError e) {
//            System.out.println("Test Failed: Removed moves that were less than two values off the best move " + e);
//        }
    }

    public static void testEmptyInput(Map<Integer,Double> dijkstraMap){
        ArrayList<Move> testArray = new ArrayList<>();
        ArrayList<Move> answerArray;
        answerArray = Filter.filterIrrelevantMoves(testArray, ScotlandYard.ALL_PIECES.get(5),dijkstraMap);
        if (!answerArray.isEmpty()){
            System.out.println("Test Failed: Resulting moves array should be of length 0, as the input is of length 0 [T3.3]");
        }
//        try{
//            assert (answerArray.size() == 0);
//        } catch (AssertionError e) {
//            System.out.println("Test Failed: Resulting moves array should be of length 0, as the input is of length 0 " + e);
//        }
    }

    public static void main(){
        setDijkstraMap();
        testRemovesIrrelevant(dijkstraMap);
        testKeepsRelevant(dijkstraMap);
        testEmptyInput(dijkstraMap);
    }

}
