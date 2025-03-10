package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Dijkstra {
    public static Map<Integer, Double> dijkstraFunction(Board.GameState board, int source){
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
            for (Map.Entry<Integer, Double> entry : distances.entrySet()) { // entry.getKey() is every node
                if (visited.get(entry.getKey())){ // visited = true
                    Set<Integer> adjNodes = valueGraph.adjacentNodes(entry.getKey());
                    for (int nextNode : adjNodes){
                        if (!visited.get(nextNode)){
                            if (valueGraph.edgeValue(entry.getKey(), nextNode).get().stream().anyMatch(t -> t.requiredTicket() == ScotlandYard.Ticket.SECRET)) {
                                break;
                            }
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
}
