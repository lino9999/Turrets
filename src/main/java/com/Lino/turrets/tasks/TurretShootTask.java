package com.Lino.turrets.tasks;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TurretShootTask extends BukkitRunnable {
    private final Turrets plugin;

    public TurretShootTask(Turrets plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Turret turret : plugin.getTurretManager().getAllTurrets()) {
            if (!turret.canShoot()) {
                continue;
            }

            LivingEntity target = turret.findNearestTarget();
            if (target == null || target.isDead() || !target.isValid()) {
                continue;
            }

            shootAt(turret, target);
        }
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
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

        turretLoc.getWorld().playSound(turretLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        Vector direction = targetLoc.toVector().subtract(turretLoc.toVector()).normalize();

        Arrow arrow = turretLoc.getWorld().spawn(turretLoc, Arrow.class);
        arrow.setShooter(null);
        arrow.setVelocity(direction.multiply(3.0));
        arrow.setDamage(turret.getDamage());
        arrow.setCritical(true);
        arrow.setGlowing(turret.getLevel() >= 10);
        arrow.setGravity(false);

        turretLoc.getWorld().spawnParticle(
                Particle.NOTE,
                turretLoc,
                10,
                0.1, 0.1, 0.1,
                0.05
        );

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (arrow.isDead() || ticks > 60) {
                    arrow.remove();
                    cancel();
                    return;
                }

                if (!target.isValid() || target.isDead()) {
                    arrow.remove();
                    cancel();
                    return;
                }

                Vector newDirection = target.getLocation().add(0, target.getHeight() / 2, 0)
                        .toVector().subtract(arrow.getLocation().toVector()).normalize();
                arrow.setVelocity(newDirection.multiply(3.0));

                if (arrow.getLocation().distance(target.getLocation()) < 1.5) {
                    target.damage(turret.getDamage());

                    target.getWorld().spawnParticle(
                            Particle.DAMAGE_INDICATOR,
                            target.getLocation().add(0, 1, 0),
                            5,
                            0.3, 0.3, 0.3,
                            0.1
                    );

                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 1.0f);

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

                    arrow.remove();
                    cancel();
                }

                arrow.getWorld().spawnParticle(
                        Particle.FLAME,
                        arrow.getLocation(),
                        1,
                        0, 0, 0, 0,
                        new Particle.DustOptions(Color.RED, 0.8f)
                );

                ticks++;
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