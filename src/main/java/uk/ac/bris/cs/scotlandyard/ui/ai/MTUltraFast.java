package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.nio.charset.StandardCharsets;

import com.google.common.collect.*;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.io.Resources;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;



class UltraFastAiThread extends Thread {
    MTUltraFast.Node child;
    public UltraFastAiThread(MTUltraFast.Node child) {
        this.child = child;
    }

    @Override
    public void run() {
        MTUltraFast.buildAllChildren(child,1);
    }
}

public class MTUltraFast implements Ai {

    public static class Node {
        Board.GameState state;
        Move move;
        double value;
        Node parent;
        ArrayList<Move> possibleMoves;
        ArrayList<Node> children;
        int location;

        public Node(Board.GameState state, Node parent, Move move, double value) {
            this.state = state;
            this.parent = parent;
            this.move = move;
            this.value = value;
            this.children = new ArrayList<>();
            this.possibleMoves = new ArrayList<>(state.getAvailableMoves().asList());
            if (this.move == null) {
                this.location = 0;
            } else {
                this.location = move.accept(new Move.Visitor<>() { // use visitor pattern to get the destination of tempMove.
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
        }
    }

    private static Map<Integer, Map<Integer, Double>> dijkstraAll;
    private static List<Piece> detectives;

    @Nonnull
    @Override
    public String name() {return "[MRX] MT Ultrafast";}

    // Dijkstra for all nodes predetermined
    @Override
    public void onStart() {
        detectives = new ArrayList<>();
        dijkstraAll = new HashMap<>();
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
        // Creating a new factory to make a GameState with the required attributes, collected from the inputted board (Instead of downcasting)
        HashMap<ScotlandYard.Ticket, Integer> tempTicketMap = new HashMap<>();
        ArrayList<ScotlandYard.Ticket> tempTicketList = new ArrayList<>(Arrays.asList(ScotlandYard.Ticket.TAXI, ScotlandYard.Ticket.BUS, ScotlandYard.Ticket.UNDERGROUND, ScotlandYard.Ticket.DOUBLE, ScotlandYard.Ticket.SECRET));
        MyGameStateFactory factory = new MyGameStateFactory();
        ArrayList<Player> detectivesList = new ArrayList<>();
        Player mrX = null;
        int location = 0;
        ArrayList<Piece> detectiveTemp = new ArrayList<>();
        for (Piece piece : board.getPlayers()) {
            // stores each ticket type, and the amount of them (Per detective)
            for (ScotlandYard.Ticket ticket : tempTicketList) {
                tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
            }
            // creating a mrX player, using his information we get from the inputted board
            if (piece.isMrX()){
                location = board.getAvailableMoves().asList().get(0).source();
                mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
            } else { // creating a list of detective players, using the information we get about them from the board
                detectiveTemp.add(piece);
                Piece.Detective newDetective = (Piece.Detective) piece;
                Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
                Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
                detectivesList.add(newPlayer);
            }
        }
        detectives = detectiveTemp;
        Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));
        Node root = new Node(gameState, null, null, 0);

        // initialise mrX first set of moves off of root;
        initialiseRootWithMrX(root, board, gameState);

        // build all subsequent states from each mrx (root) node
        ArrayList<Thread> threads = new ArrayList<>();
        for (Node child : root.children) {
            Thread newThread = new UltraFastAiThread(child);
            newThread.start();
            threads.add(newThread);
        }

        // ends the threads one by one, by iterating through the list and calling their .join() method
        for (Thread IndividualThread : threads) {
            try{
                IndividualThread.join();
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }

        // traverse tree, storing the largest value in the root node at the end
        traverseTree(root);
        Move bestMove = null;
        // find the best move, which is stored by the node with the same value as the root node
        for (Node child : root.children) {
            if (child.value == root.value) {
                bestMove = child.move;
            }
        }
        return bestMove;
    }

    // creating the child nodes of the root
    public static void initialiseRootWithMrX(Node root, Board board, Board.GameState gameState) {
        ArrayList<Piece> playerPieces = new ArrayList<>(gameState.getPlayers());
        ArrayList<Integer> detectiveLocations = new ArrayList<>();
        for(Piece individualPlayer : gameState.getPlayers()){
            if (individualPlayer.isDetective()){
                detectiveLocations.add(gameState.getDetectiveLocation((Piece.Detective) individualPlayer).get());
            }
        }
        ArrayList<Move> filteredMoves = Filter.secretPruning(new ArrayList<>(board.getAvailableMoves().asList()));
        filteredMoves = Filter.duplicatePruning(filteredMoves, Piece.MrX.MRX);
        filteredMoves = Filter.doubleOrSingleFilter(filteredMoves,true);
        filteredMoves = Filter.killerMoves(filteredMoves,detectiveLocations,dijkstraAll,playerPieces,gameState);
        for (Move mrXMove : filteredMoves) {
            Board.GameState newState = root.state.advance(mrXMove);
            Node child = new Node(newState, root, mrXMove, 0);
            root.children.add(child);
        }
    }

    //building all the subtree's from each of the root node's children
    public static void buildAllChildren(Node node, int depth) {
        Board.GameState newState = node.state;
        if (!(node.state.getWinner().isEmpty())) return; // if there is a winner it just stops
        ArrayList<Move> bestMoveList = bestArrayOfMoves(node);
        for (Move move : bestMoveList) { // advances 5x for the detectives
            if (newState.getWinner().isEmpty()) newState = newState.advance(move);
        }
        ArrayList<Move> filteredMoves = Filter.duplicatePruning(new ArrayList<>(newState.getAvailableMoves().asList()), Piece.MrX.MRX);
        filteredMoves = Filter.doubleOrSingleFilter(filteredMoves,true);
        for (Move mrXMove : filteredMoves) { // from the newest state get all mrx moves and advance, create a child and add to its parent
            Board.GameState mrXState = newState.advance(mrXMove);
            Node child = new Node(mrXState, node, mrXMove, 0);
            node.children.add(child);
            if (depth < 8) {
                buildAllChildren(child, depth + 1);
            } else {
                bestArrayOfMoves(child);
            }
        }
    }

    // recursively traverses the tree, from bottom to top, setting the value of each parent node, to its own value + Max of its children's values
    public static double traverseTree(Node node) {
        double maxVal = Double.NEGATIVE_INFINITY;
        if (node.children.isEmpty()) {
            return node.value;
        } else {
            for (Node child : node.children) {
                double value = traverseTree(child);
                maxVal = Math.max(maxVal, value);
            }
            node.value += maxVal;
            return node.value;
        }
    }

    // finds the combination of each of the detective's moves, to reduce the distance to MrX the most
    public static ArrayList<Move> bestArrayOfMoves(Node node) {
        List<Set<Integer>> twoDList = twoDArrayOfMoves(node);
        Set<List<Integer>> combinations = Sets.cartesianProduct(twoDList); // finds all combinations of the destinations, set of lists so no duplicate combinations, (det1 destination, det2 destination, det3 destination...)
        List<Integer> bestList = new ArrayList<>();
        double minVal = Double.POSITIVE_INFINITY;
        Map<Integer,Double> dijkstraMap = dijkstraAll.get(node.location); // dijkstra Map from mrX location
        for (List<Integer> list : combinations) {
            double listValueDijkstra = 0;
            double smallestVal = Double.POSITIVE_INFINITY;
            for (int val : list) {
                if (val == -1) continue;
                double temp = dijkstraMap.get(val);
                if (temp < smallestVal) smallestVal = temp;
                listValueDijkstra += temp;
            }
            // if sum of list is < temp value, and the list has no duplicates (no two locations can be occupied)
            if ((listValueDijkstra < minVal) && !hasDuplicatesNotMinusOne(list)) { // checking for duplicates
                minVal = listValueDijkstra;
                node.value = minVal;
                bestList = list;
            }
        }
        ArrayList<Move> resultList = new ArrayList<>();
        // converting list of destinations, into list of moves
        if (!(bestList.isEmpty())) {
            for (int i = 0; i < detectives.size(); i++) {
                if (bestList.get(i) == -1) continue;
                Move move = getMoveFromInteger(detectives.get(i), bestList.get(i), node.possibleMoves);
                resultList.add(move);
            }
        }
        return resultList;
    }

    // returns true if any of the destinations are the same, and the value in that position is not -1, (-1 indicates that the particular detective for that index position has no moves left)
    public static boolean hasDuplicatesNotMinusOne(List<Integer> list) {
        Set<Object> set = new HashSet<>();
        for (Integer val : list) {
            if (!set.add(val) && (val != -1)) {
                return true;
            }
        }
        return false;
    }

    // converts a list of moves, into a list of sets (because no duplicates) of a particular detectives destinations.
    // therefore each set contains, all the places a detective could move to
    public static List<Set<Integer>> twoDArrayOfMoves(Node node) {
        List<Set<Integer>> twoDList = new ArrayList<>(new HashSet<>());
        for (Piece piece : node.state.getPlayers()) {
            if (piece.isDetective()) {
                Set<Integer> intList = new HashSet<>();
                ArrayList<Move> filteredMoves = Filter.duplicatePruning(new ArrayList<>(node.state.getAvailableMoves().asList()), piece);
                for (Move move : filteredMoves) {
                    if (piece == move.commencedBy()) {
                        Move.SingleMove singleMove = (Move.SingleMove) move;
                        intList.add(singleMove.destination);
                    }
                }
                if (intList.isEmpty()) intList = Set.of(-1);
                twoDList.add(intList);
            }
        }
        return twoDList;
    }

    // turns a destination for a particular piece, into a move to that location
    public static Move getMoveFromInteger(Piece mover, int destination, ArrayList<Move> movesList) {
        for (Move individualMove : movesList) {
            int moveDestination = ((Move.SingleMove) individualMove).destination;
            if ((individualMove.commencedBy() == mover) && (moveDestination == destination)) {
                return individualMove;
            }
        }
        return null;
    }
}
