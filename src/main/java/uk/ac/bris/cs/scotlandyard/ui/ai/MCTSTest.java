package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.units.qual.A;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MCTSTest implements Ai {

    public class Node {
        Board.GameState state;
        Node parent;
        ArrayList<Node> children;
        int visits;
        double value;
        ArrayList<Move> possibleMoves;
        int mrXLocation;
        private final Move move;

        // constructor for a node
        public Node(Board.GameState state, Node parent, int mrXLocation, Move move) {
            this.state = state;
            this.parent = parent;
            this.children = new ArrayList<>();
            this.possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
            this.possibleMoves = Filter.duplicatePruning(possibleMoves);
            this.visits = 0;
            this.value = 0;
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
            Node child = new Node(newState, this, newMrXLocation, move);
            children.add(child);
            return child;
        }
    }

    @Nonnull @Override public String name() { return "[MRX] MCTS Test"; }

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

        Node root = new Node (gameState,null, location, null);
        System.out.println("Moved");
        return search(root, 2000000);
    }

    public static boolean hasWinner(Node node) {
        return (!node.state.getWinner().isEmpty());
    }

    public static boolean isExpanded(Node node) {
        return !node.children.isEmpty();
    }

    // main mcts function to search over n iterations and return the best move
    public Move search(Node root, int iterations) {
        int currentIterations = 0;

        ArrayList<Move> rootMoves = new ArrayList<>(root.state.getAvailableMoves().asList());
        rootMoves = Filter.duplicatePruning(rootMoves);

        // initialise all moves off the root node - MrX moves basically
        for (Move move : rootMoves) {
            Board.GameState newState = root.state.advance(move);
            int newMrXLocation = root.mrXLocation;
            if (move.commencedBy().isMrX()) {
                newMrXLocation = getMrXLocationFromMove(move);
            }
            Node child = new Node(newState, root, newMrXLocation, move);
            root.children.add(child);
        }

        while (currentIterations < iterations) {
            Node leaf = traverse(root);
            double value = playMove(leaf);
            backpropagate(leaf, value);
            currentIterations++;
        }
        System.out.println(currentIterations);

        return getMove(root);
    }

    // traverse function to traverse the tree
    public Node traverse(Node node) {
        while (isExpanded(node) && !hasWinner(node)) {
            Node bestUCT = node.bestUCT();
            if (bestUCT == null) {
                break;
            }
            node = bestUCT;
        }

        Node unvisited = node.chooseUnvisited();
        if (unvisited != null) {
            return unvisited;
        }

        if (!isExpanded(node)) {
            return node.expand();
        }

        return node;
    }

    // getting mrx location to keep nodes updated
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
            node.visits += 1;
            node.value += value;
            node = node.parent;
        }
    }

    // final move pick function
    public Move getMove(Node root) {
        Node bestChild = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Node child : root.children) {
            if (child.visits > bestValue) {
                bestValue = child.visits;
                bestChild = child;
            }
        }
        ArrayList<Move> moves = new ArrayList<>(root.state.getAvailableMoves());

        if (bestChild == null) {
            if (!moves.isEmpty()) {
                return moves.get(new Random().nextInt(moves.size()));
            }
            return null;
        }

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

        int depth = 0;
        int maxDepth = 50;

        // loop through until win or until you reach the max depth
        while (state.getWinner().isEmpty() || depth < maxDepth) {
            ArrayList<Move> possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
            if (possibleMoves.isEmpty()) break;

            Move move = null;
            int random = new Random().nextInt(possibleMoves.size());
            move = possibleMoves.get(random);

            state = state.advance(move);
            if (move.commencedBy().isMrX()) mrXcurrentLocation = getMrXLocationFromMove(move);
            depth++;
        }
        return getValue(state, mrXcurrentLocation);
    }

    // high values if a game ending outcome, else use dijkstra
    public double getValue(Board.GameState state, int mrXLocation) {
        if (!state.getWinner().isEmpty()) {
            if (state.getWinner().contains(Piece.MrX.MRX)) {
                return 50; // mrx wins
            } else {
                return -50; // mrx loses
            }
        }

        return closestDetectiveDistance(state, mrXLocation);
    }

    // closest detective to mrx - considering one not all.
    public double closestDetectiveDistance(Board.GameState state, int mrXLocation) {
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(state, mrXLocation);
        double detectiveTotal = Double.POSITIVE_INFINITY;
        for (Piece piece : state.getPlayers()) {
            if (piece.isDetective()) {
                detectiveTotal = Math.min(detectiveTotal, dijkstraResult.get(state.getDetectiveLocation((Piece.Detective) piece).get()));
            }
        }
        return detectiveTotal;
    }

    // mrx distance from the detective
    public double mrXDistance(Board.GameState state, int mrXLocation, Piece.Detective mover) {
        Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(state, mrXLocation);
        return dijkstraResult.get(state.getDetectiveLocation(mover).get());
    }

    // function to get the best moves from an array of moves
    public Move bestMoveBasedOnClosestDetective(Board.GameState state, ArrayList<Move> possibleMoves, int mrXLocation) {
        Move bestMove = null;
        double minDistance = Double.NEGATIVE_INFINITY;
        double maxDistance = Double.POSITIVE_INFINITY;
        for (Move move : possibleMoves) {
            Board.GameState newState = state.advance(move);
            if (move.commencedBy().isMrX()) {
                mrXLocation = getMrXLocationFromMove(move);
                double closestDetective = closestDetectiveDistance(newState, mrXLocation);
                if (closestDetective > minDistance) {
                    bestMove = move;
                    minDistance = closestDetective;
                }
            } else { // detective move
                double mrXDistance = mrXDistance(newState, mrXLocation, (Piece.Detective) move.commencedBy());
                if (mrXDistance < maxDistance) { // want detectives to get as close as possible
                    bestMove = move;
                    maxDistance = mrXDistance;
                }
            }
        }
        return bestMove;
    }

}
