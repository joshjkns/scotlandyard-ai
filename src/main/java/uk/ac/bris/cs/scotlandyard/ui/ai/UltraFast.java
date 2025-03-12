package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.nio.charset.StandardCharsets;

import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
//import com.google.
import com.google.common.io.Resources;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static uk.ac.bris.cs.scotlandyard.model.ScotlandYard.readGraph;

public class UltraFast implements Ai {

    public class Node {
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

    private Map<Integer, Map<Integer, Double>> dijkstraAll;
    private List<Piece> detectives;

    @Nonnull
    @Override
    public String name() {return "[MRX] Ultrafast";}

    // Dijkstra for all nodes predetermined
    @Override
    public void onStart() {
        detectives = Arrays.asList(Piece.Detective.RED, Piece.Detective.GREEN, Piece.Detective.BLUE, Piece.Detective.WHITE, Piece.Detective.YELLOW);
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
        Node root = new Node(gameState, null, null, 0);

        // initialise mrX first set of moves off of root;
        initialiseRootWithMrX(root, board);

        // build all subsequent states from each mrx (root) node
        for (Node child : root.children) {
            buildAllChildren(child, 0);
        }
        printTree(root);

        return null;
    }

    public void initialiseRootWithMrX(Node root, Board board) {
        ArrayList<Move> filteredMoves = Filter.duplicatePruning(new ArrayList<>(board.getAvailableMoves().asList()), Piece.MrX.MRX);
        for (Move mrXMove : filteredMoves) {
            Board.GameState newState = root.state.advance(mrXMove);
            Node child = new Node(newState, root, mrXMove, 0);
            root.children.add(child);
        }
    }

    public void buildAllChildren(Node node, int depth) {
        ArrayList<Move> bestMoveList = bestArrayOfMoves(node);
        Board.GameState newState = node.state;
        for (Move move : bestMoveList) { // advances 5x for the detectives
            if (newState.getWinner().isEmpty()) newState = newState.advance(move);
        }
        ArrayList<Move> filteredMoves = Filter.duplicatePruning(new ArrayList<>(newState.getAvailableMoves().asList()), Piece.MrX.MRX);
        for (Move mrXMove : filteredMoves) { // from the newest state get all mrx moves and advance, create a child and add to its parent
            Board.GameState mrXState = newState.advance(mrXMove);
            Node child = new Node(mrXState, node, mrXMove, node.value);
            node.children.add(child);
            if (depth < 2) buildAllChildren(child, depth + 1);
        }

    }

    public void printTree(Node node) {
        if (!(node.children.isEmpty())) {
            for (Node child : node.children) {
                printTree(child);
            }
        }
    }

    public ArrayList<Move> bestArrayOfMoves(Node node) {
        List<Set<Integer>> twoDList = twoDArrayOfMoves(node);
        Set<List<Integer>> combinations = Sets.cartesianProduct(twoDList);
        List<Integer> bestList = new ArrayList<>();
        double minVal = Double.POSITIVE_INFINITY;
        for (List<Integer> list : combinations) {
            double listValueDijkstra = list.stream().map(x -> (dijkstraAll.get(node.location)).get(x)).reduce(0.0, Double::sum);
            if ((listValueDijkstra < minVal) && (Set.copyOf(list).size() == list.size())) { // checking for duplicates
                minVal = listValueDijkstra;
                bestList = list;
            }
        }
        ArrayList<Move> resultList = new ArrayList<>();
        for (int i = 0; i < detectives.size(); i++) {
            Move move = getMoveFromInteger(detectives.get(i), bestList.get(i), node.possibleMoves);
            resultList.add(move);
        }
        return resultList;
    }

    public List<Set<Integer>> twoDArrayOfMoves(Node node) {
        List<Set<Integer>> twoDList = new ArrayList<>(new HashSet<>());
        for (Piece piece : node.state.getPlayers()) {
            if (piece.isDetective()) {
                Set<Integer> intList = new HashSet<>();
                ArrayList<Move> filteredMoves = Filter.duplicatePruning(new ArrayList<>(node.state.getAvailableMoves().asList()), piece);
                for (Move move : filteredMoves) {
                    if (piece == move.commencedBy()) {
                        Move.SingleMove singleMove = (Move.SingleMove) move;
                        intList.add(singleMove.destination);
//                        List<Integer> test = List.copyOf(intList);
//                        List<Double> test2 = test.stream().map(x -> dijkstraAll.get(node.location).get(x)).toList();
//                        System.out.println("MOVER: " + move.commencedBy() +"VALUES: " + test2);
                    }
                }
                twoDList.add(intList);
            }
        }
        return twoDList;
    }

    public Move getMoveFromInteger(Piece mover, int destination, ArrayList<Move> movesList) {
        for (Move individualMove : movesList) {
            int moveDestination = ((Move.SingleMove) individualMove).destination;
            if ((individualMove.commencedBy() == mover) && (moveDestination == destination)) {
                return individualMove;
            }
        }
        return null;
    }
}
