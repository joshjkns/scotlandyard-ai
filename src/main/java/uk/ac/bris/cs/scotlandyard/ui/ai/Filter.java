package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.sql.Array;
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
//    public static ArrayList<Move> duplicatePruningOld(List<Move> moves) {
//        Map.Entry<Integer, Boolean> entry;
//        Map<Integer, Move> singleMoveMap = new HashMap<>();
//        Map<Integer, Move> doubleMoveMap = new HashMap<>();
//        Collections.shuffle(moves); // so he doesn't use the secret x2 always first.
//        for (Move move : moves) {
//            entry = move.accept(new Move.Visitor<Map.Entry<Integer, Boolean>>() {
//                @Override
//                public Map.Entry<Integer, Boolean> visit(Move.SingleMove move) {
//                    return new AbstractMap.SimpleEntry<>(move.destination, true);
//                }
//
//                @Override
//                public Map.Entry<Integer, Boolean> visit(Move.DoubleMove move) {
//                    return new AbstractMap.SimpleEntry<>(move.destination2, false);
//                }
//            });
//            int destination = entry.getKey();
//            boolean singleMove = entry.getValue();
//            if (singleMove) {
//                singleMoveMap.put(destination, move);
//            }
//            // removing double moves that go to and back to the same spot
//            if (!singleMove && (destination != move.source())) {
//                doubleMoveMap.put(destination, move);
//            }
//        }
//        for (int tempDestination : doubleMoveMap.keySet()) {
//            if (!(singleMoveMap.containsKey(tempDestination))) {
//                singleMoveMap.put(tempDestination, doubleMoveMap.get(tempDestination));
//            }
//        }
//        return new ArrayList<>(singleMoveMap.values());
//    }
    public static ArrayList<Move> duplicatePruning(List<Move> moves, Piece mover) {
        Map.Entry<Integer, Boolean> entry;
        ArrayList<ArrayList<Move>> secretFilter = new ArrayList<>();
        ArrayList<Move> tempSecretFilter = new ArrayList<>();
        Map<Integer, Move> singleMoveMap = new HashMap<>();
        Map<Integer, Move> doubleMoveMap = new HashMap<>();
        Collections.shuffle(moves);
        for (Move move : moves) {
            if (!(move.commencedBy() == mover)) {
                continue; // go to next move
            }
            entry = move.accept(new Move.Visitor<Map.Entry<Integer, Boolean>>() {
                @Override
                public Map.Entry<Integer, Boolean> visit(Move.SingleMove move) {
                    return new AbstractMap.SimpleEntry<>(move.destination, true);
                }

                @Override
                public Map.Entry<Integer, Boolean> visit(Move.DoubleMove move) {
                    return new AbstractMap.SimpleEntry<>(move.destination2, false);
                }
            });
            int destination = entry.getKey();
            boolean singleMove = entry.getValue();
            if (singleMove) {
                singleMoveMap.put(destination, move);
                tempSecretFilter.add(move);
            }
            // removing double moves that go to and back to the same spot
            if (!singleMove && (destination != move.source())) {
                doubleMoveMap.put(destination, move);
            }
        }
        //System.out.println(tempSecretFilter);
        //singleMoveMap = secretPruning(tempSecretFilter);
        //System.out.println(singleMoveMap);
        for (int tempDestination : doubleMoveMap.keySet()) {
            if (!(singleMoveMap.containsKey(tempDestination))) {
                singleMoveMap.put(tempDestination, doubleMoveMap.get(tempDestination));
            }
        }
        return new ArrayList<>(singleMoveMap.values());
    }

    public static Map<Integer, Move> secretPruning(List<Move> moves) {
        ArrayList<Move> secondLoopMoves = new ArrayList<>(moves);
        ArrayList<ArrayList<Move>> secretFilter = new ArrayList<>();
        //ArrayList<Move> returnMoves = new ArrayList<>();
        Map<Integer, Move> returnMap = new HashMap<>();
        for (Move firstMove : moves) {
            Move.SingleMove firstSingleMove = (Move.SingleMove) firstMove;
            for (Move secondMove : secondLoopMoves) {
                Move.SingleMove secondSingleMove = (Move.SingleMove) secondMove;
                if ((firstSingleMove.destination == secondSingleMove.destination) && (firstSingleMove != secondSingleMove)) {
                    ArrayList<Move> tempSecretFilter = new ArrayList<>();
                    tempSecretFilter.add(firstMove);
                    tempSecretFilter.add(secondMove);
                    secretFilter.add(tempSecretFilter);
                }
            }
            secondLoopMoves.remove(firstMove);
        }
        System.out.println("bellow");
        System.out.println(secretFilter);
        for (ArrayList<Move> pair : secretFilter) {
            for (Move indivdualMove : pair) {
            }
            Move.SingleMove m1 = (Move.SingleMove) pair.get(0);
            Move.SingleMove m2 = (Move.SingleMove) pair.get(1);
            if ((m1.ticket == ScotlandYard.Ticket.BUS) || (m1.ticket == ScotlandYard.Ticket.TAXI)) {
                returnMap.put(m1.destination, m1);
            }
            if (m1.ticket == ScotlandYard.Ticket.UNDERGROUND) {
                //System.out.println("yo");
                returnMap.put(m2.destination, m2);
            }
            if ((m2.ticket == ScotlandYard.Ticket.BUS) || (m2.ticket == ScotlandYard.Ticket.TAXI)) {
                returnMap.put(m2.destination, m2);
            }
            if (m2.ticket == ScotlandYard.Ticket.UNDERGROUND) {
                returnMap.put(m1.destination, m1);
            }
            if ((m2.ticket == ScotlandYard.Ticket.SECRET) && (m1.ticket == ScotlandYard.Ticket.SECRET)) {
                returnMap.put(m1.destination, m1);
            }
        }
        return returnMap;
    }

    public static ArrayList<Move> filterIrrelevantMoves(List<Move> moves, Piece playerPiece, Map<Integer, Double> dijkstraResult) {
        ArrayList<Move> returnMoves = new ArrayList<>();      //??if mrX is at least 2 away from the closest move destination?? <- dunno if it's needed, and the edge is 2 away from the closest move destination, then eliminate if mrX moves are only single moves, hence all the detectives are not close to each-other, is such a way that their interaction would affect the miniMax tree
        ArrayList<Move> temporaryMoves = new ArrayList<>();
        for (Move individualMove : moves) {
            if (individualMove.commencedBy() == playerPiece) {
                temporaryMoves.add(individualMove);
            }
        }
        if (!temporaryMoves.isEmpty()) {
            double smallest = Double.POSITIVE_INFINITY;
            for (Move individualMove : temporaryMoves) {
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
                if (dijkstraResult.get(destination) < smallest) {
                    smallest = dijkstraResult.get(destination);
                }
            }

            for (Move individualMove : temporaryMoves) {
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
                if ((dijkstraResult.get(destination) - 2) < smallest) {
                    returnMoves.add(individualMove);
                }
            }
        }
        return returnMoves;

//        if (temporaryMoves.size() <= 3) {
//            return temporaryMoves;
//        }
//        else{
//            temporaryMoves3 = temporaryMoves;
//            for (Move irrelevantMove : temporaryMoves ) {
//                double smallest = Double.POSITIVE_INFINITY;
//                Move smallestMove = null;
//                for (Move individualMove : temporaryMoves3) {
//                    int destination = individualMove.accept(new Move.Visitor<Integer>() {
//                        @Override
//                        public Integer visit(Move.SingleMove move) {
//                            return move.destination;
//                        }
//
//                        @Override
//                        public Integer visit(Move.DoubleMove move) {
//                            return move.destination2;
//                        }
//                    });
//                    if (dijkstraResult.get(destination) < smallest) {
//                        smallest = dijkstraResult.get(destination);
//                        smallestMove = individualMove;
//                    }
//                }
//                temporaryMoves2.add(smallestMove);
//                temporaryMoves3.remove(smallestMove);
//            }
//            int minDestination = temporaryMoves2.get(0).accept(new Move.Visitor<Integer>() {
//                @Override
//                public Integer visit(Move.SingleMove move) {
//                    return move.destination;
//                }
//
//                @Override
//                public Integer visit(Move.DoubleMove move) {
//                    return move.destination2;
//                }
//            });
//            double smallestMove = dijkstraResult.get(minDestination);
//            int count = 1;
//            for (Move individualMove : temporaryMoves2){
//                int destination = individualMove.accept(new Move.Visitor<Integer>() {
//                    @Override
//                    public Integer visit(Move.SingleMove move) {
//                        return move.destination;
//                    }
//
//                    @Override
//                    public Integer visit(Move.DoubleMove move) {
//                        return move.destination2;
//                    }
//                });
//                if ((count <= 3) || (dijkstraResult.get(destination) == smallestMove)){
//                    returnMoves.add(individualMove);
//                }
//                count++;
//            }
//        }
        //return returnMoves;
    }

    public static ArrayList<Move> killerMoves(ArrayList<Move> mrXmoves, ArrayList<Integer> detectivesLocations, Map<Integer, Map<Integer, Double>> dijkstraAll, ArrayList<Piece> Players, Board.GameState gameState) {
        int couldBeKilled = 0;
        ArrayList<Move> returnMoves = new ArrayList<>(Filter.doubleOrSingleFilter(mrXmoves,true));
        //Map<Integer, Double> dijsktrasFromMrxPos = dijkstraAll.get(mrXmoves.get(0).source());
        for (Move individualMove : returnMoves) {
            Move.SingleMove mrXtemp = (Move.SingleMove) individualMove;
            Board.GameState newState = gameState.advance(individualMove);
            for (Move detectiveMove : newState.getAvailableMoves()) {
                Move.SingleMove DetectiveTemp = (Move.SingleMove) detectiveMove;
                if (DetectiveTemp.destination == mrXtemp.destination) {
                    System.out.println(detectiveMove.commencedBy());
                    couldBeKilled += 1;
                }
            }
        }

        if (couldBeKilled > 1){
            System.out.println("hi");
            double bestTotal = Double.NEGATIVE_INFINITY;
            Move bestMove = null;
            for (Move doubleMove : Filter.doubleOrSingleFilter(gameState.getAvailableMoves().asList(), false)) {
                double tempTotal = 0;
                Move.DoubleMove mrXtemp = (Move.DoubleMove) doubleMove;
                Map<Integer,Double> tempDijkstras = dijkstraAll.get(mrXtemp.destination2); // was originally from source rather than where he
                System.out.println(tempDijkstras);
                for (Piece playerPiece : Players) { // was going to be after the move
                    if (playerPiece.isDetective()){
                        for (Integer detectivesLocation : detectivesLocations) {
                            tempTotal += tempDijkstras.get(detectivesLocation);
                        }
                    }
                }
                if (tempTotal > bestTotal){
                    bestMove = doubleMove;
                    bestTotal = tempTotal;
                }
            }
            if (bestMove != null){
                returnMoves.add(bestMove);
            }
        }
        return returnMoves;
    }
}
