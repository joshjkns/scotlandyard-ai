package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.*;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MiniMaxAi"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		HashMap<Ticket, Integer> tempTicketMap = new HashMap<>();
		ArrayList<Ticket> tempTicketList = new ArrayList<>(Arrays.asList(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND, Ticket.DOUBLE, Ticket.SECRET));
		MyGameStateFactory factory = new MyGameStateFactory();
		ArrayList<Player> detectivesList = new ArrayList<>();
		Player mrX = null;
		for (Piece piece : board.getPlayers()) {
			for (Ticket ticket : tempTicketList) {
				tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
			}
			if (piece.isMrX()){
				int location = board.getAvailableMoves().asList().get(0).source();
				mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
			} else {
				Detective newDetective = (Detective) piece;
				Optional<Integer> location = board.getDetectiveLocation(newDetective);
				Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location.get());
				detectivesList.add(newPlayer);
			}
		}
		Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

		ArrayList<Move> moves = new ArrayList<>(gameState.getAvailableMoves().asList()); // mrX moves (currently because only AI on mrX).
		int source = 0;
		for (Move move : gameState.getAvailableMoves()) {
			if (move.commencedBy().isMrX()) {
				source = move.source();
			}
		}
		MutableValueGraph<Board.GameState, Move> graph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
		graph.addNode(gameState);
		ArrayList<Piece> playerRemainingList = new ArrayList<>(gameState.getPlayers().asList());
		ArrayList<Move> newMoves = duplicatePruning(moves);
		Map<Integer, Double> dijkstraResult = dijkstra(gameState, source);
		HashMap<Double, Board.GameState> finalMap = new HashMap<>();
		miniMaxGraph(gameState, newMoves, dijkstraResult, MrX.MRX, graph, playerRemainingList);
		printGraph(graph);
		double bestVal = miniMax(gameState, graph, true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, dijkstraResult, playerRemainingList, finalMap);
		Board.GameState chosenState = finalMap.get(bestVal);
		Move chosenMove = graph.edgeValue(gameState, chosenState).get();
		return chosenMove;

	}

	public static Map<Integer, Double> dijkstra(Board.GameState board, int source){
		ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> valueGraph = board.getSetup().graph;
		Map<Integer, Double> distances = new HashMap<>();
		Map<Integer, Boolean> visited = new HashMap<>();

		for (int i = 1; i <= 199; i++) {
			distances.put(i, Double.POSITIVE_INFINITY);
			visited.put(i, false);
		}

		distances.put(source, 0.0);
		visited.put(source, true);

		while (visited.containsValue(false)) {
			for (Map.Entry<Integer, Double> entry : distances.entrySet()) {
				if (visited.get(entry.getKey())){ // visited = true
					Set<Integer> adjNodes = valueGraph.adjacentNodes(entry.getKey());
					for (int nextNode : adjNodes){
						if (!visited.get(nextNode)){
							double newDistance = entry.getValue() + 1;
							if (newDistance < distances.get(nextNode)) {
								distances.put(nextNode, newDistance);
							}
						}
					}
				}
			}
			for (Map.Entry<Integer, Double> entry : distances.entrySet()){
				if (entry.getValue() != Double.POSITIVE_INFINITY) {
					visited.put(entry.getKey(), true);
				}
			}
		}
		return distances;
	}


	public void miniMaxGraph(Board.GameState gameState, List<Move> moves, Map<Integer, Double> dijkstraResult, Piece mover, MutableValueGraph<Board.GameState, Move> graph, ArrayList<Piece> playerRemainingList) {
		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
		Board.GameState newState = null;
		// initiate mrX moves into graph.
		if (!tempRemainingList.isEmpty()) { // final go is length 1
			tempRemainingList.remove(mover); // remove mover from playerRemainingList
			for (Move move : moves) {
				if (move.commencedBy() == mover) {
					newState = gameState.advance(move); // new state with move used
					graph.addNode(newState); // add this to the graph
					graph.putEdgeValue(gameState, newState, move);
					// connect to the root node
					if (!tempRemainingList.isEmpty()) {
						ArrayList<Move> newMoves = new ArrayList<Move>(newState.getAvailableMoves().asList());
						miniMaxGraph(newState, duplicatePruning(newMoves), dijkstraResult, tempRemainingList.get(0), graph, tempRemainingList);
					}
				}
			}
		}
	}

	public double miniMax(Board.GameState state, MutableValueGraph<Board.GameState, Move> graph, boolean isMrX, double alpha, double beta, Map<Integer, Double> dijkstraResult, ArrayList<Piece> playerRemainingList, HashMap<Double, Board.GameState> finalMap) {
		double bestVal = 0;
		double value = 0;
		double res = 0;

		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
		if (!tempRemainingList.isEmpty()) {
			Piece mover = tempRemainingList.get(0);
			if (tempRemainingList.size() != 1) {
				tempRemainingList.remove(mover);
			}
			f (mover.isDetective()) {
				res += dijkstraResult.get(state.getDetectiveLocation((Detective) mover).get());
				System.out.println("PLAYER: " + mover + "RES: " + res);
			}
			if (graph.successors(state).isEmpty()) { // leaf
				return res;
			}

			if (mover.isMrX()) {
				bestVal = Double.NEGATIVE_INFINITY;
				for (Board.GameState child : graph.successors(state)) {
					value = miniMax(child, graph, false, alpha, beta, dijkstraResult, tempRemainingList, finalMap);
					bestVal = Math.max(bestVal, value);
					finalMap.put(value, child);
					alpha = Math.max(alpha, bestVal);
					if (beta <= alpha) {
						break;
					}
				}
				return bestVal;
			}
	
			else {
				bestVal = Double.POSITIVE_INFINITY;
				for (Board.GameState child : graph.successors(state)) {
					value = miniMax(child, graph, false, alpha, beta, dijkstraResult, tempRemainingList, finalMap);
					bestVal = Math.min(bestVal, value);
					beta = Math.min(beta, bestVal);
					if (beta <= alpha) {
						break;
					}
				}
				if (tempRemainingList.size() == 1) {
					tempRemainingList.remove(mover);
				}
				return bestVal;
			}
		}
		return bestVal;
	}

	public static <N, V> void printGraph(MutableValueGraph<N, V> graph) {
		try (PrintWriter writer = new PrintWriter(new FileWriter("graph.txt"))) {
			writer.println("Graph:");
			for (N node : graph.nodes()) {
				for (N neighbor : graph.successors(node)) {
					V value = graph.edgeValueOrDefault(node, neighbor, null);
					writer.println(node + " -> " + neighbor + " [Value: " + value + "]");
				}
			}
			System.out.println("Graph data written to graph.txt");
		} catch (IOException e) {
			System.err.println("Error writing to file: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static ArrayList<Move> duplicatePruning(ArrayList<Move> moves) {
		ArrayList<Move> prunedList = new ArrayList<>();
		ArrayList<Integer> destList = new ArrayList<>();
		int destination = 0;
		for (Move move : moves) {
			destination = move.accept(new Move.Visitor<Integer>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});
			if (!(destList.contains(destination))) { // if the destination list doesn't contain the destination
				prunedList.add(move); // add to the pruned list (new moves list)
				destList.add(destination);
			}
		}
		return prunedList;
	}

}

