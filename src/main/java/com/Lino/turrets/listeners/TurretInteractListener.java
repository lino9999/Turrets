package com.Lino.turrets.listeners;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TurretInteractListener implements Listener {
    private final TurretsPlugin plugin;

    public TurretInteractListener(TurretsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.DISPENSER) {
            return;
        }

        Turret turret = plugin.getTurretManager().getTurretAtLocation(block.getLocation());
        if (turret == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(turret.getOwnerId()) && !player.hasPermission("turrets.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("turret.not_owner"));
            return;
        }

        plugin.getGuiManager().openTurretGui(player, turret);
    }
}