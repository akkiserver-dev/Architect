package uk.akkiserver.otogi.api.arena;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.mode.Mode;
import uk.akkiserver.otogi.api.stage.Stage;
import uk.akkiserver.otogi.api.util.MatchTimer;

import java.util.HashSet;
import java.util.Set;

public class Arena {
    protected final Set<ArenaPlayer> players = new HashSet<>();
    protected final Stage stage;
    protected final Mode mode;
    protected final MatchTimer matchTimer;

    public Arena(@NotNull Stage stage, @NotNull Mode mode) {
        this.stage = stage;
        this.mode = mode;
        this.matchTimer = new MatchTimer(this, mode.getDefaultMatchTime());
    }

    @NotNull
    public Stage getStage() {
        return stage;
    }

    @NotNull
    public Mode getMode() {
        return mode;
    }

    @NotNull
    public MatchTimer getMatchTimer() {
        return matchTimer;
    }

    public void end() {
        matchTimer.cancel();
        mode.onEnd(this);
    }
}