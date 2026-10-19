package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;

/** 式：金剛 */
public final class Tank extends BasePark {
    public Tank() {
        super("tank", "金剛", new String[]{"最大体力 +4 / 移動速度 -5% / ノックバック耐性 +10% / 耐性Ⅰ", "併用不可: 術「神隠し」"}, "warp");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        Modifiers.add(player.getPlayer(), Attribute.MAX_HEALTH, this, 4.0D, Operation.ADD_NUMBER);
        Modifiers.add(player.getPlayer(), Attribute.MOVEMENT_SPEED, this, -0.05D, Operation.ADD_SCALAR);
        Modifiers.add(player.getPlayer(), Attribute.KNOCKBACK_RESISTANCE, this, 0.10D, Operation.ADD_NUMBER);
        player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) {
        super.onUnequip(player);
        player.getPlayer().removePotionEffect(PotionEffectType.RESISTANCE);
    }
}
