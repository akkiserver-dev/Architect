package uk.akkiserver.otogi.api.arena;

import org.bukkit.entity.Player;
import uk.akkiserver.otogi.api.architect.Architect;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ArenaPlayer {
    protected final Player player;
    protected final Set<Architect> architects = new HashSet<>();
    protected final Arena arena;

    public ArenaPlayer(Player player, Arena arena) {
        this.player = player;
        this.arena = arena;
    }

    public Player getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    public Set<Architect> getArchitects() {
        return Collections.unmodifiableSet(architects);
    }
}
