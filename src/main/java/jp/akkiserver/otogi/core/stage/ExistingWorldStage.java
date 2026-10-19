package jp.akkiserver.otogi.core.stage;

import net.kyori.adventure.text.Component;
import org.bukkit.World;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.stage.Stage;
import jp.akkiserver.otogi.api.stage.StageData;
import jp.akkiserver.otogi.api.util.Position;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Runtime view of operator-created terrain; no world or block generation occurs here. */
public final class ExistingWorldStage implements Stage {
    private final MapDefinition definition;
    private final List<Position> spawns;
    private final Position spectator, recovery, control, min, max;
    private final Set<StageData> data;
    public ExistingWorldStage(MapDefinition definition, World world) {
        this.definition = definition;
        spawns = definition.spawns().stream().map(point -> point.in(world)).toList();
        spectator = definition.spectator().in(world);
        recovery = definition.recovery().in(world);
        control = (definition.controlPoint() == null ? definition.recovery() : definition.controlPoint()).in(world);
        min = definition.min().in(world);
        max = definition.max().in(world);
        data = OtogiAPI.getModes().stream().map(mode -> mode.createCommandStageData(spawns))
                .filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }
    @Override public String getId() { return definition.id(); }
    @Override public Component getTitle() { return Component.text(definition.title()); }
    @Override public Component getDescription() { return Component.text(definition.description()); }
    @Override public List<Position> getSpawnPositions() { return spawns; }
    @Override public Position getSpectatorSpawn() { return spectator; }
    @Override public Position getRecoverySpawn() { return recovery; }
    @Override public Position getControlPoint() { return control; }
    @Override public double getControlPointRadius() { return definition.controlRadius(); }
    @Override public Position getPos1() { return min; }
    @Override public Position getPos2() { return max; }
    @Override public Set<StageData> getData() { return data; }
}
