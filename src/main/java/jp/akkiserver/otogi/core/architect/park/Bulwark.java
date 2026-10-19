package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;

/** 式：金城 */
public final class Bulwark extends BasePark {
    public Bulwark() {
        super("bulwark", "金城", new String[]{"最大体力 +4 / ノックバック耐性 +20% / 移動速度 -5%", "体力が最大の間、受ける最初のダメージを15%軽減", "併用不可: 式「韋駄天」"}, "rush");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        Modifiers.add(player.getPlayer(), Attribute.MAX_HEALTH, this, 4.0D, Operation.ADD_NUMBER);
        Modifiers.add(player.getPlayer(), Attribute.KNOCKBACK_RESISTANCE, this, 0.20D, Operation.ADD_NUMBER);
        Modifiers.add(player.getPlayer(), Attribute.MOVEMENT_SPEED, this, -0.05D, Operation.ADD_SCALAR);
    }

    @Override
    public void onDamageTaken(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {
        if (self.getPlayer().getHealth() >= self.getMaxHealth()) {
            event.setDamage(event.getDamage() * 0.85D);
        }
    }
}
