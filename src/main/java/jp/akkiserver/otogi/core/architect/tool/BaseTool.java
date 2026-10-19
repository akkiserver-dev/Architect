package jp.akkiserver.otogi.core.architect.tool;

import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.util.ArchitectItems;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

/**
 * A tool whose item is a plain icon. Hit detection, effects and damage are implemented by each tool.
 */
abstract class BaseTool extends BaseArchitect implements Tool {
    private final Material material;
    private final int attackInterval;

    protected BaseTool(@NotNull String id, @NotNull String name, @NotNull Material material, int attackInterval,
                       @NotNull String... description) {
        super(id, name, description);
        this.material = material;
        this.attackInterval = attackInterval;
    }

    @Override
    public int getAttackInterval() {
        return CombatSettings.ticks("tools." + getId().getKey() + ".attack-interval", attackInterval);
    }

    @Override
    public @NotNull ItemStack createItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.itemName(getName().color(NamedTextColor.WHITE));
        meta.lore(getDescription().stream().map(line -> line.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return ArchitectItems.mark(item, this);
    }

    /**
     * Draws particles along a straight line.
     */
    protected static void trail(@NotNull Location from, @NotNull Vector direction, double length, double step, @NotNull Particle particle) {
        if (!CombatSettings.enabled("feedback.particles") || direction.lengthSquared() < 0.0001D) return;
        Vector unit = direction.clone().normalize();
        double reach = Targets.clearDistance(from, direction, Math.min(64, length));
        for (double d = 0; d <= reach; d += Math.max(0.15, step)) {
            Location point = from.clone().add(unit.clone().multiply(d));
            point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
}
