package com.Lino.turrets.tasks;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.ref.WeakReference;

public class TurretIndividualTask extends BukkitRunnable {
    private final Turrets plugin;
    private final WeakReference<Turret> turretRef;
    private int soundTicks = 0;
    private int hologramCheckTicks = 0;
    private static final Particle.DustOptions ORANGE_DUST = new Particle.DustOptions(Color.ORANGE, 1.0f);
    private static final Particle.DustOptions YELLOW_DUST = new Particle.DustOptions(Color.YELLOW, 1.2f);

    public TurretIndividualTask(Turrets plugin, Turret turret) {
        this.plugin = plugin;
        this.turretRef = new WeakReference<>(turret);
    }

    @Override
    public void run() {
        Turret turret = turretRef.get();

        if (turret == null || plugin.getTurretManager().getTurret(turret.getId()) == null) {
            cancel();
            return;
        }

        soundTicks++;
        hologramCheckTicks++;

        if (hologramCheckTicks >= 100) {
            plugin.getHologramManager().updateHologram(turret);
            hologramCheckTicks = 0;
        }

        if (soundTicks >= 200) {
            Location turretLoc = turret.getLocation().clone().add(0.5, 0.5, 0.5);
            turretLoc.getWorld().playSound(turretLoc, Sound.BLOCK_BEACON_AMBIENT, 0.15f, 0.5f);
            soundTicks = 0;
        }

        if (!turret.canShoot()) {
            return;
        }

        LivingEntity target = turret.findNearestTarget();
        if (target == null || target.isDead() || !target.isValid()) {
            return;
        }

        shootAt(turret, target);
    }

    private void shootAt(Turret turret, LivingEntity target) {
        if (target instanceof Player) {
            Player playerTarget = (Player) target;
            if (playerTarget.getUniqueId().equals(turret.getOwnerId())) {
                return;
            }
        }

        turret.shoot();
        turret.useAmmo();

        Location turretLoc = turret.getLocation().clone().add(0.5, 1.5, 0.5);
        World world = turretLoc.getWorld();

        world.playSound(turretLoc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.5f);
        world.spawnParticle(Particle.FLAME, turretLoc, 10, 0.1, 0.1, 0.1, 0.03);

        new ProjectileTask(plugin, turret, target, turretLoc.clone()).runTaskTimer(plugin, 0L, 1L);

        plugin.getHologramManager().updateHologram(turret);

        if (turret.getAmmo() == 0) {
            Player owner = plugin.getServer().getPlayer(turret.getOwnerId());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(plugin.getMessageManager().getMessage("turret.out_of_ammo"));
            }
        }
    }

    private static class ProjectileTask extends BukkitRunnable {
        private final Turrets plugin;
        private final WeakReference<Turret> turretRef;
        private final WeakReference<LivingEntity> targetRef;
        private Location projectileLoc;
        private final double speed = 1.5;
        private int ticksAlive = 0;
        private static final int MAX_TICKS = 40;

        ProjectileTask(Turrets plugin, Turret turret, LivingEntity target, Location startLoc) {
            this.plugin = plugin;
            this.turretRef = new WeakReference<>(turret);
            this.targetRef = new WeakReference<>(target);
            this.projectileLoc = startLoc;
        }

        @Override
        public void run() {
            LivingEntity target = targetRef.get();
            Turret turret = turretRef.get();

            if (target == null || turret == null || !target.isValid() || target.isDead() || ticksAlive > MAX_TICKS) {
                cancel();
                return;
            }

            Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
            Vector direction = targetLoc.toVector().subtract(projectileLoc.toVector());

            if (direction.length() < 1.0) {
                target.damage(turret.getDamage());

                World world = targetLoc.getWorld();
                world.spawnParticle(Particle.CRIT, targetLoc, 10, 0.2, 0.2, 0.2, 0.1);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0.05);
                world.playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.0f);

                if (target.isDead() || target.getHealth() <= 0) {
                    turret.addKill();
                    plugin.getTurretManager().checkLevelUp(turret);

                    if (target instanceof Player) {
                        Player killed = (Player) target;
                        plugin.getServer().broadcastMessage(
                                plugin.getMessageManager().getMessage("turret.killed_player",
                                        "{owner}", turret.getOwnerName(),
                                        "{player}", killed.getName())
                        );
                    }
                }

                cancel();
                return;
            }

            direction.normalize().multiply(speed);
            projectileLoc.add(direction);

            World world = projectileLoc.getWorld();
            Particle.DustOptions dust = turret.getLevel() >= 10 ? YELLOW_DUST : ORANGE_DUST;
            world.spawnParticle(Particle.DUST, projectileLoc, 2, 0, 0, 0, 0, dust);

            if (ticksAlive % 2 == 0) {
                world.spawnParticle(Particle.FLAME, projectileLoc, 1, 0, 0, 0, 0);
            }

            ticksAlive++;
        }
    }
}