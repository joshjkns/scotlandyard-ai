package uk.ac.bris.cs.scotlandyard.ui.ai;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import org.glassfish.grizzly.Transport;

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

public class MyDetectiveAI implements Ai {

    @Nonnull @Override public String name() { return "Detective AI"; }

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
        if (gameState.getMrXTravelLog().isEmpty()){
            return maxDistancetoDetectives(gameState,detectivesList,detectivesList.get(0));
        }
        else{
            return minDistanceToMrx(gameState,detectivesList,detectivesList.get(0));
        }
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
            for (Map.Entry<Integer, Double> entry : distances.entrySet()) { // entry.getKey() is every node
                if (visited.get(entry.getKey())){ // visited = true
                    Set<Integer> adjNodes = valueGraph.adjacentNodes(entry.getKey());
                    for (int nextNode : adjNodes){
                        if (!visited.get(nextNode)){
                            if (valueGraph.edgeValue(entry.getKey(), nextNode).get().stream().anyMatch(t -> t.requiredTicket() == Ticket.SECRET)) {
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
    public static Move maxDistancetoDetectives(Board.GameState gameState, ArrayList<Player> detectivesList, Player currentDetective){
        int source = currentDetective.location();
        for(Move induvidualMove : gameState.getAvailableMoves()){
            if (induvidualMove.commencedBy() == currentDetective.piece()){
                Map<Integer, Double> dijkstraResult = dijkstra(gameState, source);
            }
        }
        //Map<Integer, Double> dijkstraResult = dijkstra(gameState, source);
        return null;
    }
    public static Move minDistanceToMrx(Board.GameState board, ArrayList<Player> detectivesList, Player currentDetective){
        return null;
    }
}
