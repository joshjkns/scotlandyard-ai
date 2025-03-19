package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;

import com.google.common.graph.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.ui.ai.Resources.*;

class GraphThread extends Thread {
	private final Move move;
  private final Piece mover;
  private final Board.GameState gameState;
  private final MutableValueGraph<Board.GameState, Move> graph;
  private final ArrayList<Piece> tempRemainingList;
  private final Map<Integer, Double> dijkstraResult;

	public GraphThread(Move move, Piece mover, Board.GameState gameState, MutableValueGraph<Board.GameState, Move> graph, ArrayList<Piece> tempRemainingList, Map<Integer, Double> dijkstraResult) {
        this.move = move;
        this.mover = mover;
        this.gameState = gameState;
        this.graph = graph;
        this.tempRemainingList = new ArrayList<>(tempRemainingList);
        this.dijkstraResult = dijkstraResult;
  }

	@Override
    public void run() {
        if (move.commencedBy() == mover) {
            Board.GameState newState = gameState.advance(move);

            graph.addNode(newState);
            graph.putEdgeValue(gameState, newState, move);

            if (!tempRemainingList.isEmpty()) {
                ArrayList<Move> newMoves = new ArrayList<>(newState.getAvailableMoves().asList());
                Graph.miniMaxGraph(newState, Filter.duplicatePruning(newMoves, tempRemainingList.get(0)), dijkstraResult, tempRemainingList.get(0), graph, tempRemainingList);
            }
        }
    }
}

public class Graph {
	public static void miniMaxGraph(Board.GameState gameState, ArrayList<Move> moves, Map<Integer, Double> dijkstraResult, Piece mover, MutableValueGraph<Board.GameState, Move> graph, ArrayList<Piece> playerRemainingList) {
		ArrayList<Piece> tempRemainingList = new ArrayList<>(playerRemainingList);
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
				}
				if ((totalDistances > gameState.getPlayers().size() * 2) || filteredMoves.isEmpty()){
					filteredMoves = Filter.doubleOrSingleFilter(moves, true);
				}
			}
			if (mover.isDetective()) {
				filteredMoves = moves;
			}
			List<Thread> threads = new ArrayList<>();

			for (Move move : filteredMoves) {
				// only wanna make a thread if its MrX on first go.
				if (mover.isMrX()) {
					GraphThread newThread = new GraphThread(move, mover, gameState, graph, tempRemainingList, dijkstraResult);
					threads.add(newThread);
					newThread.start();
				} else {
					if (move.commencedBy() == mover) {
						Board.GameState newState = gameState.advance(move); // new state with move used
						if (!newState.getWinner().isEmpty()) continue;
						graph.addNode(newState); // add this to the graph
						graph.putEdgeValue(gameState, newState, move);
						// connect to the root node
						if (!tempRemainingList.isEmpty()) {
							ArrayList<Move> newMoves = new ArrayList<Move>(newState.getAvailableMoves().asList());
							Graph.miniMaxGraph(newState, Filter.duplicatePruning(newMoves, tempRemainingList.get(0)), dijkstraResult, tempRemainingList.get(0), graph, tempRemainingList);
						}
					}
				}
			}
			// to wait for all threads to end before giving to myai so that it doesnt pick a move before the graph is finished
			for (Thread t : threads) {
				try {
						t.join();
				} catch (Exception e) {
						System.out.println("Error:" + e);
				}
			}
		}
	}
}

// TODO: new ideas just in main in myai4 loop for mrx moves and edit the make graph to just make each subtree ignoring mrx but idk
