package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MCTSAi implements Ai {

    public class Node {
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
            this.possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
            this.possibleMoves = Filter.duplicatePruning(possibleMoves, mover);
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

    @Nonnull @Override public String name() { return "[MRX] MCTS"; }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        HashMap<ScotlandYard.Ticket, Integer> tempTicketMap = new HashMap<>();
        ArrayList<ScotlandYard.Ticket> tempTicketList = new ArrayList<>(Arrays.asList(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        Player mrX = null;
        int location = 0;
        for (Piece piece : board.getPlayers()) {
            for (ScotlandYard.Ticket ticket : tempTicketList) {
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            if (piece.isMrX()){
                location = board.getAvailableMoves().asList().get(0).source();
                mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
            } else {
                Piece.Detective newDetective = (Piece.Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
            }
        }
        Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

        Node root = new Node (gameState,null, location, null, Piece.MrX.MRX);
        return mcts(root, 1000);
    }

    public static boolean hasWinner(Node node) {
        return (!node.state.getWinner().isEmpty());
    }

    public static boolean isExpanded(Node node) {
        return !node.children.isEmpty();
    }

    // main mcts function to search over n iterations and return the best move
    public Move mcts(Node root, int iterations) {
        int currentIterations = 0;

        ArrayList<Move> rootMoves = new ArrayList<>(root.state.getAvailableMoves().asList());

        // initialise all moves off the root node - MrX moves basically
        for (Move move : rootMoves) {
            Board.GameState newState = root.state.advance(move);
            int newMrXLocation = root.mrXLocation;
            if (move.commencedBy().isMrX()) {
                newMrXLocation = getMrXLocationFromMove(move);
            }
            Node child = new Node(newState, root, newMrXLocation, move, move.commencedBy());
            root.children.add(child);
        }

        // loop through iterations
        while (currentIterations < iterations) {
            Node leaf = traverse(root); // get leaf node via traverse function
            double value = playMove(leaf); // playMove function - random moves until game is over
            backpropagate(leaf, value); // backpropagate through tree
            currentIterations++; // iterate
        }

        return getMove(root); // get the best move and use it
    }

    // traverse function to traverse the tree
    public Node traverse(Node node) {
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
    public int getMrXLocationFromMove(Move move) {
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
    public void backpropagate(Node node, double value) {
        while (node != null) { // until node parent = null which is the root node
            node.visits += 1; // add one to visit
            node.value += value; // add to the nodes value
            node = node.parent; // move to its parent and loop again
        }
    }

    // final move pick function
    public Move getMove(Node root) {
        Node bestChild = null;
        double bestVal = Double.NEGATIVE_INFINITY;

        // ranking based on visits over values
        for (Node child : root.children) { // for child of the root node (mrx moves)
            if (child.visits > bestVal) { // find the one that has the most visits
                bestVal = child.visits; // that will be the bestChild / bestMove
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
    public double playMove(Node node) {
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
    public double getValue(Board.GameState state, int mrXLocation) {
        if (!state.getWinner().isEmpty()) {
            if (state.getWinner().contains(Piece.MrX.MRX)) {
                return 50; // mrx wins so +50
            } else {
                return -50; // mrx loses so -50
            }
        }
        // else just calculate the distance to the closest detective and use that as the value
        return closestDetectiveDistance(state, mrXLocation);
    }

    // closest detective to mrx - considering one not all.
    public double closestDetectiveDistance(Board.GameState state, int mrXLocation) {
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(state, mrXLocation);
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
