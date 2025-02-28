package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.lang.reflect.Array;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
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
		miniMax(gameState, newMoves, source, MrX.MRX, graph, playerRemainingList);
		printGraph(graph);
		return moves.get(0);

	}

	public Map<Integer, Double> dijkstra(Board.GameState board, int source){
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


	public Move miniMax(Board.GameState gameState, List<Move> moves, int source, Piece mover, MutableValueGraph<Board.GameState, Move> graph, ArrayList<Piece> playerRemainingList) {
		int i = 0;
		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);

		Board.GameState newState;
		// initiate mrX moves into graph.
		if (tempRemainingList.size() > 0) { // final go is length 1
			tempRemainingList.remove(mover); // remove mover from playerRemainingList
			for (Move move : moves) {
				if (move.commencedBy() == mover) {
					newState = gameState.advance(move); // new state with move used
					graph.addNode(newState); // add this to the graph
					graph.putEdgeValue(gameState, newState, move); // connect to the root node
//					System.out.println("added node!" + i);
					if (tempRemainingList.size() > 0) {
						ArrayList<Move> newMoves = new ArrayList<Move>(newState.getAvailableMoves().asList());
						miniMax(newState, duplicatePruning(newMoves), source, tempRemainingList.get(0), graph, tempRemainingList);
					}
				}
			}
		}
//		System.out.println(moves.size());
		//printGraph(graph);
		return null;
	}
	public static <N, V> void printGraph(MutableValueGraph<N, V> graph) {
		System.out.println("Graph:");
		for (N node : graph.nodes()) {
			for (N neighbor : graph.successors(node)) {
				V value = graph.edgeValueOrDefault(node, neighbor, null);
				System.out.println(node + " -> " + neighbor + " [Value: " + value + "]");
			}
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

