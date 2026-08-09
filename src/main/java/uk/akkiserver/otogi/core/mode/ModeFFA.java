package uk.akkiserver.otogi.core.mode;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.mode.Mode;
import uk.akkiserver.otogi.api.stage.StageData;
import uk.akkiserver.otogi.api.util.Position;

import java.util.Collection;

public class ModeFFA implements Mode {
    public static final NamespacedKey ID = new NamespacedKey("otogi", "ffa");

    @Override
    public @NotNull NamespacedKey getId() {
        return ID;
    }

    @Override
    public int getDefaultMatchTime() {
        return 0;
    }

    public record FFAStageData(Collection<Position> spawns) implements StageData {
        @Override
        public @NotNull NamespacedKey getModeId() {
            return ID;
        }
    }
}
