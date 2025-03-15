package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

import javax.annotation.Nonnull;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DetectiveMCTSAi implements Ai {

    public class DetectiveMCTSThread extends Thread {
        Node child;
        int iterations;
        Piece mover;
        ArrayList<Move> finalMoveList;

        public DetectiveMCTSThread(Node child, int iterations, Piece mover) {
            this.child = child;
            this.iterations = iterations;
            this.mover = mover;
        }

        @Override
        public void run() {
            finalMoveList = mcts(child, iterations, mover);
        }

        public ArrayList<Move> getFinalMoveList() {
            return finalMoveList;
        }
    }

    public static class Node {
        Board.GameState state;
        Node parent;
        ArrayList<Node> children;
        int visits;
        double value;
        ArrayList<Move> possibleMoves;
        int mrXLocation;
        Piece mover;
        private final Move move;

        // constructor for a node
        public Node(Board.GameState state, Node parent, int mrXLocation, Move move, Piece mover) {
            this.state = state;
            this.parent = parent;
            this.children = new ArrayList<>();
            if (state == null) {
                this.possibleMoves = null;
            } else {
                this.possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
                this.possibleMoves = Filter.duplicatePruning(this.possibleMoves, mover);
            }
            this.visits = 0;
            this.value = 0;
            this.mover = mover;
            this.mrXLocation = mrXLocation;
            this.move = move;
        }

        // function to get the best uct (Upper Confidence bounds applied to Trees)
        // used formula from wikipedia
        public Node bestUCT() {
            Node chosen = null;
            double bestUCT = Double.NEGATIVE_INFINITY;
            for (Node child : children) {
                if (child.visits == 0) return child;
                double nodeUCTValue = child.value / child.visits + Math.sqrt(2 * Math.log(this.visits) / child.visits);

                if (nodeUCTValue > bestUCT) {
                    bestUCT = nodeUCTValue;
                    chosen = child;
                }
            }
            return chosen;
        }

        // getter for the gameState
        public Board.GameState getState() {
            return this.state;
        }

        // find unvisited children nodes from a parent node
        public Node chooseUnvisited() {
            for (Node child: children) {
                if (child.visits == 0) {
                    return child;
                }
            }
            return null;
        }

        // expansion function for mcts - one move (child) at a time
        public Node expand() {
            if (possibleMoves.isEmpty() || hasWinner(this)) {
                return this;
            }

            List<Move> availableMoves = new ArrayList<>(possibleMoves);
            int possibleMovesSize = availableMoves.size();

            Random random = new Random();
            int randomMoveIndex = random.nextInt(possibleMovesSize);

            Move move = availableMoves.get(randomMoveIndex);
            possibleMoves.remove(move);

            Board.GameState newState = state.advance(move);

            int newMrXLocation = this.mrXLocation;
            if (move.commencedBy().isMrX()) {
                newMrXLocation = getMrXLocationFromMove(move);
            }
            Node child = new Node(newState, this, newMrXLocation, move, move.commencedBy());
            children.add(child);
            return child;
        }
    }

    @Nonnull @Override public String name() { return "[Detective] MCTS"; }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        HashMap<ScotlandYard.Ticket, Integer> tempTicketMap = new HashMap<>();
        ArrayList<Integer> mrXPossibleLocations = new ArrayList<>(Arrays.asList(35, 45, 51, 71, 78, 104, 106, 127, 132, 166, 170, 172));
        ArrayList<Player> allMrX = new ArrayList<>();
        ArrayList<ScotlandYard.Ticket> tempTicketList = new ArrayList<>(Arrays.asList(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        Piece firstMover = null;
        Player mrX = null;
        for (Piece piece : board.getPlayers()) {
            for (ScotlandYard.Ticket ticket : tempTicketList) {
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            if (piece.isMrX()){
                if (board.getMrXTravelLog().size() >= 3) {
                    int location = getLastMrXRevealLocation(board);
                    if (location != -1) mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);

                } else {
                    for (int possibleLocation : mrXPossibleLocations) {
                        Player mrXTemp = new Player(piece, ImmutableMap.copyOf(tempTicketMap), possibleLocation);
                        allMrX.add(mrXTemp);
                    }
                }
            } else {
                Piece.Detective newDetective = (Piece.Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
            }
        }
        firstMover = board.getAvailableMoves().asList().get(0).commencedBy();
        System.out.println(firstMover + "\n");

        ArrayList<Node> rootNodes = new ArrayList<>();

        // making all possible starting states and adding to the root which is just a null root
        if (board.getMrXTravelLog().size() >= 3) {
            Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));
            Node root = new Node (gameState, null, mrX.location(), null, Piece.MrX.MRX);

            ArrayList<Move> finalMoveList = mcts(root, 1000, firstMover);
            double bestFreq = Double.NEGATIVE_INFINITY;
            Move bestMove = null;
            for (Move move : finalMoveList) {
                double freq = Collections.frequency(finalMoveList, move);
                if (freq > bestFreq) {
                    bestFreq = freq;
                    bestMove = move;
                }
            }
            assert bestMove != null;
            return bestMove;

        } else {
            for (Player mrXTemp : allMrX) {
                Board.GameState gameState = factory.build(board.getSetup(), mrXTemp, ImmutableList.copyOf(detectivesList));
                Node root = new Node (gameState,null, mrXTemp.location(), null, null); // each one of these has mrx moves in the state
                rootNodes.add(root);
            }

            List<Move> finalSetOfMoves = new ArrayList<>();

            ArrayList<DetectiveMCTSThread> threads = new ArrayList<>();

            for (Node child : rootNodes) {
                DetectiveMCTSThread thread = new DetectiveMCTSThread(child, 200, firstMover);
                threads.add(thread);
                thread.start();
            }

            for (DetectiveMCTSThread thread : threads) {
                try {
                    thread.join();
                    finalSetOfMoves.addAll(thread.getFinalMoveList());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Move bestMove = null;
            double bestFreq = Double.NEGATIVE_INFINITY;

            for (Move move: finalSetOfMoves) {
                double freq = Collections.frequency(finalSetOfMoves, move);
                if (freq > bestFreq) {
                    bestFreq = freq;
                    bestMove = move;
                }
            }

            assert bestMove != null;
            return bestMove;
        }
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

    public static boolean hasWinner(Node node) {
        return (!node.state.getWinner().isEmpty());
    }

    public static boolean isExpanded(Node node) {
        return !node.children.isEmpty();
    }

    // main mcts function to search over n iterations and return the best move
    public static ArrayList<Move> mcts(Node root, int iterations, Piece mover) {
        int currentIterations = 0;

        ArrayList<Move> rootMoves = new ArrayList<>(root.state.getAvailableMoves().asList());

        // initialise all moves off each root node - mrx moves
        for (Move move : rootMoves) {
            Board.GameState newState = root.state.advance(move);
            int newMrXLocation = root.mrXLocation; // have to pick from a list of possible locations he could be in.
            if (move.commencedBy().isMrX()) {
                newMrXLocation = getMrXLocationFromMove(move);
            }
            Node child = new Node(newState, root, newMrXLocation, move, move.commencedBy());
            root.children.add(child);
        }

        ArrayList<Move> finalMovesList = new ArrayList<>();

        // loop through iterations
        for (Node child : root.children) {
            for (Move move : child.state.getAvailableMoves()) {
                Board.GameState newState = child.state.advance(move);
                if (move.commencedBy() == mover) {
                    Node grandChild = new Node(newState, child, child.mrXLocation, move, mover);
                    child.children.add(grandChild);
                }
            }
            System.out.println(child.children);
            while (currentIterations < iterations) {
                Node leaf = traverse(child); // get leaf node via traverse function
                double value = playMove(leaf); // playMove function - random moves until game is over
                backpropagate(leaf, value); // backpropagate through tree
                currentIterations++; // iterate
            }
            Move finalMove = getMove(child);
            if (finalMove.commencedBy() == mover) {
                finalMovesList.add(finalMove);
            }

        }

        return finalMovesList;
    }

    // traverse function to traverse the tree
    public static Node traverse(Node node) {
        while (isExpanded(node) && !hasWinner(node)) {
            Node bestUCT = node.bestUCT(); // pick best uct and check it is not null
            if (bestUCT == null) {
                break;
            }
            node = bestUCT;
        }

        Node unvisited = node.chooseUnvisited(); // pick unvisited node and return if not null
        if (unvisited != null) {
            return unvisited;
        }

        if (!isExpanded(node)) { // check if fully expanded, if not then expand the node
            return node.expand();
        }

        return node;
    }

    // getting mrx location to keep nodes updated using visitor
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

    // back-propagating through tree to assign values to nodes
    public static void backpropagate(Node node, double value) {
        while (node != null) { // until node parent = null which is the root node
            node.visits += 1; // add one to visit
            node.value += value; // add to the nodes value
            node = node.parent; // move to its parent and loop again
        }
    }

    // final move pick function
    public static Move getMove(Node root) {
        Node bestChild = null;
        double bestVal = Double.POSITIVE_INFINITY;
        // ranking based on visits over values
        for (Node child : root.children) { // for child of the root node (mrx moves)
            if (child.value < bestVal) { // find the one that has the most visits
                bestVal = child.value; // that will be the bestChild / bestMove
                bestChild = child;
            }
        }
        ArrayList<Move> moves = new ArrayList<>(root.state.getAvailableMoves());

        // if there is no best child - root has no children
        if (bestChild == null) {
            if (!moves.isEmpty()) { // pick a random move
                return moves.get(new Random().nextInt(moves.size()));
            }
            return null;
        }
        // for move in the move array if the bestChild's move matches the move in the array, return it.
        for (Move move : moves) {
            if (bestChild.move == move) {
                return move;
            }
        }
        return null;
    }

    // play the move to get the value of the move
    public static double playMove(Node node) {
        Board.GameState state = node.getState();
        int mrXcurrentLocation = node.mrXLocation;

        // loop through until win or until you reach the max number of moves
        while (state.getWinner().isEmpty()) {
            ArrayList<Move> possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
            if (possibleMoves.isEmpty()) break;

            Move move = null;
            // assign move to a random one
            int random = new Random().nextInt(possibleMoves.size());
            move = possibleMoves.get(random);

            state = state.advance(move); // advance the gameState
            if (move.commencedBy().isMrX()) mrXcurrentLocation = getMrXLocationFromMove(move);
        }
        // return the value of the state - basically how good the state is
        return getValue(state, mrXcurrentLocation);
    }

    // high values if a game ending outcome, else use dijkstra
    public static double getValue(Board.GameState state, int mrXLocation) {
        if (!state.getWinner().isEmpty()) {
            if (state.getWinner().contains(Piece.MrX.MRX)) {
                return 50; // mrx wins so 50
            } else {
                return -50; // mrx loses so -50
            }
        }
        // else just calculate the distance to the closest detective and use that as the value
        return closestDetectiveDistance(state, mrXLocation);
    }

    // closest detective to mrx - considering one not all.
    public static double closestDetectiveDistance(Board.GameState state, int mrXLocation) {
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(state.getSetup().graph, mrXLocation);
        double closestDistance = Double.POSITIVE_INFINITY;
        // just loop through and keep track of the closest distance
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                closestDistance = Math.min(closestDistance, dijkstraResult.get(state.getDetectiveLocation((Piece.Detective) piece).get()));
            }
        }
        return closestDistance;
    }

    // mrx distance from the detective
//    public double mrXDistance(Board.GameState state, int mrXLocation, Piece.Detective mover) {
//        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(state, mrXLocation);
//        return dijkstraResult.get(state.getDetectiveLocation(mover).get());
//    }

    // function to get the best moves from an array of moves
//    public Move bestMoveBasedOnClosestDetective(Board.GameState state, ArrayList<Move> possibleMoves, int mrXLocation) {
//        Move bestMove = null;
//        double minDistance = Double.NEGATIVE_INFINITY;
//        double maxDistance = Double.POSITIVE_INFINITY;
//        for (Move move : possibleMoves) {
//            Board.GameState newState = state.advance(move);
//            if (move.commencedBy().isMrX()) {
//                mrXLocation = getMrXLocationFromMove(move);
//                double closestDetective = closestDetectiveDistance(newState, mrXLocation);
//                if (closestDetective > minDistance) {
//                    bestMove = move;
//                    minDistance = closestDetective;
//                }
//            } else { // detective move
//                double mrXDistance = mrXDistance(newState, mrXLocation, (Piece.Detective) move.commencedBy());
//                if (mrXDistance < maxDistance) { // want detectives to get as close as possible
//                    bestMove = move;
//                    maxDistance = mrXDistance;
//                }
//            }
//        }
//        return bestMove;
//    }

}
