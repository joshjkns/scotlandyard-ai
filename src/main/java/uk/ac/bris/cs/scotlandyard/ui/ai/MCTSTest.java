package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MCTSTest implements Ai {

    @Nonnull @Override public String name() { return "[MRX] MCTS Test"; }

    @Nonnull
    @Override
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        return null;
    }

    public Board.GameState monteCarloTreeSearch(Board.GameState root) {
        return null;
    }
}
