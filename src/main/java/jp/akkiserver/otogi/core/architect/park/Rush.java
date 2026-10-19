package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;

/** 式：韋駄天 */
public final class Rush extends BasePark {
    public Rush() {
        super("rush", "韋駄天", new String[]{"体力 -6 / 移動速度 +10% / 跳躍力 +10%", "併用不可: 相「隠密」"}, "stealth");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        Modifiers.add(player.getPlayer(), Attribute.MAX_HEALTH, this, -6.0D, Operation.ADD_NUMBER);
        Modifiers.add(player.getPlayer(), Attribute.MOVEMENT_SPEED, this, 0.10D, Operation.ADD_SCALAR);
        Modifiers.add(player.getPlayer(), Attribute.JUMP_STRENGTH, this, 0.10D, Operation.ADD_SCALAR);
    }
}
