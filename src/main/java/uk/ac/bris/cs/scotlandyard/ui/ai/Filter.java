package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ArrayListMultimap;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;

import java.util.*;

public class Filter {
    public static ArrayList<Move> doubleOrSingleFilter(List<Move> moves, boolean singleMoves) {
        ArrayList<Move> returnMoves = new ArrayList<>();
        for (Move move : moves) {
            int isSingleMove = move.accept(new Move.Visitor<Integer>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return 1;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return 0;
                }
            });
            if (singleMoves && (isSingleMove == 1)) {
                returnMoves.add(move);
            }
            if (!singleMoves && (isSingleMove == 0)) {
                returnMoves.add(move);
            }
        }
        return returnMoves;
    }

    public static ArrayList<Move> duplicatePruning(List<Move> moves) {
        Map.Entry<Integer,Boolean> entry;
        Map<Integer,Move> singleMoveMap = new HashMap<>();
        Map<Integer,Move> doubleMoveMap = new HashMap<>();
        Collections.shuffle(moves); // so he doesn't use the secret x2 always first.
        for (Move move : moves) {
            entry = move.accept(new Move.Visitor<Map.Entry<Integer,Boolean>>() {
                @Override
                public Map.Entry<Integer,Boolean> visit(Move.SingleMove move) {
                    return new AbstractMap.SimpleEntry<>(move.destination, true);
                }

                @Override
                public Map.Entry<Integer,Boolean> visit(Move.DoubleMove move) {
                    return new AbstractMap.SimpleEntry<>(move.destination2, false);
                }
            });
            int destination = entry.getKey();
            boolean singleMove = entry.getValue();
            if (singleMove) {
                singleMoveMap.put(destination,move);
            }
            // removing double moves that go to and back to the same spot
            if (!singleMove && (destination != move.source())){
                doubleMoveMap.put(destination,move);
            }
        }
        for (int tempDestination : doubleMoveMap.keySet()) {
            if (!(singleMoveMap.containsKey(tempDestination))){
                singleMoveMap.put(tempDestination, doubleMoveMap.get(tempDestination));
            }
        }
        return new ArrayList<>(singleMoveMap.values());
    }

    public static ArrayList<Move> filterIrrelevantMoves(List<Move> moves, Board.GameState gameState, ArrayListMultimap<Move, Integer> movesMultimap){
        ArrayList<Move> operationMoves = new ArrayList<>(duplicatePruning(moves));
        operationMoves = doubleOrSingleFilter(operationMoves,true);
        ArrayList<Piece> players = new ArrayList<>(gameState.getPlayers());
        for (Move individualMove : operationMoves){
            Board.GameState newState = gameState.advance(individualMove);
            int destination = individualMove.accept(new Move.Visitor<>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });
            Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(newState, destination);
            int totalDistance = 0;
            for (Piece individualPlayer : players){
                if (individualPlayer.isDetective()){
                    Piece.Detective currentPiece = (Piece.Detective) individualPlayer;
                    totalDistance += dijkstraResult.get(newState.getDetectiveLocation(currentPiece).get());
                }
            }
            movesMultimap.put(individualMove, totalDistance);
        }
        return operationMoves;
    }
}
