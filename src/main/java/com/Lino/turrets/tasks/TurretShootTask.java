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
            if (target == null) {
                continue;
            }

            shootAt(turret, target);
        }
    }

    private void shootAt(Turret turret, LivingEntity target) {
        turret.shoot();
        turret.useAmmo();

        Location turretLoc = turret.getLocation().clone().add(0.5, 1.5, 0.5);
        Location targetLoc = target.getEyeLocation();

        turretLoc.getWorld().playSound(turretLoc, Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);

        Vector direction = targetLoc.toVector().subtract(turretLoc.toVector()).normalize();

        Arrow arrow = turretLoc.getWorld().spawn(turretLoc, Arrow.class);
        arrow.setVelocity(direction.multiply(3.0));
        arrow.setDamage(turret.getDamage());
        arrow.setCritical(true);
        arrow.setGlowing(turret.getLevel() >= 10);

        turretLoc.getWorld().spawnParticle(
                Particle.SMOKE,
                turretLoc,
                10,
                0.1, 0.1, 0.1,
                0.05
        );

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround() || ticks > 100) {
                    arrow.remove();
                    cancel();
                    return;
                }

                if (arrow.getLocation().distance(target.getLocation()) < 2.0) {
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
                        Particle.DUST,
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
