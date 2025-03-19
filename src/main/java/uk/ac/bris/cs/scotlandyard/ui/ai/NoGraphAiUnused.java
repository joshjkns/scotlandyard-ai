package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;

import java.util.*;

import com.google.common.collect.ImmutableMap;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

public class NoGraphAiUnused implements Ai {

    @Nonnull @Override public String name() { return "[MRX] Unused (No Graph)"; }

    // pick move function to get move (main function)
    @Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        HashMap<Ticket, Integer> tempTicketMap = new HashMap<>();
        ArrayList<Ticket> tempTicketList = new ArrayList<>(Arrays.asList(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND, Ticket.DOUBLE, Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        Player mrX = null;
        int location = 0;

        // creating temporary gamestate to allow advance() to be used.
        // for all pieces
        for (Piece piece : board.getPlayers()) {
            for (Ticket ticket : tempTicketList) {
                // put their tickets into a map with each ticket type and the number of them.
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            if (piece.isMrX()){ // if piece is mrx, get his location and create his player
                location = board.getAvailableMoves().asList().get(0).source();
                mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
            } else {
                // cast piece to a detective and get their location before making their player.
                Detective newDetective = (Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
            }
        }
        // get the gamestate from the list of players, mrx and the setup
        Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

        // lastlocation is set to location unless he has made a reveal move - tries to get away due to players naturally going there.
        int lastLocation = location;
        for (LogEntry entry : board.getMrXTravelLog()) {
            if (entry.location().isPresent()) {
                lastLocation = entry.location().get();
            }
        }

        // looping through moves to get mrX source - why are we doign this
        ArrayList<Move> moves = new ArrayList<Move>(gameState.getAvailableMoves().asList());
        int source = 0;
        for (Move move : gameState.getAvailableMoves()) {
            if (move.commencedBy().isMrX()) {
                source = move.source();
            }
        }

        ArrayList<Piece> playerList = new ArrayList<>(gameState.getPlayers().asList());
        ArrayList<Move> newMoves = Filter.duplicatePruning(moves, Piece.MrX.MRX);
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(gameState.getSetup().graph, source);
        ArrayListMultimap<Double, Move> finalMap = ArrayListMultimap.create();
        double bestVal = miniMax(dijkstraResult, playerList, gameState, finalMap, newMoves);

        Move chosenMove = null;
        double maxDistance = -1;
        Map<Integer, Double> dijkstraLastLocation = Dijkstra.dijkstraFunction(gameState.getSetup().graph, lastLocation); // dijkstra called on prev location of mrX
        for (Move tempMove : finalMap.get(bestVal)) { // looping through the potential duplicate value moves
            if (maxDistance == -1) { // assign chosenMove to tempMove to prevent null.
                chosenMove = tempMove;
            }
            int destination = tempMove.accept(new Move.Visitor<>() { // use visitor pattern to get the destination of tempMove.
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });
            // pick the move that is furthest away from the last location.
            double distance = dijkstraLastLocation.get(destination);
            if (distance > maxDistance) {
                maxDistance = distance;
                chosenMove = tempMove;
            }
        }

        assert chosenMove != null;
        return chosenMove;
    }

    public static double miniMax(Map<Integer,Double> dijkstraResult, ArrayList<Piece> players, Board.GameState gameState, ArrayListMultimap<Double, Move> finalMap, List<Move> moves) {
        double bestVal = 0;
        double value = 0;
        ArrayList<Piece> tempPlayers = new ArrayList<>(players);

        Piece mover = tempPlayers.get(0); // remove current player
        if (tempPlayers.size() != 1) {
            tempPlayers.remove(0);
        }
        if (tempPlayers.isEmpty()) { // leaf node
            Detective lastPiece = (Detective) mover;
            return dijkstraResult.get(gameState.getDetectiveLocation(lastPiece).get());
        }

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
                moveList = Filter.doubleOrSingleFilter(moves, false);
            }
            if (!(detectiveTotal <= gameState.getPlayers().size() * 2) || moveList.isEmpty()) {
                moveList = Filter.doubleOrSingleFilter(moves, true);
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
                value = miniMax(Dijkstra.dijkstraFunction(newState.getSetup().graph, destination), tempPlayers, newState, finalMap, Filter.duplicatePruning(newMoves, tempPlayers.get(0)));
                finalMap.put(value, move);
                bestVal = Math.max(value, bestVal);
            }
            return bestVal; // max values of all of mrx nodes

        } else { // detective
            bestVal = Double.POSITIVE_INFINITY;
            ArrayList<Move> moveList = getPlayerMoves(moves, mover);
            if (moveList.isEmpty()) {
                return dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get());// if they dont have moves just return the distance to them.
            }
            for (Move move : moveList) {
                Board.GameState newState = gameState.advance(move);
                ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves());
                value = miniMax(dijkstraResult, tempPlayers, newState, finalMap, Filter.duplicatePruning(newMoves, tempPlayers.get(0)));

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

}



