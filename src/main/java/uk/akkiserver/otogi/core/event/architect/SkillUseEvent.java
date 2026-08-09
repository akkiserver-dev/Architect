package uk.akkiserver.otogi.core.event.architect;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.architect.Skill;
import uk.akkiserver.otogi.api.arena.ArenaPlayer;

public abstract class SkillUseEvent extends SkillEvent {
    private final Skill skill;
    private final ArenaPlayer player;

    public SkillUseEvent(Skill skill, ArenaPlayer player) {
        this.skill = skill;
        this.player = player;
    }

    public ArenaPlayer getPlayer() {
        return player;
    }

    public Skill getSkill() {
        return skill;
    }

    public static final class Pre extends SkillUseEvent {
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Pre(Skill skill, ArenaPlayer player) {
            super(skill, player);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        public static HandlerList getHandlerList() {
            return HANDLER_LIST;
        }
    }

    public static final class Post extends SkillUseEvent {
        private static final HandlerList HANDLER_LIST = new HandlerList();

        public Post(Skill skill, ArenaPlayer player) {
            super(skill, player);
        }

        @Override
        public @NotNull HandlerList getHandlers() {
            return HANDLER_LIST;
        }

        public static HandlerList getHandlerList() {
            return HANDLER_LIST;
        }
    }
}
