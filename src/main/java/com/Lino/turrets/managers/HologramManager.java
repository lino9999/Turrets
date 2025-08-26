package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramManager {
    private final Turrets plugin;
    private final Map<UUID, ArmorStand[]> holograms;

    public HologramManager(Turrets plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }

    public void createHologram(Turret turret) {
        ArmorStand[] existing = holograms.get(turret.getId());
        if (existing != null && existing.length == 2) {
            if (existing[0] != null && !existing[0].isDead() && existing[1] != null && !existing[1].isDead()) {
                updateHologramText(existing[0], existing[1], turret);
                return;
            }
        }

        removeHologram(turret.getId());

        Location loc = turret.getLocation().clone().add(0.5, 2, 0.5);

        ArmorStand nameStand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        nameStand.setCustomNameVisible(true);
        nameStand.setGravity(false);
        nameStand.setVisible(false);
        nameStand.setMarker(true);
        nameStand.setInvulnerable(true);
        nameStand.setSmall(false);
        nameStand.setBasePlate(false);

        ArmorStand ammoStand = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().subtract(0, 0.3, 0), EntityType.ARMOR_STAND);
        ammoStand.setCustomNameVisible(true);
        ammoStand.setGravity(false);
        ammoStand.setVisible(false);
        ammoStand.setMarker(true);
        ammoStand.setInvulnerable(true);
        ammoStand.setSmall(false);
        ammoStand.setBasePlate(false);

        updateHologramText(nameStand, ammoStand, turret);

        holograms.put(turret.getId(), new ArmorStand[]{nameStand, ammoStand});
    }

    public void updateHologram(Turret turret) {
        ArmorStand[] stands = holograms.get(turret.getId());
        if (stands != null && stands.length == 2) {
            if (stands[0] != null && !stands[0].isDead() && stands[1] != null && !stands[1].isDead()) {
                updateHologramText(stands[0], stands[1], turret);
                return;
            }
        }

        createHologram(turret);
    }

    private void updateHologramText(ArmorStand nameStand, ArmorStand ammoStand, Turret turret) {
        if (nameStand == null || nameStand.isDead() || ammoStand == null || ammoStand.isDead()) {
            return;
        }

        String gradient = plugin.getMessageManager().applyGradient(turret.getOwnerName(), "#00ff00", "#ffff00");
        nameStand.setCustomName(gradient + " §7[§6Lv." + turret.getLevel() + "§7]");

        int ammo = turret.getAmmo();
        int maxAmmo = turret.getMaxAmmo();
        int barLength = 20;
        int filled = (int) ((double) ammo / maxAmmo * barLength);

        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("§a|");
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§8] §e").append(ammo).append("/").append(maxAmmo);

        ammoStand.setCustomName(bar.toString());
    }

    public void removeHologram(UUID turretId) {
        ArmorStand[] stands = holograms.remove(turretId);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
    }

    public void removeAllHolograms() {
        for (ArmorStand[] stands : holograms.values()) {
            if (stands != null) {
                for (ArmorStand stand : stands) {
                    if (stand != null && !stand.isDead()) {
                        stand.remove();
                    }
                }
            }
        }
        holograms.clear();
    }

    public void cleanupDuplicates(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location.clone().add(0.5, 2, 0.5), 1, 3, 1)) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                if (!stand.isVisible() && stand.isMarker()) {
                    boolean isTracked = false;
                    for (ArmorStand[] tracked : holograms.values()) {
                        if (tracked != null && (stand.equals(tracked[0]) || stand.equals(tracked[1]))) {
                            isTracked = true;
                            break;
                        }
                    }
                    if (!isTracked) {
                        stand.remove();
                    }
                }
            }
        }
    }
}