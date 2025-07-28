package com.Lino.turrets.tasks;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TurretIndividualTask extends BukkitRunnable {
    private final Turrets plugin;
    private final Turret turret;
    private int soundTicks = 0;

    public TurretIndividualTask(Turrets plugin, Turret turret) {
        this.plugin = plugin;
        this.turret = turret;
    }

    @Override
    public void run() {
        if (plugin.getTurretManager().getTurret(turret.getId()) == null) {
            cancel();
            return;
        }

        soundTicks++;
        if (soundTicks >= 10) {
            Location turretLoc = turret.getLocation().clone().add(0.5, 0.5, 0.5);
            turretLoc.getWorld().playSound(turretLoc, Sound.BLOCK_BEACON_AMBIENT, 0.2f, 0.5f);
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

        turretLoc.getWorld().playSound(turretLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.5f);

        turretLoc.getWorld().spawnParticle(
                Particle.FLAME,
                turretLoc,
                20,
                0.1, 0.1, 0.1,
                0.05
        );

        new BukkitRunnable() {
            Location projectileLoc = turretLoc.clone();
            double speed = 1.5;
            int ticksAlive = 0;
            int maxTicks = 40;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticksAlive > maxTicks) {
                    cancel();
                    return;
                }

                Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);
                Vector direction = targetLoc.toVector().subtract(projectileLoc.toVector());

                if (direction.length() < 1.0) {
                    target.damage(turret.getDamage());

                    targetLoc.getWorld().spawnParticle(
                            Particle.CRIT,
                            targetLoc,
                            15,
                            0.3, 0.3, 0.3,
                            0.2
                    );

                    targetLoc.getWorld().spawnParticle(
                            Particle.DAMAGE_INDICATOR,
                            target.getLocation().add(0, 1, 0),
                            5,
                            0.3, 0.3, 0.3,
                            0.1
                    );

                    targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

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

                if (turret.getLevel() >= 10) {
                    projectileLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            projectileLoc,
                            3,
                            0, 0, 0, 0,
                            new Particle.DustOptions(Color.YELLOW, 1.2f)
                    );
                } else {
                    projectileLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            projectileLoc,
                            3,
                            0, 0, 0, 0,
                            new Particle.DustOptions(Color.ORANGE, 1.0f)
                    );
                }

                projectileLoc.getWorld().spawnParticle(
                        Particle.FLAME,
                        projectileLoc,
                        1,
                        0, 0, 0, 0
                );

                ticksAlive++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        plugin.getHologramManager().updateHologram(turret);

        if (turret.getAmmo() == 0) {
            Player owner = plugin.getServer().getPlayer(turret.getOwnerId());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(plugin.getMessageManager().getMessage("turret.out_of_ammo"));
            }
        }
    }
}