package com.Lino.turrets.listeners;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class TurretPlaceListener implements Listener {
    private final Turrets plugin;

    public TurretPlaceListener(Turrets plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!plugin.getTurretManager().isTurretItem(item)) {
            return;
        }

        if (!player.hasPermission("turrets.use")) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
            return;
        }

        int playerTurrets = plugin.getTurretManager().getPlayerTurretCount(player.getUniqueId());
        int maxTurrets = plugin.getConfigManager().getMaxTurretsPerPlayer();

        if (playerTurrets >= maxTurrets) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessageManager().getMessage("turret.max_reached", "{max}", String.valueOf(maxTurrets)));
            return;
        }

        Block block = event.getBlock();
        plugin.getTurretManager().createTurret(player, block.getLocation());
        player.sendMessage(plugin.getMessageManager().getMessage("turret.placed"));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getType() != Material.DISPENSER) {
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