package com.Lino.turrets.models;

import com.Lino.turrets.Turrets;
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
    private TargetMode targetMode;

    public enum TargetMode {
        ALL_ENTITIES("All Entities"),
        HOSTILE_ONLY("Hostile Only");

        private final String displayName;

        TargetMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Turret(UUID ownerId, String ownerName, Location location) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.level = 1;
        this.kills = 0;
        this.ammo = getMaxAmmo();
        this.lastShot = 0;
        this.targetMode = TargetMode.ALL_ENTITIES;
    }

    public Turret(UUID ownerId, String ownerName, Location location, int level, int kills, int ammo) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.level = level;
        this.kills = kills;
        this.ammo = ammo;
        this.lastShot = 0;
        this.targetMode = TargetMode.ALL_ENTITIES;
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
        this.targetMode = TargetMode.ALL_ENTITIES;
    }

    public Turret(UUID id, UUID ownerId, String ownerName, Location location, int level, int kills, int ammo, TargetMode targetMode) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location;
        this.level = level;
        this.kills = kills;
        this.ammo = ammo;
        this.lastShot = 0;
        this.targetMode = targetMode != null ? targetMode : TargetMode.ALL_ENTITIES;
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

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(TargetMode targetMode) {
        this.targetMode = targetMode;
    }

    public double getDamage() {
        return Turrets.getInstance().getConfigManager().getDamageForLevel(level);
    }

    public double getRange() {
        return Turrets.getInstance().getConfigManager().getRangeForLevel(level);
    }

    public int getMaxAmmo() {
        return Turrets.getInstance().getConfigManager().getAmmoForLevel(level);
    }

    public int getShootDelay() {
        return Turrets.getInstance().getConfigManager().getShootDelayForLevel(level);
    }

    public boolean canShoot() {
        return ammo > 0 && System.currentTimeMillis() - lastShot >= getShootDelay();
    }

    public void shoot() {
        lastShot = System.currentTimeMillis();
    }

    private boolean isHostileMob(Entity entity) {
        return entity instanceof Monster ||
                entity instanceof Slime ||
                entity instanceof Ghast ||
                entity instanceof Phantom ||
                entity instanceof Shulker ||
                entity instanceof EnderDragon ||
                entity instanceof Wither ||
                entity instanceof Warden ||
                entity instanceof Hoglin ||
                entity instanceof Piglin ||
                entity instanceof PiglinBrute ||
                entity instanceof Ravager ||
                entity instanceof Guardian ||
                entity instanceof ElderGuardian;
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

                if (targetMode == TargetMode.HOSTILE_ONLY) continue;
            }

            if (living instanceof ArmorStand ||
                    living instanceof Villager ||
                    living.isDead() ||
                    !living.isValid()) continue;

            if (targetMode == TargetMode.HOSTILE_ONLY) {
                if (!(living instanceof Player) && !isHostileMob(living)) {
                    continue;
                }
            }

            double distance = turretCenter.distance(living.getLocation());
            if (distance <= range && distance < minDistance) {
                minDistance = distance;
                nearest = living;
            }
        }

        return nearest;
    }
}