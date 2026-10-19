package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AfterimageTest {
    @Test void armorStandEquipmentUsesLocksInsteadOfMobDropChanceSetters() {
        List<String> equipped = new ArrayList<>();
        Set<String> locks = new HashSet<>();
        EntityEquipment equipment = (EntityEquipment) Proxy.newProxyInstance(EntityEquipment.class.getClassLoader(),
                new Class[]{EntityEquipment.class}, (proxy, method, args) -> {
                    if (method.getName().contains("DropChance"))
                        throw new IllegalArgumentException("Cannot set drop chance for non-Mob entity");
                    equipped.add(method.getName());
                    return null;
                });
        ArmorStand stand = (ArmorStand) Proxy.newProxyInstance(ArmorStand.class.getClassLoader(),
                new Class[]{ArmorStand.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getEquipment")) return equipment;
                    if (method.getName().equals("addEquipmentLock")) locks.add(args[0] + "/" + args[1]);
                    return null;
                });

        assertDoesNotThrow(() -> Afterimage.equipDecoy(stand, null, null, null));
        assertEquals(List.of("setHelmet", "setChestplate", "setLeggings"), equipped);
        assertEquals(6, locks.size());
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS)) {
            assertTrue(locks.contains(slot + "/" + ArmorStand.LockType.ADDING_OR_CHANGING));
            assertTrue(locks.contains(slot + "/" + ArmorStand.LockType.REMOVING_OR_CHANGING));
        }
    }
}
