//package uk.ac.bris.cs.scotlandyard.ui.ai;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.lang.reflect.Array;
//import java.util.concurrent.ForkJoinPool;
//import java.util.concurrent.RecursiveTask;
//import java.util.concurrent.TimeUnit;
//import javax.annotation.Nonnull;
//
//import com.google.common.collect.ArrayListMultimap;
//import org.glassfish.grizzly.Transport;
//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import com.google.common.collect.ImmutableList;
//import io.atlassian.fugue.Pair;
//import uk.ac.bris.cs.gamekit.graph.Node;
//import uk.ac.bris.cs.scotlandyard.model.Ai;
//import uk.ac.bris.cs.scotlandyard.model.Board;
//import uk.ac.bris.cs.scotlandyard.model.Move;
//
//import java.util.*;
//
//import com.google.common.collect.ImmutableMap;
//import com.google.common.graph.*;
//import com.google.common.collect.ImmutableSet;
//import uk.ac.bris.cs.scotlandyard.model.Player;
//import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
//import uk.ac.bris.cs.scotlandyard.model.Piece.*;
//import uk.ac.bris.cs.scotlandyard.model.*;
//import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
//
//public class MiniMaxParallel {
//    private static final int MAX_DEPTH = 6;
//    private static final ForkJoinPool pool = new ForkJoinPool();
//
//    public static void miniMaxGraphParallel(Board.GameState gameState, ArrayList<Move> moves,
//                                            Map<Integer, Double> dijkstraResult, Piece mover,
//                                            MutableValueGraph<Board.GameState, Move> graph,
//                                            ArrayList<Piece> playerRemainingList) {
//        pool.invoke(new MiniMaxTask(gameState, moves, dijkstraResult, mover, graph, playerRemainingList, 0));
//    }
//
//    private static class MiniMaxTask extends RecursiveTask<Void> {
//        private final Board.GameState gameState;
//        private final ArrayList<Move> moves;
//        private final Map<Integer, Double> dijkstraResult;
//        private final Piece mover;
//        private final MutableValueGraph<Board.GameState, Move> graph;
//        private final ArrayList<Piece> playerRemainingList;
//        private final int depth;
//
//        MiniMaxTask(Board.GameState gameState, ArrayList<Move> moves, Map<Integer, Double> dijkstraResult,
//                    Piece mover, MutableValueGraph<Board.GameState, Move> graph,
//                    ArrayList<Piece> playerRemainingList, int depth) {
//            this.gameState = gameState;
//            this.moves = moves;
//            this.dijkstraResult = dijkstraResult;
//            this.mover = mover;
//            this.graph = graph;
//            this.playerRemainingList = new ArrayList<>(playerRemainingList);
//            this.depth = depth;
//        }
//
//        @Override
//        protected Void compute() {
//            if (depth >= MAX_DEPTH || playerRemainingList.isEmpty()) return null;
//
//            ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
//            tempRemainingList.remove(mover);
//            ArrayList<Move> filteredMoves = MyAi.doubleOrSingleFilter(moves, true);
//
//            List<MiniMaxTask> subTasks = new ArrayList<>();
//
//            for (Move move : filteredMoves) {
//                if (move.commencedBy() == mover) {
//                    Board.GameState newState = gameState.advance(move);
//                    graph.addNode(newState);
//                    graph.putEdgeValue(gameState, newState, move);
//
//                    if (!tempRemainingList.isEmpty()) {
//                        ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves().asList());
//                        MiniMaxTask task = new MiniMaxTask(newState, MyAi.duplicatePruning(newMoves),
//                                dijkstraResult, tempRemainingList.get(0), graph, tempRemainingList, depth + 1);
//                        subTasks.add(task);
//                    }
//                }
//            }
//
//            // Run subtasks in parallel
//            invokeAll(subTasks);
//            return null;
//        }
//    }
//}