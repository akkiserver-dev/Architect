package jp.akkiserver.otogi.core.architect;

import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Park;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.build.Build;
import jp.akkiserver.otogi.api.build.Builds;
import jp.akkiserver.otogi.core.architect.park.Bulwark;
import jp.akkiserver.otogi.core.architect.park.Levity;
import jp.akkiserver.otogi.core.architect.park.Rush;
import jp.akkiserver.otogi.core.architect.park.Tank;
import jp.akkiserver.otogi.core.architect.park.Vitality;
import jp.akkiserver.otogi.core.architect.passive.Bounty;
import jp.akkiserver.otogi.core.architect.passive.Resurrection;
import jp.akkiserver.otogi.core.architect.passive.Scavenger;
import jp.akkiserver.otogi.core.architect.passive.Stealth;
import jp.akkiserver.otogi.core.architect.passive.Tribute;
import jp.akkiserver.otogi.core.architect.skill.Overclock;
import jp.akkiserver.otogi.core.architect.skill.Purge;
import jp.akkiserver.otogi.core.architect.skill.Warp;
import jp.akkiserver.otogi.core.architect.tool.Chakram;
import jp.akkiserver.otogi.core.architect.tool.Halberd;
import jp.akkiserver.otogi.core.architect.tool.Katana;
import jp.akkiserver.otogi.core.architect.tool.Rapier;
import jp.akkiserver.otogi.core.architect.tool.Tome;
import jp.akkiserver.otogi.core.architect.park.Conduit;
import jp.akkiserver.otogi.core.architect.park.Focus;
import jp.akkiserver.otogi.core.architect.park.Resonance;
import jp.akkiserver.otogi.core.architect.passive.Afterimage;
import jp.akkiserver.otogi.core.architect.passive.Counter;
import jp.akkiserver.otogi.core.architect.passive.Curse;
import jp.akkiserver.otogi.core.architect.passive.Echo;
import jp.akkiserver.otogi.core.architect.passive.Foresight;
import jp.akkiserver.otogi.core.architect.skill.Anchor;
import jp.akkiserver.otogi.core.architect.skill.Hack;
import jp.akkiserver.otogi.core.architect.skill.Mirror;
import jp.akkiserver.otogi.core.architect.skill.Sever;
import jp.akkiserver.otogi.core.architect.skill.Tether;
import jp.akkiserver.otogi.core.architect.skill.Veil;
import jp.akkiserver.otogi.core.architect.tool.Lantern;
import jp.akkiserver.otogi.core.architect.tool.Ward;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Architects {
    public static void registerDefaults() {
        OtogiAPI.registerArchitects(
                new Rush(), new Tank(), new Bulwark(), new Vitality(), new Levity(), new Conduit(), new Focus(), new Resonance(),
                new Resurrection(), new Stealth(), new Tribute(), new Bounty(), new Scavenger(),
                new Afterimage(), new Counter(), new Curse(), new Echo(), new Foresight(),
                new Warp(), new Purge(), new Overclock(), new Anchor(), new Hack(), new Mirror(), new Sever(), new Tether(), new Veil(),
                new Katana(), new Rapier(), new Halberd(), new Tome(), new Chakram(), new Lantern(), new Ward()
        );
    }

    /**
     * Gives the player the architects of their build.
     * A player without a build gets a random one: one park and random others up to the build size.
     */
    public static void assign(@NotNull ArenaPlayer player) {
        Build build = Builds.get(player.getPlayer().getUniqueId());
        List<Architect> chosen = build.isEmpty() ? random() : build.getArchitects();
        chosen.forEach(player::addArchitect);
    }

    private static List<Architect> random() {
        Build build = new Build();
        Park park = pick(list(Park.class));
        if (park != null) {
            Builds.add(build, park);
        }
        Tool tool = pick(list(Tool.class));
        if (tool != null) {
            Builds.add(build, tool);
        }
        List<Architect> others = new ArrayList<>();
        for (Architect architect : OtogiAPI.getArchitects()) {
            if (!(architect instanceof Park)) {
                others.add(architect);
            }
        }
        Collections.shuffle(others);
        for (Architect architect : others) {
            if (build.size() >= Builds.getSize()) {
                break;
            }
            Builds.add(build, architect);
        }
        return build.getArchitects();
    }

    private static <T extends Architect> List<T> list(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Architect architect : OtogiAPI.getArchitects()) {
            if (type.isInstance(architect)) {
                result.add(type.cast(architect));
            }
        }
        Collections.sort(result, (a, b) -> a.getId().compareTo(b.getId()));
        return result;
    }

    private static <T> T pick(List<T> list) {
        return list.isEmpty() ? null : list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private Architects() {}
}
