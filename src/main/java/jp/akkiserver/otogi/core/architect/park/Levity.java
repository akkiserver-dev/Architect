package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;

/** 式：浮力 */
public final class Levity extends BasePark {
    private static final int SPEED_TICKS = 40;

    public Levity() {
        super("levity", "浮力", new String[]{"落下ダメージ無効 / 最大体力 -3", "空中で敵を攻撃すると短時間移動速度上昇", "併用不可: 式「金城」"}, "bulwark");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        Modifiers.add(player.getPlayer(), Attribute.MAX_HEALTH, this, -3.0D, Operation.ADD_NUMBER);
    }

    @Override
    public void onDamageTaken(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onDamageDealt(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @NotNull EntityDamageByEntityEvent event) {
        if (!self.getPlayer().isOnGround()) {
            self.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_TICKS, 0, false, false));
        }
    }
}
