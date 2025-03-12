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

class AiThread extends Thread {
    public Move bestMove;
    public double mapValue;
    public ArrayListMultimap<Double, Move> finalMap;
    private Move move;
    private Board.GameState gameState;
    private ArrayList<Piece> tempPlayers;
    private Map<Integer, Double> dijkstraResult;
    private double value;
    private double alpha;
    private double beta;

    public AiThread(ArrayListMultimap<Double, Move> finalMap, Move move, Board.GameState gameState, ArrayList<Piece> tempPlayers, Map<Integer, Double> dijkstraResult, double alpha, double beta) {
        this.move = move;
        this.gameState = gameState;
        this.tempPlayers = tempPlayers;
        this.dijkstraResult = dijkstraResult;
        this.alpha = alpha;
        this.beta = beta;
        this.value = 0;
        this.mapValue = 0;
        this.bestMove = null;
        this.finalMap = finalMap;
    }

  @Override
  public void run() {
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
      ArrayList<Move> newMoveList = new ArrayList<>();
      for (Piece individualDetectivePiece : gameState.getPlayers().asList()){
          if (individualDetectivePiece.isDetective()){
              newMoveList.addAll(Filter.filterIrrelevantMovesV2(newMoves,individualDetectivePiece,dijkstraResult));
          }
      }
      value = MTNoGraphAi.miniMax(Dijkstra.dijkstraFunction(newState.getSetup().graph, destination), tempPlayers, newState, Filter.duplicatePruning(newMoveList, tempPlayers.get(0)), alpha, beta, finalMap);
      mapValue = value;
      bestMove = move;
  }
}

public class MTNoGraphAi implements Ai {

    ArrayList<Move> mrXMoves = new ArrayList<>();

    @Nonnull @Override public String name() { return "[MRX] MT (No Graph)"; }

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

        //ArrayListMultimap<Move, Integer> movesMultimap = ArrayListMultimap.create();
        //filterIrrelevantMoves(moves,gameState,movesMultimap);
        //System.out.println(movesMultimap);

        ArrayList<Piece> playerList = new ArrayList<>(gameState.getPlayers().asList());
        playerList.add(MrX.MRX);
        playerList.add(Piece.Detective.RED);

        ArrayList<Move> newMoves = Filter.duplicatePruning(moves, Piece.MrX.MRX);
        //newMoves = noRepeatMoves(newMoves);
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(gameState.getSetup().graph, location);
        ArrayListMultimap<Double, Move> finalMap = ArrayListMultimap.create();
        double bestVal = miniMax(dijkstraResult, playerList, gameState, newMoves, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, finalMap);

        Move chosenMove = null;
        double maxDistance = -1;
        Map<Integer, Double> dijkstraLastLocation = Dijkstra.dijkstraFunction(gameState.getSetup().graph, lastLocation);
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

        mrXMoves.add(chosenMove);
        assert chosenMove != null;
        return chosenMove;
    }

    public static double miniMax(Map<Integer,Double> dijkstraResult, ArrayList<Piece> players, Board.GameState gameState, List<Move> moves, double alpha, double beta, ArrayListMultimap<Double, Move> finalMap) {
        double bestVal = 0;
        double value = 0;
        ArrayList<Piece> tempPlayers = new ArrayList<>(players);
        ArrayList<AiThread> threads = new ArrayList<>();

        Piece mover = tempPlayers.get(0); // remove current player
        //tempPlayers.remove(0);
        if (tempPlayers.size() != 1) {
            tempPlayers.remove(0);
        }
        if (tempPlayers.isEmpty()) { // leaf node
            //Detective lastPiece = (Detective) gameState.getPlayers().asList().get(gameState.getPlayers().size() - 1);
            Detective lastPiece = (Detective) mover;
            return dijkstraResult.get(gameState.getDetectiveLocation(lastPiece).get());
        }

        if (mover.isMrX()) {
            bestVal = Double.NEGATIVE_INFINITY;

            double detectiveTotal = 0;
            ArrayList<Move> moveList = new ArrayList<>(moves);
            for (Piece detective : gameState.getPlayers()) {
                if (detective.isDetective()) {
                    detectiveTotal += dijkstraResult.get(gameState.getDetectiveLocation((Detective) detective).get());
                }
            }
            if (detectiveTotal <= gameState.getPlayers().size() * 2) {
                moveList = Filter.doubleOrSingleFilter(moves, false);
            }
            if ((detectiveTotal > gameState.getPlayers().size() * 2) || (moveList.isEmpty())) {
                moveList = Filter.doubleOrSingleFilter(moves, true);
            }

            boolean stillContainsMrx = false;
            for (Piece individualPiece : tempPlayers){
                if (individualPiece.isMrX()){
                    stillContainsMrx = true;
                }
            }
            if (stillContainsMrx) {
                for (Move move : moveList) {
                    //if (tempPlayers.size() > 4) {
                    AiThread newThread = new AiThread(finalMap, move, gameState, tempPlayers, dijkstraResult, alpha, beta);
                    newThread.start();
                    threads.add(newThread); // put outside the loop
                    //}
                }
                for (AiThread IndividualThread : threads) {
                    try{
                        IndividualThread.join();
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                }
                for (AiThread IndividualThread : threads){
                    finalMap.put(IndividualThread.mapValue, IndividualThread.bestMove);
                    bestVal = Math.max(IndividualThread.mapValue,bestVal);
                }
            }
            else {
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
                    Map<Integer, Double> tempDijkstraResult = Dijkstra.dijkstraFunction(newState.getSetup().graph, destination);

                    ArrayList<Move> newMoveList = new ArrayList<>();
                    for (Piece individualDetectivePiece : gameState.getPlayers().asList()) {
                        if (individualDetectivePiece.isDetective()) {
                            newMoveList.addAll(Filter.filterIrrelevantMovesV2(newMoves, individualDetectivePiece, dijkstraResult));
                        }
                    }
                    value = miniMax(tempDijkstraResult, tempPlayers, newState, Filter.duplicatePruning(newMoveList, tempPlayers.get(0)), alpha, beta, finalMap);
                    //if (alpha == Double.NEGATIVE_INFINITY && beta == Double.POSITIVE_INFINITY) finalMap.put(value, move);
                    bestVal = Math.max(value, bestVal);
                    if (bestVal == Double.NEGATIVE_INFINITY){
                        bestVal = 0;
                    }
                }
            }
            if (tempPlayers.size() == 1) {
                tempPlayers.remove(0);
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
                value = miniMax(dijkstraResult, tempPlayers, newState, Filter.duplicatePruning(newMoves, tempPlayers.get(0)), alpha, beta, finalMap);
                bestVal = Math.min(value, bestVal);
//                 beta = Math.min(bestVal + dijkstraResult.get(gameState.getDetectiveLocation((Detective) mover).get()), beta);
//                 if (beta <= alpha){
//                     break;
//                 }
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
            if (!(mrXMoves.isEmpty())) {
                if (!(destination == mrXMoves.get(mrXMoves.size() - 1).source()) || !(moves.size() > 1)) {
                    returnMoves.add(individualMove);
                }
            }
            else{
                returnMoves.add(individualMove);
            }
            //returnMoves.add(individualMove);
        }
        return returnMoves;
    }

}




