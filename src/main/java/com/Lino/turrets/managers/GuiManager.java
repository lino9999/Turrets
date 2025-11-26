package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiManager implements Listener {
    private final Turrets plugin;
    private final Map<UUID, UUID> openGuis;

    public GuiManager(Turrets plugin) {
        this.plugin = plugin;
        this.openGuis = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openTurretGui(Player player, Turret turret) {
        String title = plugin.getMessageManager().getMessage("gui.title");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        // Info Item
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(plugin.getMessageManager().getMessage("gui.info.name"));

        List<String> lore = plugin.getMessageManager().getList("gui.info.lore",
                "{owner}", turret.getOwnerName(),
                "{level}", String.valueOf(turret.getLevel()),
                "{kills}", String.valueOf(turret.getKills()),
                "{damage}", String.format("%.1f", turret.getDamage()),
                "{range}", String.format("%.1f", turret.getRange()),
                "{rate}", String.format("%.2f", 1000.0 / turret.getShootDelay())
        );

        if (turret.getLevel() < 20) {
            lore.addAll(plugin.getMessageManager().getList("gui.info.next_level",
                    "{kills}", String.valueOf(plugin.getConfigManager().getKillsForLevel(turret.getLevel() + 1))));
        } else {
            lore.addAll(plugin.getMessageManager().getList("gui.info.max_level"));
        }

        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);

        // Ammo Item
        ItemStack ammoItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta ammoMeta = ammoItem.getItemMeta();
        ammoMeta.setDisplayName(plugin.getMessageManager().getMessage("gui.ammo.name"));
        ammoMeta.setLore(plugin.getMessageManager().getList("gui.ammo.lore",
                "{current}", String.valueOf(turret.getAmmo()),
                "{max}", String.valueOf(turret.getMaxAmmo())));
        ammoItem.setItemMeta(ammoMeta);

        // Target Mode Item
        ItemStack targetModeItem = new ItemStack(turret.getTargetMode() == Turret.TargetMode.ALL_ENTITIES ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta targetMeta = targetModeItem.getItemMeta();
        targetMeta.setDisplayName(plugin.getMessageManager().getMessage("gui.target.name"));
        String modeName = plugin.getMessageManager().getMessage("gui.target.modes." + turret.getTargetMode().name());
        targetMeta.setLore(plugin.getMessageManager().getList("gui.target.lore", "{mode}", modeName));
        targetModeItem.setItemMeta(targetMeta);

        // Remove Item
        ItemStack removeItem = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = removeItem.getItemMeta();
        removeMeta.setDisplayName(plugin.getMessageManager().getMessage("gui.remove.name"));
        removeMeta.setLore(plugin.getMessageManager().getList("gui.remove.lore"));
        removeItem.setItemMeta(removeMeta);

        gui.setItem(10, infoItem);
        gui.setItem(12, ammoItem);
        gui.setItem(14, targetModeItem);
        gui.setItem(16, removeItem);

        openGuis.put(player.getUniqueId(), turret.getId());
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID turretId = openGuis.get(player.getUniqueId());

        if (turretId == null) return;

        String expectedTitle = plugin.getMessageManager().getMessage("gui.title");
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().getType() != InventoryType.CHEST) {
            return;
        }

        Turret turret = plugin.getTurretManager().getTurret(turretId);
        if (turret == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();

        if (slot == 12) {
            handleAmmoReload(player, turret);
        } else if (slot == 14) {
            handleTargetModeToggle(player, turret);
        } else if (slot == 16) {
            handleTurretRemoval(player, turret);
        }
    }

    private void handleAmmoReload(Player player, Turret turret) {
        int nuggetCount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GOLD_NUGGET) {
                nuggetCount += item.getAmount();
            }
        }

        if (nuggetCount == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("turret.no_ammo"));
            return;
        }

        int maxAmmo = turret.getMaxAmmo();
        int currentAmmo = turret.getAmmo();
        int needed = maxAmmo - currentAmmo;
        int toUse = Math.min(needed, nuggetCount);

        if (toUse > 0) {
            int remaining = toUse;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.GOLD_NUGGET) {
                    int amount = item.getAmount();
                    if (amount <= remaining) {
                        item.setAmount(0);
                        remaining -= amount;
                    } else {
                        item.setAmount(amount - remaining);
                        remaining = 0;
                    }
                    if (remaining == 0) break;
                }
            }

            turret.setAmmo(currentAmmo + toUse);
            plugin.getHologramManager().updateHologram(turret);
            plugin.getTurretManager().saveTurrets();
            player.sendMessage(plugin.getMessageManager().getMessage("turret.reloaded", "{amount}", String.valueOf(toUse)));
            player.closeInventory();
        }
    }

    private void handleTargetModeToggle(Player player, Turret turret) {
        Turret.TargetMode currentMode = turret.getTargetMode();
        Turret.TargetMode newMode = currentMode == Turret.TargetMode.ALL_ENTITIES
                ? Turret.TargetMode.HOSTILE_ONLY
                : Turret.TargetMode.ALL_ENTITIES;

        turret.setTargetMode(newMode);
        plugin.getTurretManager().saveTurrets();

        String modeName = plugin.getMessageManager().getMessage("gui.target.modes." + newMode.name());
        player.sendMessage(plugin.getMessageManager().getMessage("turret.target_mode_changed", "{mode}", modeName));

        Inventory currentInv = player.getOpenInventory().getTopInventory();

        ItemStack targetModeItem = new ItemStack(newMode == Turret.TargetMode.ALL_ENTITIES ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta targetMeta = targetModeItem.getItemMeta();
        targetMeta.setDisplayName(plugin.getMessageManager().getMessage("gui.target.name"));
        targetMeta.setLore(plugin.getMessageManager().getList("gui.target.lore", "{mode}", modeName));
        targetModeItem.setItemMeta(targetMeta);

        currentInv.setItem(14, targetModeItem);
    }

    private void handleTurretRemoval(Player player, Turret turret) {
        ItemStack turretItem = plugin.getTurretManager().createTurretItem(
                turret.getLevel(),
                turret.getKills(),
                turret.getAmmo()
        );

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(turretItem);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), turretItem);
        }

        plugin.getTurretManager().removeTurret(turret.getId());
        Location loc = turret.getLocation();
        loc.getBlock().setType(Material.AIR);

        player.sendMessage(plugin.getMessageManager().getMessage("turret.removed"));
        player.closeInventory();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }
}