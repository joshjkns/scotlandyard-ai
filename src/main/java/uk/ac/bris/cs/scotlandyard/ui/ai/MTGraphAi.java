package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.*;

import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;


public class MTGraphAi implements Ai {

	ArrayList<Move> mrXMoves = new ArrayList<>();

	@Nonnull @Override public String name() { return "[MRX] MT Graph AI"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		HashMap<Ticket, Integer> tempTicketMap = new HashMap<>();
		ArrayList<Ticket> tempTicketList = new ArrayList<>(Arrays.asList(Ticket.TAXI, Ticket.BUS, Ticket.UNDERGROUND, Ticket.DOUBLE, Ticket.SECRET));
		MyGameStateFactory factory = new MyGameStateFactory();
		ArrayList<Player> detectivesList = new ArrayList<>();
		Player mrX = null;
		int location = 0;
		// creating temporary gamestate to allow advance() to be used.
		// for all pieces
		for (Piece piece : board.getPlayers()) {
			for (Ticket ticket : tempTicketList) {
				// put their tickets into a map with each ticket type and the number of them.
				tempTicketMap.put(ticket, board.getPlayerTickets(piece).get().getCount(ticket));
			}
			if (piece.isMrX()){ // if piece is mrx, get his location and create his player
				location = board.getAvailableMoves().asList().get(0).source();
				mrX = new Player(piece, ImmutableMap.copyOf(tempTicketMap), location);
			} else {
				// cast piece to a detective and get their location before making their player.
				Detective newDetective = (Detective) piece;
				Optional<Integer> detectiveLocation = board.getDetectiveLocation(newDetective);
				Player newPlayer = new Player(piece, ImmutableMap.copyOf(tempTicketMap), detectiveLocation.get());
				detectivesList.add(newPlayer);
			}
		}
		// get the gamestate from the list of players, mrx and the setup
		Board.GameState gameState = factory.build(board.getSetup(), mrX, ImmutableList.copyOf(detectivesList));

		// lastlocation is set to location unless he has made a reveal move - tries to get away due to players naturally going there.
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

//		MutableValueGraph<Board.GameState, Move> normGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
//		normGraph.addNode(gameState); // add root nodes

		ArrayList<Piece> playerRemainingList = new ArrayList<>(gameState.getPlayers().asList());
		ArrayList<Move> newMoves = Filter.duplicatePruning(moves, Piece.MrX.MRX);
		newMoves = noRepeatMoves(newMoves);
		Map<Integer, Double> dijkstraResult = Dijkstra.dijkstraFunction(gameState.getSetup().graph, source);
		ArrayListMultimap<Double, Board.GameState> finalMap = ArrayListMultimap.create();
		//creating the physical miniMax tree
		Graph.miniMaxGraph(gameState, newMoves, dijkstraResult, MrX.MRX, graph, playerRemainingList);
		// running the miniMax traversal on the created graph
		double bestVal = miniMax(gameState, graph, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, dijkstraResult, playerRemainingList, finalMap);

		Move chosenMove = null;
		double maxDistance = -1;
		Map<Integer, Double> dijkstraLastLocation = Dijkstra.dijkstraFunction(gameState.getSetup().graph, lastLocation); // dijkstra's called on prev location of mrX
		for (Board.GameState tempState : finalMap.get(bestVal)) { // looping through the potential duplicate value moves
			Move tempMove = graph.edgeValue(gameState, tempState).get();
			if (maxDistance == -1) { // assign chosenMove to tempMove to prevent null.
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
			// pick the move that is furthest away from the last location.
			double distance = dijkstraLastLocation.get(destination);
			if (distance > maxDistance) {
				maxDistance = distance;
				chosenMove = tempMove;
			}
		}

		ArrayList<Move> printMap = new ArrayList<>();
		for (Board.GameState individualGameState : finalMap.values()){
			if (graph.edgeValue(gameState,individualGameState).isPresent()) {
				printMap.add(graph.edgeValue(gameState,individualGameState).get());
			}
			
		}

		mrXMoves.add(chosenMove);
    assert chosenMove != null;
    return chosenMove;

	}

	//miniMax traversal of teh physical graph
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
			if (!(graph.nodes().contains(state))) {
				return 0;
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
					Map<Integer, Double> dijkstraResultInput = Dijkstra.dijkstraFunction(child.getSetup().graph, destination);
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
	
			else { //detective mover
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

	// stops mrX from going back to the same location he just came from, because moving more, reduces likelihood of being trapped
	public ArrayList<Move> noRepeatMoves(ArrayList<Move> moves){
		ArrayList<Move> returnMoves = new ArrayList<>();
		for (Move individualMove : moves){
				int destination = individualMove.accept(new Move.Visitor<Integer>() {
						@Override
						public Integer visit(Move.SingleMove move) {
								return move.destination;
						}

						@Override
						public Integer visit(Move.DoubleMove move) {
								return move.destination2;
						}
				});
				if (!(mrXMoves.isEmpty())) {
						if (!(destination == mrXMoves.get(mrXMoves.size() - 1).source()) || !(moves.size() > 1)) {
								returnMoves.add(individualMove);
						}
				}
				else{
						returnMoves.add(individualMove);
				}
				//returnMoves.add(individualMove);
		}
		return returnMoves;
}
}




