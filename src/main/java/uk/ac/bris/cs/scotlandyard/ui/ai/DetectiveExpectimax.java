package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class DetectiveExpectimax implements Ai {

    public static class Node {
        Board.GameState state;
        Node parent;
        ArrayList<Node> children;
        double value;
        int mrXLocation;
        Piece mover;
        private final Move move;

        // constructor for a node
        public Node(Board.GameState state, Node parent, int mrXLocation, Move move, Piece mover) {
            this.state = state;
            this.parent = parent;
            this.children = new ArrayList<>();
            this.mover = mover;
            this.value = 0;
            this.mrXLocation = mrXLocation;
            this.move = move;
        }
    }

    private static Map<Integer, Map<Integer, Double>> dijkstraAll;
    private static ArrayList<Piece> detectives;
    private static ArrayList<Integer> lastLocations;

    @Nonnull
    @Override
    public String name() {return "[DETECTIVE] Expectimax";}

    // Dijkstra for all nodes predetermined
    @Override
    public void onStart() {
        detectives = new ArrayList<>();
        dijkstraAll = new HashMap<>();
        lastLocations = new ArrayList<>(Arrays.asList(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172));
        ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> defaultGraph;
        try {
            defaultGraph = readGraph(Resources.toString(Resources.getResource(
                            "graph.txt"),
                    StandardCharsets.UTF_8));
        } catch (IOException e) { throw new RuntimeException("Unable to read game graph", e); }
        for (int i = 1; i <= 199; i++) {
            dijkstraAll.put(i, Dijkstra.dijkstraFunction(defaultGraph, i));
        }
    }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        HashMap<ScotlandYard.Ticket, Integer> tempTicketMap = new HashMap<>();
        ImmutableMap<ScotlandYard.Ticket, Integer> mrXTicketMap = ImmutableMap.of();
        ArrayList<Integer> mrXPossibleLocations = new ArrayList<>();
        ArrayList<Player> allMrX = new ArrayList<>();
        ArrayList<ScotlandYard.Ticket> tempTicketList = new ArrayList<>(Arrays.asList(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        ArrayList<Piece> tempDetectives = new ArrayList<>();
        Piece firstMover = null;
        Player mrX = null;
        for (Piece piece : board.getPlayers()) {
            for (ScotlandYard.Ticket ticket : tempTicketList) {
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            if (piece.isMrX()) mrXTicketMap = ImmutableMap.copyOf(tempTicketMap);
            if (piece.isDetective()){
                Piece.Detective newDetective = (Piece.Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
                tempDetectives.add(piece);
            }
        }
        detectives = tempDetectives;
        ArrayList<Integer> revealMoves = new ArrayList<>(Arrays.asList(3,8, 13, 18, 24));

        firstMover = board.getAvailableMoves().asList().get(0).commencedBy();
        Player detective = null;
        for (Player detectiveTemp : detectivesList) {
            if (detectiveTemp.piece() == firstMover) {
                detective = detectiveTemp;
            }
        }

        ArrayList<Integer> potentialLocations = new ArrayList<>();

        // making all possible starting states and adding to the root which is just a null root
        int logSize = board.getMrXTravelLog().size();
        if (logSize >= 3) {
            System.out.println(lastLocations);
            if (revealMoves.contains(logSize)) {
                lastLocations = (ArrayList<Integer>) List.of(board.getMrXTravelLog().get(board.getMrXTravelLog().size() - 1).location().get());
            } else { // doesn't contain - lastlocation will be last move
                for (Integer lastLocation : lastLocations) {
                    ScotlandYard.Ticket ticketUsedByMrX = board.getMrXTravelLog().get(logSize- 1).ticket();
                    for (int adjNode : board.getSetup().graph.adjacentNodes(lastLocation)) {
                        System.out.println(adjNode);
                        for (ScotlandYard.Transport transport : board.getSetup().graph.edgeValue(lastLocation, adjNode).get()) {
                            System.out.println(ticketUsedByMrX + " " + transport.requiredTicket());
                            if (transport.requiredTicket() == ticketUsedByMrX || ticketUsedByMrX == ScotlandYard.Ticket.SECRET) {
                                potentialLocations.add(adjNode);
                            }
                        }
                    }
                }
                System.out.println("POTENTIAL" + potentialLocations);
                lastLocations = potentialLocations;
            }

            ArrayList<Node> potentialStates = new ArrayList<>();

            for (int potentialLocation : potentialLocations) {
                Player mrXNew = new Player(Piece.MrX.MRX, mrXTicketMap, potentialLocation);
                Board.GameState newState = factory.build(board.getSetup(), mrXNew, ImmutableList.copyOf(detectivesList));
                Node potentialState = new Node(newState, null, potentialLocation, null, null);
                potentialStates.add(potentialState);
            }

            for (Node potentialState : potentialStates) {
                for (Move move : potentialState.state.getAvailableMoves()) {
                    Board.GameState newState = potentialState.state.advance(move);
                    System.out.println(move);
                    Node child = new Node(newState, potentialState, getMrXLocationFromMove(move), move, Piece.MrX.MRX);
                    potentialState.children.add(child);
                }
            }

            for (Node potentialState : potentialStates) {
                for (Node child : potentialState.children) {
                    buildNodes(child, 0, 0, new ArrayList<>(board.getAvailableMoves()));
                }
            }

            for (Node potentialState : potentialStates) {
                for (Node child : potentialState.children) {
                    potentialState.value += expectiMax(child, 0);
                }
            }

            Move bestMove = null;
            double bestVal = Double.POSITIVE_INFINITY;
            for (Node potentialState : potentialStates) {
                System.out.println(potentialState.value);
                if (potentialState.value < bestVal) {
                    bestVal = potentialState.value;
                    bestMove = potentialState.move;
                }
            }
            System.out.println(board.getAvailableMoves());
            assert bestMove != null;
            return bestMove;
        } else {
            return maxDistanceToDetectives(board, detectivesList, detective); // just spread out
        }

    }

    public static int getMrXLocationFromMove(Move move) {
        return move.accept(new Move.Visitor<>() { // visitor to get the destination
            @Override
            public Integer visit(Move.SingleMove move) {
                return move.destination;
            }

            @Override
            public Integer visit(Move.DoubleMove move) {
                return move.destination2;
            }
        });
    }

    public static Integer getMovesSinceLastReveal(Player mrX, Board board) {
        ArrayList<Integer> lastReveal = new ArrayList<>();
        ArrayList<Integer> revealMoves = new ArrayList<>(Arrays.asList(3,8, 13, 18, 24));
        for (int integer : revealMoves) {
            assert mrX != null;
            if (mrX.location() >= integer) {
                lastReveal.add(integer);
            }
        }
        int lastRevealMove = lastReveal.get(0);

        int movesSinceLastReveal = board.getMrXTravelLog().size() - lastRevealMove;
        return movesSinceLastReveal;
    }

    public static Integer getLastMrXRevealLocation(Board board) {
        int location = -1;
        for (LogEntry entry : board.getMrXTravelLog()) {
            if (entry.location().isPresent()) {
                location = entry.location().get();
            }
        }
        return location;
    }

    public static Move maxDistanceToDetectives(Board board, ArrayList<Player> detectivesList, Player currentDetective){
        //int source = currentDetective.location();
        Map<Double, Move> moveMap = new HashMap<>();
        for (Move individualMove : board.getAvailableMoves()){
            double moveTotal = 0;
            if (individualMove.commencedBy() == currentDetective.piece()){
                int destination = individualMove.accept(new Move.Visitor<>() { // visitor to get the destination
                    @Override
                    public Integer visit(Move.SingleMove move) {
                        return move.destination;
                    }

                    @Override
                    public Integer visit(Move.DoubleMove move) {
                        return move.destination2;
                    }
                });
                Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(board.getSetup().graph, destination);
                for (Player individualDetective : detectivesList){
                    moveTotal += dijkstraResult.get(individualDetective.location());
                }
                moveMap.put(moveTotal,individualMove);
            }
        }
        // getting the max value of the keys and using it to get the best move.
        return moveMap.get(moveMap.keySet().stream().max(Double::compareTo).get());
    }

    public void buildNodes(Node root, int depth, int detectiveIndex, ArrayList<Move> moves) {
        if (depth == 4) return;

        Piece mover = detectives.get(detectiveIndex);

        for (Move move : moves) {
            if (move.commencedBy() == mover) {
                Board.GameState newState = root.state.advance(move);
                Node child = new Node(newState, root, root.mrXLocation, move, mover);
                root.children.add(child);
                buildNodes(child, depth + 1, detectiveIndex + 1, new ArrayList<>(newState.getAvailableMoves()));
            }
        }
    }

    public double expectiMax(Node node, int depth) {
        double bestVal = 0;
        if (depth == 2 && !(node.state.getWinner().isEmpty())) {
            return distanceToMrX(node.mrXLocation, node.state.getDetectiveLocation((Piece.Detective) node.mover).get());
        }

        if (node.mover.isMrX()) {
            bestVal = Double.POSITIVE_INFINITY;
            for (Node child : node.children) {
                bestVal += Math.min(bestVal, expectiMax(child, depth+1));
            }
        } else {
            bestVal = Double.NEGATIVE_INFINITY;
            for (Node child : node.children) {
                bestVal = Math.max(bestVal, expectiMax(child, depth + 1));
            }
        }
        return bestVal;
    }

    public static double distanceToMrX(int mrXLocation, int detectiveLocation) {
        return dijkstraAll.get(detectiveLocation).get(mrXLocation);
    }
}
