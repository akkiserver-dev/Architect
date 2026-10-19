package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;

/** 式：生気 */
public final class Vitality extends BasePark implements Tickable {
    /** Extra health restored per second while at or below half health (about +20% of natural regeneration). */
    private static final double BONUS_REGEN_PER_SECOND = 0.1D;

    public Vitality() {
        super("vitality", "生気", new String[]{"最大体力 +6 / 移動速度 -8%", "体力が50%以下の間、自然回復量 +20%", "併用不可: 式「韋駄天」"}, "rush");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        Modifiers.add(player.getPlayer(), Attribute.MAX_HEALTH, this, 6.0D, Operation.ADD_NUMBER);
        Modifiers.add(player.getPlayer(), Attribute.MOVEMENT_SPEED, this, -0.08D, Operation.ADD_SCALAR);
    }

    @Override
    public void onTick(@NotNull ArenaPlayer player) {
        double health = player.getPlayer().getHealth();
        if (player.getArchitects().stream().anyMatch(architect -> !architect.canRegenerate(player))) return;
        if (health > 0.0D && health <= player.getMaxHealth() / 2.0D && player.getPlayer().getFoodLevel() >= 18) {
            player.setHealth(health + BONUS_REGEN_PER_SECOND);
        }
    }
}
