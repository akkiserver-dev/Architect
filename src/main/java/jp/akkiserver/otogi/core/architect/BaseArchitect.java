package jp.akkiserver.otogi.core.architect;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.OtogiAPI;

import java.util.Arrays;
import java.util.List;

public abstract class BaseArchitect implements Architect {
    private final NamespacedKey id;
    private final Component name;
    private final List<Component> description;

    protected BaseArchitect(@NotNull String id, @NotNull String name, @NotNull String... description) {
        this.id = new NamespacedKey("otogi", id);
        this.name = Component.translatable("architect." + id + ".name", name);
        this.description = Arrays.stream(description).<Component>map(Component::text).toList();
    }

    @Override
    public @NotNull NamespacedKey getId() {
        return id;
    }

    @Override
    public @NotNull Component getName() {
        String configured = OtogiAPI.plugin == null ? null : OtogiAPI.plugin.getConfig().getString("abilities." + id.getKey() + ".name");
        return configured == null ? name : Component.text(configured);
    }

    @Override
    public @NotNull List<Component> getDescription() {
        if (OtogiAPI.plugin != null && OtogiAPI.plugin.getConfig().isList("abilities." + id.getKey() + ".description")) {
            return OtogiAPI.plugin.getConfig().getStringList("abilities." + id.getKey() + ".description").stream().<Component>map(Component::text).toList();
        }
        return description;
    }
}
