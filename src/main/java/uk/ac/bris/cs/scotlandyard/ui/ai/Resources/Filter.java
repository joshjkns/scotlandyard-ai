package uk.ac.bris.cs.scotlandyard.ui.ai.Resources;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

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

    public static ArrayList<Move> duplicatePruning(List<Move> moves, Piece mover) {
        Map.Entry<Integer, Boolean> entry;
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
            }
            // removing double moves that go to and back to the same spot
            if (!singleMove && (destination != move.source())) {
                doubleMoveMap.put(destination, move);
            }
        }
        for (int tempDestination : doubleMoveMap.keySet()) {
            if (!(singleMoveMap.containsKey(tempDestination))) {
                singleMoveMap.put(tempDestination, doubleMoveMap.get(tempDestination));
            }
        }
        return new ArrayList<>(singleMoveMap.values());
    }

    public static ArrayList<Move> secretPruning(List<Move> moves) {
        ArrayList<Move> returnMoves = new ArrayList<>();
        Map<Integer, ArrayList<Move.SingleMove>> destinationsAndMoves = new HashMap<>();
        ArrayList<Move> singleMoveList = doubleOrSingleFilter(moves, true);
        ArrayList<Move> doubleMoveList = doubleOrSingleFilter(moves, false);
        for (Move move : singleMoveList) {
            Move.SingleMove singleMoveVersion = (Move.SingleMove) move;
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
            if (destinationsAndMoves.containsKey(destination)) {
                destinationsAndMoves.get(destination).add(singleMoveVersion);
            } else {
                ArrayList<Move.SingleMove> tempList = new ArrayList<>();
                tempList.add(singleMoveVersion);
                destinationsAndMoves.put(destination, tempList);
            }
        }
        for (ArrayList<Move.SingleMove> movesList : destinationsAndMoves.values()) {
            ArrayList<Move.SingleMove> orderedMovesList = new ArrayList<>();
            orderedMovesList.addAll(movesList.stream().filter(move -> move.ticket.equals(ScotlandYard.Ticket.TAXI)).toList());
            orderedMovesList.addAll(movesList.stream().filter(move -> move.ticket.equals(ScotlandYard.Ticket.BUS)).toList());
            orderedMovesList.addAll(movesList.stream().filter(move -> move.ticket.equals(ScotlandYard.Ticket.SECRET)).toList());
            orderedMovesList.addAll(movesList.stream().filter(move -> move.ticket.equals(ScotlandYard.Ticket.UNDERGROUND)).toList());
            returnMoves.add(orderedMovesList.get(0));
        }
        returnMoves.addAll(doubleMoveList);
        return returnMoves;
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
        for (Move individualMove : returnMoves) {
            Move.SingleMove mrXTemp = (Move.SingleMove) individualMove;
            Board.GameState newState = gameState.advance(individualMove);
            for (Move detectiveMove : newState.getAvailableMoves()) {
                Move.SingleMove DetectiveTemp = (Move.SingleMove) detectiveMove;
                if (DetectiveTemp.destination == mrXTemp.destination) {
                    couldBeKilled += 1;
                }
            }
        }

        if (couldBeKilled > 1){
            double bestTotal = Double.NEGATIVE_INFINITY;
            Move bestMove = null;
            for (Move doubleMove : Filter.doubleOrSingleFilter(gameState.getAvailableMoves().asList(), false)) {
                double tempTotal = 0;
                Move.DoubleMove mrXTemp = (Move.DoubleMove) doubleMove;
                Map<Integer,Double> tempDijkstras = dijkstraAll.get(mrXTemp.destination2); // was originally from source rather than where he
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
