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

public class NoGraphAiTotal implements Ai {

    ArrayList<Move> mrXMoves = new ArrayList<>();

    @Nonnull @Override public String name() { return "[MRX] 6 layer total (No Graph)"; }

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

        ArrayList<Move> moves = new ArrayList<Move>(gameState.getAvailableMoves().asList());
        ArrayList<Piece> playerList = new ArrayList<>(gameState.getPlayers().asList());

        ArrayList<Move> newMoves = Filter.duplicatePruning(moves, Piece.MrX.MRX);
        newMoves = noRepeatMoves(newMoves);
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(gameState.getSetup().graph, location);
        ArrayListMultimap<Double, Move> finalMap = ArrayListMultimap.create();
        double bestVal = miniMax(dijkstraResult, playerList, gameState, finalMap, newMoves, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        Move chosenMove = null;
        double maxDistance = -1;
        Map<Integer, Double> dijkstraLastLocation = Dijkstra.dijkstraFunction(gameState.getSetup().graph, lastLocation); // dijkstra's called on prev location of mrX
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

        mrXMoves.add(chosenMove);
        assert chosenMove != null;
        return chosenMove;
    }

    public static double miniMax(Map<Integer,Double> dijkstraResult, ArrayList<Piece> players, Board.GameState gameState, ArrayListMultimap<Double, Move> finalMap, List<Move> moves, double alpha, double beta) {
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
            //if the detectives average a distance of 2 from mrX, then he can use double moves
            for (Piece detective : gameState.getPlayers()) {
                if (detective.isDetective()) {
                    detectiveTotal += dijkstraResult.get(gameState.getDetectiveLocation((Detective) detective).get());
                }
            }
            if (detectiveTotal <= gameState.getPlayers().size() * 2) {
                moveList = Filter.doubleOrSingleFilter(moves, false);
            }
            if (detectiveTotal > gameState.getPlayers().size() * 2 || (moveList.isEmpty())) {
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
                //newMoves = Filter.duplicatePruning(newMoves);
                Map<Integer,Double> tempDijkstraResult = Dijkstra.dijkstraFunction(newState.getSetup().graph, destination);

                ArrayList<Move> newMoveList = new ArrayList<>();
                for (Piece individualDetectivePiece : gameState.getPlayers().asList()){
                    if (individualDetectivePiece.isDetective()){
                        newMoveList.addAll(Filter.filterIrrelevantMoves(newMoves,individualDetectivePiece,dijkstraResult));
                    }
                }

                value = miniMax(tempDijkstraResult, tempPlayers, newState, finalMap, Filter.duplicatePruning(newMoveList, tempPlayers.get(0)), alpha, beta);
                if (alpha == Double.NEGATIVE_INFINITY && beta == Double.POSITIVE_INFINITY) finalMap.put(value, move);
                bestVal = Math.max(value, bestVal);
//                alpha = Math.max(bestVal, alpha);
//                if (beta <= alpha) {
//                    //System.out.println("mrX break");
//                    break;
//                }
            }
            if (tempPlayers.size() == 1) {
                tempPlayers.remove(0);
            }
            return bestVal; // max values of all of mrx nodes

        } else { // detective
            bestVal = Double.POSITIVE_INFINITY;
            ArrayList<Move> moveList = getPlayerMoves(moves, mover);
            if (moveList.isEmpty()) {
                return dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get());// if they don't have moves just return the distance to them.
            }
            for (Move move : moveList) {
                Board.GameState newState = gameState.advance(move);
                ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves());
                value = miniMax(dijkstraResult, tempPlayers, newState, finalMap, Filter.duplicatePruning(newMoves, tempPlayers.get(0)), alpha, beta);
                bestVal = Math.min(value, bestVal);
                beta = Math.min(bestVal + dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get()), beta);
//                if (beta <= alpha){
//                    //System.out.println("Detective break");
//                    break;
//                }
            }
            if (tempPlayers.size() == 1) {
                tempPlayers.remove(0);
            }
            return bestVal + dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get()); // Min of all the 'children' values added to the 'parent' value, is returned
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

    // stops mrX from going back to the same location he just came from, because moving more, reduces likelihood of being trapped
    public ArrayList<Move> noRepeatMoves(ArrayList<Move> moves){
        ArrayList<Move> returnMoves = new ArrayList<>();
        for (Move individualMove : moves){
            int destination = individualMove.accept(new Move.Visitor<Integer>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            });
            if (!(mrXMoves.isEmpty()) && (moves.size() > 1)) {
                if (!(destination == mrXMoves.get(mrXMoves.size() - 1).source())) {
                    returnMoves.add(individualMove);
                }
            }
            else{
                returnMoves.add(individualMove);
            }
        }
        return returnMoves;
    }

}




