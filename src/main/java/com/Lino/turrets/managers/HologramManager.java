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
    private final Map<UUID, HologramData> holograms;

    private static class HologramData {
        ArmorStand nameStand;
        ArmorStand ammoStand;
        Location baseLocation;

        HologramData(ArmorStand nameStand, ArmorStand ammoStand, Location baseLocation) {
            this.nameStand = nameStand;
            this.ammoStand = ammoStand;
            this.baseLocation = baseLocation;
        }

        boolean isValid() {
            return nameStand != null && !nameStand.isDead() &&
                    ammoStand != null && !ammoStand.isDead();
        }
    }

    public HologramManager(Turrets plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }

    public void createHologram(Turret turret) {
        HologramData existing = holograms.get(turret.getId());
        if (existing != null && existing.isValid()) {
            updateHologramText(existing.nameStand, existing.ammoStand, turret);
            return;
        }

        removeHologram(turret.getId());

        Location baseLoc = turret.getLocation().clone();
        Location nameLoc = baseLoc.clone().add(0.5, 2, 0.5);
        Location ammoLoc = baseLoc.clone().add(0.5, 1.7, 0.5);

        ArmorStand nameStand = (ArmorStand) nameLoc.getWorld().spawnEntity(nameLoc, EntityType.ARMOR_STAND);
        setupArmorStand(nameStand);

        ArmorStand ammoStand = (ArmorStand) ammoLoc.getWorld().spawnEntity(ammoLoc, EntityType.ARMOR_STAND);
        setupArmorStand(ammoStand);

        updateHologramText(nameStand, ammoStand, turret);

        holograms.put(turret.getId(), new HologramData(nameStand, ammoStand, baseLoc));
    }

    private void setupArmorStand(ArmorStand stand) {
        stand.setCustomNameVisible(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setSmall(false);
        stand.setBasePlate(false);
        stand.setCanPickupItems(false);
        stand.setCollidable(false);
    }

    public void updateHologram(Turret turret) {
        HologramData data = holograms.get(turret.getId());

        if (data == null || !data.isValid()) {
            createHologram(turret);
            return;
        }

        Location expectedLoc = turret.getLocation();
        if (!data.baseLocation.getBlock().equals(expectedLoc.getBlock())) {
            createHologram(turret);
            return;
        }

        updateHologramText(data.nameStand, data.ammoStand, turret);
    }

    private void updateHologramText(ArmorStand nameStand, ArmorStand ammoStand, Turret turret) {
        if (nameStand == null || nameStand.isDead() || ammoStand == null || ammoStand.isDead()) {
            return;
        }

        String gradient = plugin.getMessageManager().applyGradient(turret.getOwnerName(), "#00ff00", "#ffff00");
        String nameText = gradient + " §7[§6Lv." + turret.getLevel() + "§7]";

        if (!nameText.equals(nameStand.getCustomName())) {
            nameStand.setCustomName(nameText);
        }

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

        String ammoText = bar.toString();
        if (!ammoText.equals(ammoStand.getCustomName())) {
            ammoStand.setCustomName(ammoText);
        }
    }

    public void removeHologram(UUID turretId) {
        HologramData data = holograms.remove(turretId);
        if (data != null) {
            if (data.nameStand != null && !data.nameStand.isDead()) {
                data.nameStand.remove();
            }
            if (data.ammoStand != null && !data.ammoStand.isDead()) {
                data.ammoStand.remove();
            }
        }
    }

    public void removeAllHolograms() {
        for (HologramData data : holograms.values()) {
            if (data != null) {
                if (data.nameStand != null && !data.nameStand.isDead()) {
                    data.nameStand.remove();
                }
                if (data.ammoStand != null && !data.ammoStand.isDead()) {
                    data.ammoStand.remove();
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
                    for (HologramData data : holograms.values()) {
                        if (data != null && (stand.equals(data.nameStand) || stand.equals(data.ammoStand))) {
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

    public void forceRefresh(Turret turret) {
        removeHologram(turret.getId());
        createHologram(turret);
    }
}