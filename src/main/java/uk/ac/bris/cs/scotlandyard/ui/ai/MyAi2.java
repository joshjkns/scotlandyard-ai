package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ArrayListMultimap;
import org.glassfish.grizzly.Transport;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.gamekit.graph.Node;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.*;
import com.google.common.collect.ImmutableSet;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi2 implements Ai {

    @Nonnull @Override public String name() { return "[MRX] MiniMax v2"; }

    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        HashMap<Ticket, Integer> tempTicketMap = new HashMap<>();
        ArrayList<Ticket> tempTicketList = new ArrayList<>(Arrays.asList(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND, Ticket.DOUBLE, Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        Player mrX = null;
        int location = 0;
        for (Piece piece : board.getPlayers()) {
            for (Ticket ticket : tempTicketList) {
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            if (piece.isMrX()){
                location = board.getAvailableMoves().asList().get(0).source();
                mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
            } else {
                Detective newDetective = (Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
            }
        }
        Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

        int lastLocation = location;
        for (LogEntry entry : board.getMrXTravelLog()) {
            if (entry.location().isPresent()) {
                lastLocation = entry.location().get();
            }
        }

        ArrayList<Move> moves = new ArrayList<Move>(gameState.getAvailableMoves().asList());
        int source = 0;
        for (Move move : gameState.getAvailableMoves()) {
            if (move.commencedBy().isMrX()) {
                source = move.source();
            }
        }

        ArrayList<Piece> playerList = new ArrayList<>(gameState.getPlayers().asList());
        ArrayList<Move> newMoves = duplicatePruning(moves);
        Map<Integer, Double> dijkstraResult = dijkstra(gameState, source);
        ArrayListMultimap<Double, Move> finalMap = ArrayListMultimap.create();
        double bestVal = miniMax(dijkstraResult, playerList, gameState, finalMap, newMoves);

        Move chosenMove = null;
        double maxDistance = -1;
        Map<Integer, Double> dijkstraLastLocation = dijkstra(gameState, lastLocation);
        for (Move tempMove : finalMap.get(bestVal)) {
            if (maxDistance == -1) {
                chosenMove = tempMove;
            }
            int destination = tempMove.accept(new Move.Visitor<>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });
            double distance = dijkstraLastLocation.get(destination);
            if (distance > maxDistance) {
                maxDistance = distance;
                chosenMove = tempMove;
            }
        }


        System.out.println(finalMap);
        assert chosenMove != null;
        return chosenMove;
    }

    public static Map<Integer, Double> dijkstra(Board.GameState board, int source){
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> valueGraph = board.getSetup().graph;
        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Boolean> visited = new HashMap<>();

        for (int i = 1; i <= 199; i++) {
            distances.put(i, Double.POSITIVE_INFINITY);
            visited.put(i, false);
        }

        distances.put(source, 0.0);
        visited.put(source, true);

        while (visited.containsValue(false)) {
            for (Map.Entry<Integer, Double> entry : distances.entrySet()) { // entry.getKey() is every node
                if (visited.get(entry.getKey())){ // visited = true
                    Set<Integer> adjNodes = valueGraph.adjacentNodes(entry.getKey());
                    for (int nextNode : adjNodes){
                        if (!visited.get(nextNode)){
                            if (valueGraph.edgeValue(entry.getKey(), nextNode).get().stream().anyMatch(t -> t.requiredTicket() == Ticket.SECRET)) {
                                break;
                            }
                            double newDistance = entry.getValue() + 1;
                            if (newDistance < distances.get(nextNode)) {
                                distances.put(nextNode, newDistance);
                            }
                        }
                    }
                }
            }
            for (Map.Entry<Integer, Double> entry : distances.entrySet()){
                if (entry.getValue() != Double.POSITIVE_INFINITY) {
                    visited.put(entry.getKey(), true);
                }
            }
        }
        return distances;
    }

    public static double miniMax(Map<Integer,Double> dijkstraResult, ArrayList<Piece> players, Board.GameState gameState, ArrayListMultimap<Double, Move> finalMap, List<Move> moves) {
        //System.out.println(mover);
        double bestVal = 0;
        double value = 0;
        ArrayList<Piece> tempPlayers = new ArrayList<>(players);

        Piece mover = tempPlayers.get(0); // remove current player
        //tempPlayers.remove(0);
        //System.out.println(mover);
        if (tempPlayers.size() != 1) {
            tempPlayers.remove(0);
        }
        if (tempPlayers.isEmpty()) { // leaf node
            //Detective lastPiece = (Detective) gameState.getPlayers().asList().get(gameState.getPlayers().size() - 1);
            Detective lastPiece = (Detective) mover;
            return dijkstraResult.get(gameState.getDetectiveLocation(lastPiece).get());
        }

        //System.out.println(mover);
        if (mover.isMrX()) {
            bestVal = Double.NEGATIVE_INFINITY;

            double detectiveTotal = 0;
            ArrayList<Move> moveList = new ArrayList<>(moves);
            for (Piece detective : players) {
                if (detective.isDetective()) {
                    detectiveTotal += dijkstraResult.get(gameState.getDetectiveLocation((Detective) detective).get());
                }
            }
            if (detectiveTotal <= gameState.getPlayers().size() * 2) {
                moveList = doubleOrSingleFilter(moves, false);
                //System.out.println(moveList);
            }
            if (!(detectiveTotal <= gameState.getPlayers().size() * 2) || moveList.isEmpty()) {
                moveList = doubleOrSingleFilter(moves, true);
            }

            for (Move move : moveList) {
                Board.GameState newState = gameState.advance(move);
                int destination = move.accept(new Move.Visitor<Integer>() {
                    @Override
                    public Integer visit(Move.SingleMove move) {
                        return move.destination;
                    }

                    @Override
                    public Integer visit(Move.DoubleMove move) {
                        return move.destination2;
                    }
                });
                ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves());
                value = miniMax(dijkstra(newState,destination), tempPlayers, newState, finalMap, duplicatePruning(newMoves));
                finalMap.put(value, move);
                bestVal = Math.max(value, bestVal);
            }
            return bestVal; // max values of all of mrx nodes

        } else { // detective
            bestVal = Double.POSITIVE_INFINITY;
            ArrayList<Move> moveList = getPlayerMoves(moves, mover);
            if (moveList.isEmpty()) {
                //System.out.println(mover);
                return dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get());// if they dont have moves just return the distance to them.
            }
            for (Move move : moveList) {
                Board.GameState newState = gameState.advance(move);
                ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves());
                value = miniMax(dijkstraResult, tempPlayers, newState, finalMap, duplicatePruning(newMoves));

                bestVal = Math.min(value, bestVal);
            }
            if (tempPlayers.size() == 1) {
                tempPlayers.remove(0);
            }
            return bestVal + dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get());
        }
    }

    public static ArrayList<Move> getPlayerMoves(List<Move> moves, Piece currentPlayer) {
        ArrayList<Move> resultList = new ArrayList<>();
        for (Move move : moves) {
            if (move.commencedBy() == currentPlayer) {
                resultList.add(move);
            }
        }
        return resultList;
    }

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

    public static ArrayList<Move> duplicatePruning(ArrayList<Move> moves) {
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

    public static <N, V> void printGraphToFile(MutableValueGraph<N, V> graph, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Graph:\n");
            for (N node : graph.nodes()) {
                for (N neighbor : graph.successors(node)) {
                    V value = graph.edgeValueOrDefault(node, neighbor, null);
                    writer.write(node + " -> " + neighbor + " [Value: " + value + "]\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



