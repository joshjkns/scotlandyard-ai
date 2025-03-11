package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ArrayListMultimap;
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

	@Nonnull @Override public String name() { return "[MRX:1] 6 layer boss (Graph)"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		HashMap<Ticket, Integer> tempTicketMap = new HashMap<>();
		ArrayList<Ticket> tempTicketList = new ArrayList<>(Arrays.asList(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND, Ticket.DOUBLE, Ticket.SECRET));
		MyGameStateFactory factory = new MyGameStateFactory();
		ArrayList<Player> detectivesList = new ArrayList<>();
		Player mrX = null;
		int location = 0;
		for (Piece piece : board.getPlayers()) {
			for (Ticket ticket : tempTicketList) {
				tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
			}
			if (piece.isMrX()){
				location = board.getAvailableMoves().asList().get(0).source();
				mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
			} else {
				Detective newDetective = (Detective) piece;
				Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
				Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
				detectivesList.add(newPlayer);
			}
		}
		Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

		int lastLocation = location;
		for (LogEntry entry : board.getMrXTravelLog()) {
			if (entry.location().isPresent()) {
				lastLocation = entry.location().get();
			}
		}

		ArrayList<Move> moves = new ArrayList<>(gameState.getAvailableMoves().asList()); // mrX moves (currently because only AI on mrX).
		int source = 0;
		for (Move move : gameState.getAvailableMoves()) {
			if (move.commencedBy().isMrX()) {
				source = move.source();
			}
		}

		MutableValueGraph<Board.GameState, Move> graph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
		graph.addNode(gameState); // add root node

		ArrayList<Piece> playerRemainingList = new ArrayList<>(gameState.getPlayers().asList());
		ArrayList<Move> newMoves = Filter.duplicatePruning(moves);
		Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(gameState, source);
		ArrayListMultimap<Double, Board.GameState> finalMap = ArrayListMultimap.create();
		miniMaxGraph(gameState, newMoves, dijkstraResult, MrX.MRX, graph, playerRemainingList);
		double bestVal = miniMax(gameState, graph, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, dijkstraResult, playerRemainingList, finalMap);
//		Board.GameState chosenState = finalMap.get(bestVal);

		Move chosenMove = null;
		double maxDistance = -1;
		Map<Integer, Double> dijkstraLastLocation = Dijkstra.dijkstraFunction(gameState, lastLocation);
		for (Board.GameState tempState : finalMap.get(bestVal)) {
			Move tempMove = graph.edgeValue(gameState, tempState).get();
			if (maxDistance == -1) {
				chosenMove = tempMove;
			}
			int destination = tempMove.accept(new Move.Visitor<>() {
				@Override
				public Integer visit(Move.SingleMove move) {
					return move.destination;
				}

				@Override
				public Integer visit(Move.DoubleMove move) {
					return move.destination2;
				}
			});
			double distance = dijkstraLastLocation.get(destination);
			if (distance > maxDistance) {
				maxDistance = distance;
				chosenMove = tempMove;
			}
		}
		ArrayList<Move> printMap = new ArrayList<>();
		for (Board.GameState individualGameState : finalMap.values()){
			printMap.add(graph.edgeValue(gameState,individualGameState).get());
		}
		System.out.println(printMap);
		System.out.println(finalMap);

    assert chosenMove != null;
    return chosenMove;

	}

	public static void miniMaxGraph(Board.GameState gameState, ArrayList<Move> moves, Map<Integer, Double> dijkstraResult, Piece mover, MutableValueGraph<Board.GameState, Move> graph, ArrayList<Piece> playerRemainingList) {
		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
		Board.GameState newState = null;
		ArrayList<Move> filteredMoves = new ArrayList<>();
		double totalDistances = 0;
		// initiate mrX moves into graph.
		if (!tempRemainingList.isEmpty()) { // final go is length 1
			tempRemainingList.remove(mover);// remove mover from playerRemainingList
			if (mover.isMrX()){
				for (Piece detectivePiece : tempRemainingList) {
					if (detectivePiece.isMrX()) {
						break;
					}
					if (detectivePiece.isDetective()) {
						totalDistances += dijkstraResult.get(gameState.getDetectiveLocation((Detective) detectivePiece).get());
					}
				}
				if (totalDistances <= gameState.getPlayers().size() * 2) {
					filteredMoves = Filter.doubleOrSingleFilter(moves, false);
					System.out.println(filteredMoves);
				}
				if ((totalDistances > gameState.getPlayers().size() * 2) || filteredMoves.isEmpty()){
					filteredMoves = Filter.doubleOrSingleFilter(moves, true);
				}
			}
			if (mover.isDetective()) {
				filteredMoves = moves;
			}
			//eliminateMoves(moves,false)
			if (filteredMoves.isEmpty()){
				System.out.println("hello");
			}
			for (Move move : filteredMoves) {
				if (move.commencedBy() == mover) {
					newState = gameState.advance(move); // new state with move used
					graph.addNode(newState); // add this to the graph
					graph.putEdgeValue(gameState, newState, move);
					// connect to the root node
					if (!tempRemainingList.isEmpty()) {
						ArrayList<Move> newMoves = new ArrayList<Move>(newState.getAvailableMoves().asList());
						miniMaxGraph(newState, Filter.duplicatePruning(newMoves), dijkstraResult, tempRemainingList.get(0), graph, tempRemainingList);
					}
				}
			}
		}
	}

	public double miniMax(Board.GameState state, MutableValueGraph<Board.GameState, Move> graph, double alpha, double beta, Map<Integer, Double> dijkstraResult, ArrayList<Piece> playerRemainingList, ArrayListMultimap<Double, Board.GameState> finalMap) {
		double bestVal = 0;
		double value = 0;
		double intermediate = 0;

		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
		if (!tempRemainingList.isEmpty()) {
			Piece mover = tempRemainingList.get(0);
			if (tempRemainingList.size() != 1) {
				tempRemainingList.remove(mover);
			}
			if (mover.isDetective()) {
				intermediate = dijkstraResult.get(state.getDetectiveLocation((Detective) mover).get());
			}
			if (graph.successors(state).isEmpty()) { // leaf
				return dijkstraResult.get(state.getDetectiveLocation((Detective) mover).get());
			}

			if (mover.isMrX()) {
				bestVal = Double.NEGATIVE_INFINITY;
				for (Board.GameState child : graph.successors(state)) {
					int destination = graph.edgeValue(state,child).get().accept(new Move.Visitor<Integer>() {
						@Override
						public Integer visit(Move.SingleMove move) {
							return move.destination;
						}

						@Override
						public Integer visit(Move.DoubleMove move) {
							return move.destination2;
						}
					});
					Map<Integer, Double> dijkstraResultInput = Dijkstra.dijkstraFunction(child, destination);
					value = miniMax(child, graph, alpha, beta, dijkstraResultInput, tempRemainingList, finalMap);
					bestVal = Math.max(bestVal, value);
					finalMap.put(value, child);
//				 Alpha Beta pruning
//					alpha = Math.max(alpha, bestVal);
//					if (beta <= alpha) {
//						break;
//					}
				}
				return bestVal;

			}
	
			else {
				bestVal = Double.POSITIVE_INFINITY;
				for (Board.GameState child : graph.successors(state)) {
					value = miniMax(child, graph, alpha, beta, dijkstraResult, tempRemainingList, finalMap);
					bestVal = Math.min(bestVal, value);
//				Alpha Beta pruning
//					beta = Math.min(beta, bestVal);
//					if (beta <= alpha) {
//						break;
//					}
				}
				if (tempRemainingList.size() == 1) {
					tempRemainingList.remove(mover);
				}
				return bestVal + intermediate;
			}
		}
		return bestVal;
	}

}



