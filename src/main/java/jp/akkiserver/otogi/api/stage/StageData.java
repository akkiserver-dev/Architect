package jp.akkiserver.otogi.api.stage;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface StageData extends Serializable {
    @NotNull
    NamespacedKey getModeId();
}
