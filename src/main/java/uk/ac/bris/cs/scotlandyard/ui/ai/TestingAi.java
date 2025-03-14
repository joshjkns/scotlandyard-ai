package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.*;

public class TestingAi {
    public static void main(String[] args){
        ArrayList<Move> testArray = new ArrayList<>();
        Move move1 = new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,193);
        testArray.add(move1);
        Move move2 = new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.TAXI,193);
        testArray.add(move2);
        Move move3 = new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,182);
        testArray.add(move3);
        Move move4 = new Move.SingleMove(ScotlandYard.ALL_PIECES.get(0),181, ScotlandYard.Ticket.SECRET,182);
        testArray.add(move4);
        System.out.println(testArray);
        System.out.println(Filter.secretPruning(testArray));
        //ScotlandYard.
    }
}
