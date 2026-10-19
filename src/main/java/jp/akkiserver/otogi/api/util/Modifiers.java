package jp.akkiserver.otogi.api.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Architect;

/**
 * Helpers for attribute modifiers owned by an architect.
 */
public final class Modifiers {
    private static String prefix(Architect owner) {
        return owner.getId().getKey() + "/";
    }

    public static void add(@NotNull Player player, @NotNull Attribute attribute, @NotNull Architect owner,
                           double amount, @NotNull AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        NamespacedKey attributeKey = Registry.ATTRIBUTE.getKey(attribute);
        if (attributeKey == null || !Double.isFinite(amount)) return;
        NamespacedKey key = new NamespacedKey(owner.getId().getNamespace(), prefix(owner) + attributeKey.getKey());
        instance.getModifiers().stream().filter(m -> m.getKey().equals(key)).forEach(instance::removeModifier);
        instance.addTransientModifier(new AttributeModifier(key, amount, operation));
    }

    public static void removeAll(@NotNull Player player, @NotNull Architect owner) {
        String namespace = owner.getId().getNamespace();
        String prefix = prefix(owner);
        for (Attribute attribute : Registry.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instance.getModifiers().stream()
                    .filter(m -> m.getKey().getNamespace().equals(namespace) && m.getKey().getKey().startsWith(prefix))
                    .forEach(instance::removeModifier);
        }
        AttributeInstance maximum = player.getAttribute(Attribute.MAX_HEALTH);
        if (maximum != null && player.getHealth() > maximum.getValue()) {
            player.setHealth(maximum.getValue());
        }
    }

    private Modifiers() {}
}
