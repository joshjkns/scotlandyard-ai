package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import java.sql.Array;
import java.util.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.*;
import com.google.common.graph.ImmutableGraph;
import com.google.common.collect.ImmutableSet;
import com.sun.source.tree.Tree;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Player;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MiniMaxAi"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
//		Board.GameState gameState = (Board.GameState) board;
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

		var moves = gameState.getAvailableMoves().asList();
		int source = 0;
		for (Move move : gameState.getAvailableMoves()) {
			if (move.commencedBy().isMrX()) {
				source = move.source();
			}
		}
		System.out.println(moves);
		miniMax(gameState, moves, source, MrX.MRX);
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
//		System.out.println(distances);
		return distances;
	}

//	public Move miniMax(Board.GameState gameState, List<Move> moves, int source) {
//		MyGameStateFactory factory = new MyGameStateFactory();
//		HashMap<ScotlandYard.Ticket, Integer> temp = new HashMap<>();
//		Optional<Board.TicketBoard> tempTicketBoard = gameState.getPlayerTickets(Detective.RED);
//		temp.put(ScotlandYard.Ticket.SECRET, tempTicketBoard.get().getCount(ScotlandYard.Ticket.SECRET));
//		ImmutableMap<ScotlandYard.Ticket, Integer> ticketTemp = ImmutableMap.of();
//
//		//Player test = new Player(Detective.RED,, 0);
//		Map<Integer, Double> dijkstraMap = dijkstra(gameState, source);
//		//gameState.advance(mo);
//		return moves.get(new Random().nextInt(moves.size()));
//	}


	public Move miniMax(Board.GameState gameState, List<Move> moves, int source, Piece mover) {
		MutableValueGraph<Board.GameState, Move> graph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
		graph.addNode(gameState);
		int i = 0;

		Board.GameState newState;
		// initiate mrX moves into graph.
		for (Move move : moves) {
			if (move.commencedBy() == mover) {
				newState = gameState.advance(move); // new state with move used
				graph.addNode(newState); // add this to the graph
				graph.putEdgeValue(gameState, newState, move); // connect to the root node
				System.out.println("added node!" + i);
				i++;
			}
//			if (!(move.commencedBy() == gameState.getPlayers().asList().get(gameState.getPlayers().size() - 1))) {
//				break;
//			}
//			miniMax(newState, moves, source, newState.getPlayers().asList().get(i));
		}
		System.out.println(moves.size());
		System.out.println(graph);
		return null;
	}
}

