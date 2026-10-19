package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.architect.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.luaj.vm2.LuaValue;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.*;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.ArchitectItems;
import jp.akkiserver.otogi.api.util.Modifiers;

import java.util.List;
import java.util.Set;

/** Immutable definition snapshot. ArenaPlayer owns all script state and lifecycle cleanup. */
public abstract class ScriptArchitect implements Architect, Tickable {
    final ArchitectDefinition definition;
    private final LuaSandbox script;

    ScriptArchitect(ArchitectDefinition definition, LuaSandbox script) { this.definition = definition; this.script = script; }

    static ScriptArchitect create(ArchitectDefinition definition, LuaSandbox script) {
        if (script != null) {
            String required = switch (definition.kind()) { case SKILL -> "on_use"; case TOOL -> "on_attack"; default -> null; };
            if (required != null && !script.hasHook(required)) throw new IllegalArgumentException(definition.id() + " requires " + required);
        }
        return switch (definition.kind()) {
            case SKILL -> new ScriptSkill(definition, script);
            case TOOL -> new ScriptTool(definition, script);
            case PASSIVE -> new ScriptPassive(definition, script);
            case PARK -> new ScriptPark(definition, script);
        };
    }

    public ArchitectDefinition getDefinition() { return definition; }
    @Override public NamespacedKey getId() { return new NamespacedKey("otogi", definition.id()); }
    @Override public Component getName() { return Component.text(definition.name()); }
    @Override public List<Component> getDescription() { return definition.description().stream().<Component>map(Component::text).toList(); }
    String stateKey() { return "script." + getId() + ".state"; }
    final boolean hasFailed(ArenaPlayer self) { return Boolean.TRUE.equals(self.getMatchState(stateKey() + ".failed", Boolean.class)); }
    @Override public void onEquip(ArenaPlayer self) { invoke("on_equip", self, null); }
    @Override public void onUnequip(ArenaPlayer self) {
        try { invoke("on_unequip", self, null); }
        finally { clearPlayerEffects(self); }
    }
    private void clearPlayerEffects(ArenaPlayer self) {
        try { removeAttributeModifiers(self); }
        finally {
            MythicSkillHandlers.clear(this, self);
            self.removeState(stateKey());
            self.removeState(stateKey() + ".tick");
        }
    }
    /** Separate hook so lifecycle behavior can be tested without a running Paper registry. */
    protected void removeAttributeModifiers(ArenaPlayer self) { Modifiers.removeAll(self.getPlayer(), this); }
    @Override public void onTick(ArenaPlayer self) {
        if (hasFailed(self)) return;
        if (script == null && MythicSkillHandlers.supports(definition.handler())) {
            invoke("on_tick", self, null);
            return;
        }
        if (script == null || !script.hasHook("on_tick")) return;
        Integer next = self.getState(stateKey() + ".tick", Integer.class);
        int now = Bukkit.getCurrentTick();
        if (next != null && now < next) return;
        self.setState(stateKey() + ".tick", now + definition.tickInterval());
        invoke("on_tick", self, null);
    }
    @Override public void onDamageDealt(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) { invoke("on_damage_dealt", self, event); }
    @Override public void onDamageTaken(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) { invoke("on_damage_taken", self, event); }
    @Override public void onDamageConfirmed(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) { invoke("on_hit", self, event); }
    @Override public void onDamageTakenConfirmed(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) { invoke("on_hurt", self, event); }
    @Override public void onKill(ArenaPlayer self, ArenaPlayer victim) { invoke("on_kill", self, null); }
    @Override public void onSkillUse(ArenaPlayer self, Skill skill) { invoke("on_skill", self, null); }

    final boolean invoke(String hook, ArenaPlayer self, EntityDamageEvent event) {
        if (hasFailed(self)) return false;
        if (script != null && !script.hasHook(hook)) return false;
        if (script == null && !hook.equals("on_use") && !hook.equals("on_attack")
                && !(MythicSkillHandlers.supports(definition.handler())
                && Set.of("on_tick", "on_damage_dealt", "on_damage_taken").contains(hook))) return false;
        try {
            return executeHook(hook, self, event);
        } catch (RuntimeException exception) {
            self.setMatchState(stateKey() + ".failed", true);
            try { clearPlayerEffects(self); }
            catch (RuntimeException cleanupFailure) { exception.addSuppressed(cleanupFailure); }
            OtogiAPI.LOGGER.error("Disabled scripted architect {} for owner {} for this match after {} failed; edit and reload: {}",
                    getId(), self.getPlayer(), hook, exception.getMessage());
            self.sendNotice(Component.text(definition.name() + " の定義エラー。運営へ連絡してください"));
            return false;
        }
    }

    protected boolean executeHook(String hook, ArenaPlayer self, EntityDamageEvent event) {
        GameplayContext context = new GameplayContext(this, self, hook, event);
        if (script != null) {
            LuaValue result = script.invoke(hook, context.table());
            // Explicit true avoids accidental cooldown consumption on missing return.
            return result.isboolean() && result.toboolean();
        }
        if (MythicSkillHandlers.supports(definition.handler())) return MythicSkillHandlers.handle(context, hook, event);
        return javaHandler(context);
    }

    private boolean javaHandler(GameplayContext context) {
        boolean used;
        switch (definition.handler()) {
            case "pulse" -> {
                context.cast("sphere", definition.parameter("range", 5), 0, definition.parameter("damage", 4),
                        definition.kind() == ArchitectDefinition.Kind.TOOL ? "AREA_TOOL" : "AREA_SKILL", definition.parameter("knockback", .5));
                context.effect("arcane");
                return true;
            }
            case "lance" -> {
                context.cast("first", definition.parameter("range", 8), definition.parameter("width", .25), definition.parameter("damage", 3),
                        definition.kind() == ArchitectDefinition.Kind.TOOL ? "TOOL" : "SKILL", definition.parameter("knockback", .15));
                return true;
            }
            case "dash" -> {
                used = context.dash(definition.parameter("speed", 1.25), definition.parameter("lift", .18));
                if (used) context.effect("wind");
                return used;
            }
            case "mend" -> {
                used = context.heal(definition.parameter("heal", 5));
                if (used) context.effect("heal");
                return used;
            }
            default -> throw new IllegalStateException("Unknown Java handler");
        }
    }

    private static final class ScriptSkill extends ScriptArchitect implements Skill {
        ScriptSkill(ArchitectDefinition definition, LuaSandbox script) { super(definition, script); }
        @Override public int getDefaultCoolTime() { return definition.cooldown(); }
        @Override public Material getIcon() { return definition.material(); }
        @Override public boolean onUse(ArenaPlayer self) { return invoke("on_use", self, null); }
    }
    private static final class ScriptTool extends ScriptArchitect implements Tool {
        ScriptTool(ArchitectDefinition definition, LuaSandbox script) { super(definition, script); }
        @Override public int getAttackInterval() { return definition.attackInterval(); }
        @Override public boolean onAttack(ArenaPlayer self) { return invoke("on_attack", self, null); }
        @Override public boolean onUse(ArenaPlayer self) {
            // Java template tools are left-click only; Lua may add a separate right-click action.
            if (scriptMissing() || self.getRemainingCoolTime(this) > 0) return false;
            boolean used = invoke("on_use", self, null);
            if (used) self.setCoolTime(this, definition.cooldown());
            return used;
        }
        private boolean scriptMissing() { return !definition.backend().equals("lua"); }
        @Override public ItemStack createItem() {
            ItemStack item = new ItemStack(definition.material());
            var meta = item.getItemMeta();
            meta.itemName(getName());
            meta.lore(getDescription().stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
            return ArchitectItems.mark(item, this);
        }
    }
    private static final class ScriptPassive extends ScriptArchitect implements Passive {
        ScriptPassive(ArchitectDefinition definition, LuaSandbox script) { super(definition, script); }
    }
    private static final class ScriptPark extends ScriptArchitect implements Park {
        ScriptPark(ArchitectDefinition definition, LuaSandbox script) { super(definition, script); }
        @Override public Set<NamespacedKey> getIncompatiblearchitects() { return definition.incompatible(); }
    }
}
