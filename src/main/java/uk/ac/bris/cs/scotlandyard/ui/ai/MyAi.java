package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.sql.Array;
import java.util.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import com.google.common.graph.*;
import com.google.common.graph.ImmutableGraph;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MiniMaxAi"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		int source = 0;
		for (Move move : board.getAvailableMoves()) {
			if (move.commencedBy().isMrX()) {
				source = move.source();
			}
		}
		dijkstra(board, source);
		return moves.get(new Random().nextInt(moves.size()));

	}
	public Map<Integer, Double> dijkstra(Board board, int source){
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
		//System.out.println(valueGraph.edgeValue(1,9));
		System.out.println(distances);
		return distances;
	}
}
