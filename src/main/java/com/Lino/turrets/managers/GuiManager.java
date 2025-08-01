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
        Inventory gui = Bukkit.createInventory(null, 27, "§6Turret Management");

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§6Turret Information");

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
            lore.add("§6§lMAX LEVEL REACHED!");
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

        ItemStack removeItem = new ItemStack(Material.BARRIER);
        ItemMeta removeMeta = removeItem.getItemMeta();
        removeMeta.setDisplayName("§cRemove Turret");
        removeMeta.setLore(Arrays.asList(
                "§7Click to remove this turret",
                "§aProgress will be saved!",
                "§c§lThis action cannot be undone!"
        ));
        removeItem.setItemMeta(removeMeta);

        gui.setItem(11, infoItem);
        gui.setItem(13, ammoItem);
        gui.setItem(15, removeItem);

        openGuis.put(player.getUniqueId(), turret.getId());
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        UUID turretId = openGuis.get(player.getUniqueId());

        if (turretId == null) return;
        if (!event.getView().getTitle().equals("§6Turret Management")) return;

        event.setCancelled(true);

        Turret turret = plugin.getTurretManager().getTurret(turretId);
        if (turret == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();

        if (slot == 13) {
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
        } else if (slot == 15) {
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

            plugin.getTurretManager().removeTurret(turretId);
            Location loc = turret.getLocation();
            loc.getBlock().setType(Material.AIR);

            player.sendMessage(plugin.getMessageManager().getMessage("turret.removed"));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }
}