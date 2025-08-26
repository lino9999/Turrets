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
        String title = plugin.getMessageManager().applyGradient("Turret Management", "#ff8800", "#ffff00");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        String infoTitle = plugin.getMessageManager().applyGradient("Turret Information", "#00ff00", "#00ffff");
        infoMeta.setDisplayName(infoTitle);

        List<String> lore = new ArrayList<>(Arrays.asList(
                "§7Owner: §a" + turret.getOwnerName(),
                "§7Level: §e" + turret.getLevel() + "/20",
                "§7Kills: §c" + turret.getKills(),
                "§7Damage: §c" + String.format("%.1f", turret.getDamage()),
                "§7Range: §b" + String.format("%.1f", turret.getRange()) + " blocks",
                "§7Fire Rate: §d" + String.format("%.2f", 1000.0 / turret.getShootDelay()) + " shots/sec"
        ));

        if (turret.getLevel() < 20) {
            lore.add("");
            lore.add("§7Next level at: §e" + plugin.getConfigManager().getKillsForLevel(turret.getLevel() + 1) + " kills");
        } else {
            lore.add("");
            String maxLevel = plugin.getMessageManager().applyGradient("MAX LEVEL REACHED!", "#ffff00", "#ff00ff");
            lore.add(maxLevel);
        }

        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);

        ItemStack ammoItem = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta ammoMeta = ammoItem.getItemMeta();
        ammoMeta.setDisplayName("§eAmmo Status");
        ammoMeta.setLore(Arrays.asList(
                "§7Current: §a" + turret.getAmmo() + "/" + turret.getMaxAmmo(),
                "",
                "§eClick to reload!",
                "§7Requires gold nuggets"
        ));
        ammoItem.setItemMeta(ammoMeta);

        ItemStack targetModeItem = new ItemStack(turret.getTargetMode() == Turret.TargetMode.ALL_ENTITIES ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta targetMeta = targetModeItem.getItemMeta();
        targetMeta.setDisplayName("§6Target Mode");
        targetMeta.setLore(Arrays.asList(
                "",
                "§7Current: §e" + turret.getTargetMode().getDisplayName(),
                "",
                "§8• §fAll Entities §7- Attacks all mobs and players",
                "§8• §fHostile Only §7- Only attacks hostile mobs",
                "",
                "§eClick to toggle!"
        ));
        targetModeItem.setItemMeta(targetMeta);

        ItemStack removeItem = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = removeItem.getItemMeta();
        removeMeta.setDisplayName("§cRemove Turret");
        removeMeta.setLore(Arrays.asList(
                "§7Click to remove this turret",
                "§aProgress will be saved!",
                "§c§lThis action cannot be undone!"
        ));
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

        String expectedTitle = plugin.getMessageManager().applyGradient("Turret Management", "#ff8800", "#ffff00");
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

        String modeMessage = plugin.getMessageManager().applyGradient(
                "Target mode changed to: " + newMode.getDisplayName(),
                "#00ff00", "#00ffff"
        );
        player.sendMessage(modeMessage);

        Inventory currentInv = player.getOpenInventory().getTopInventory();

        ItemStack targetModeItem = new ItemStack(newMode == Turret.TargetMode.ALL_ENTITIES ? Material.IRON_SWORD : Material.WOODEN_SWORD);
        ItemMeta targetMeta = targetModeItem.getItemMeta();
        targetMeta.setDisplayName("§6Target Mode");
        targetMeta.setLore(Arrays.asList(
                "",
                "§7Current: §e" + newMode.getDisplayName(),
                "",
                "§8• §fAll Entities §7- Attacks all mobs and players",
                "§8• §fHostile Only §7- Only attacks hostile mobs",
                "",
                "§eClick to toggle!"
        ));
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