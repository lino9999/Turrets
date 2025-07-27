package com.Lino.turrets.tasks;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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

        Vector direction = targetLoc.toVector().subtract(turretLoc.toVector());
        double distance = turretLoc.distance(targetLoc);

        int particles = (int) (distance * 3);
        for (int i = 0; i < particles; i++) {
            double progress = (double) i / particles;
            Location particleLoc = turretLoc.clone().add(direction.clone().multiply(progress));
            particleLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 1)
            );
        }

        double damage = turret.getDamage();
        target.damage(damage);

        if (target.isDead()) {
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

        plugin.getHologramManager().updateHologram(turret);

        if (turret.getAmmo() == 0) {
            Player owner = plugin.getServer().getPlayer(turret.getOwnerId());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(plugin.getMessageManager().getMessage("turret.out_of_ammo"));
            }
        }
    }
}