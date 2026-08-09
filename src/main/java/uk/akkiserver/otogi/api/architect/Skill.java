package uk.akkiserver.otogi.api.architect;

import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.ArenaPlayer;

public interface Skill extends Architect, CoolTime {
   void onUse(@NotNull ArenaPlayer player);

   default boolean canUse() {
       return isReady();
   }
}
