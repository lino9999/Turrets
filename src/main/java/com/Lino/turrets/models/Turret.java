package com.Lino.turrets.models;

import org.bukkit.Location;
import org.bukkit.entity.*;
import java.util.UUID;

public class Turret {
    private final UUID id;
    private final UUID ownerId;
    private final String ownerName;
    private Location location;
    private int level;
    private int kills;
    private int ammo;
    private long lastShot;

    public Turret(UUID ownerId, String ownerName, Location location) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.level = 1;
        this.kills = 0;
        this.ammo = getMaxAmmo();
        this.lastShot = 0;
    }

    public Turret(UUID id, UUID ownerId, String ownerName, Location location, int level, int kills, int ammo) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.level = level;
        this.kills = kills;
        this.ammo = ammo;
        this.lastShot = 0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public int getLevel() {
        return level;
    }

    public int getKills() {
        return kills;
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = Math.min(ammo, getMaxAmmo());
    }

    public void useAmmo() {
        if (ammo > 0) {
            ammo--;
        }
    }

    public void addKill() {
        kills++;
    }

    public void setLevel(int level) {
        this.level = Math.min(level, 20);
    }

    public double getDamage() {
        return 2.0 + (level - 1) * 0.5;
    }

    public double getRange() {
        return 10.0 + (level - 1) * 0.5;
    }

    public int getMaxAmmo() {
        return 100 + (level - 1) * 20;
    }

    public boolean canShoot() {
        return ammo > 0 && System.currentTimeMillis() - lastShot >= 1000;
    }

    public void shoot() {
        lastShot = System.currentTimeMillis();
    }

    public LivingEntity findNearestTarget() {
        double range = getRange();
        LivingEntity nearest = null;
        double minDistance = range;

        Location turretCenter = location.clone().add(0.5, 0.5, 0.5);

        for (Entity entity : turretCenter.getWorld().getNearbyEntities(turretCenter, range, range, range)) {
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity living = (LivingEntity) entity;

            if (living instanceof Player) {
                Player player = (Player) living;
                if (player.getUniqueId().equals(ownerId)) continue;
                if (player.getGameMode() == org.bukkit.GameMode.CREATIVE ||
                        player.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            }

            if (living instanceof ArmorStand ||
                    living instanceof Villager ||
                    living.isDead() ||
                    !living.isValid()) continue;

            double distance = turretCenter.distance(living.getLocation());
            if (distance <= range && distance < minDistance) {
                minDistance = distance;
                nearest = living;
            }
        }

        return nearest;
    }
}