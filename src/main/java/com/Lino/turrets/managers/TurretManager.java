package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TurretManager {
    private final Turrets plugin;
    private final Map<UUID, Turret> turrets;
    private final Map<UUID, List<UUID>> playerTurrets;
    private final NamespacedKey turretKey;

    public TurretManager(Turrets plugin) {
        this.plugin = plugin;
        this.turrets = new ConcurrentHashMap<>();
        this.playerTurrets = new ConcurrentHashMap<>();
        this.turretKey = new NamespacedKey(plugin, "turret");
    }

    public void loadTurrets() {
        List<Turret> loadedTurrets = plugin.getDatabaseManager().loadTurrets();
        for (Turret turret : loadedTurrets) {
            turrets.put(turret.getId(), turret);
            playerTurrets.computeIfAbsent(turret.getOwnerId(), k -> new ArrayList<>()).add(turret.getId());
            plugin.getHologramManager().createHologram(turret);
        }
    }

    public void saveTurrets() {
        plugin.getDatabaseManager().saveTurrets(new ArrayList<>(turrets.values()));
    }

    public Turret createTurret(Player player, Location location) {
        Turret turret = new Turret(player.getUniqueId(), player.getName(), location);
        turrets.put(turret.getId(), turret);
        playerTurrets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(turret.getId());
        plugin.getHologramManager().createHologram(turret);
        saveTurrets();
        return turret;
    }

    public void removeTurret(UUID turretId) {
        Turret turret = turrets.remove(turretId);
        if (turret != null) {
            List<UUID> playerTurretList = playerTurrets.get(turret.getOwnerId());
            if (playerTurretList != null) {
                playerTurretList.remove(turretId);
            }
            plugin.getHologramManager().removeHologram(turretId);
            saveTurrets();
        }
    }

    public Turret getTurret(UUID turretId) {
        return turrets.get(turretId);
    }

    public Turret getTurretAtLocation(Location location) {
        for (Turret turret : turrets.values()) {
            if (turret.getLocation().getBlock().equals(location.getBlock())) {
                return turret;
            }
        }
        return null;
    }

    public List<Turret> getPlayerTurrets(UUID playerId) {
        List<UUID> turretIds = playerTurrets.get(playerId);
        if (turretIds == null) return new ArrayList<>();

        return turretIds.stream()
                .map(turrets::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public int getPlayerTurretCount(UUID playerId) {
        List<UUID> turretIds = playerTurrets.get(playerId);
        return turretIds != null ? turretIds.size() : 0;
    }

    public Collection<Turret> getAllTurrets() {
        return turrets.values();
    }

    public ItemStack createTurretItem() {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6Turret");
        meta.setLore(Arrays.asList(
                "§7Place to deploy",
                "§7Right-click to manage"
        ));

        meta.getPersistentDataContainer().set(turretKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        return item;
    }

    public boolean isTurretItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(turretKey, PersistentDataType.BYTE);
    }

    public void checkLevelUp(Turret turret) {
        int currentLevel = turret.getLevel();
        if (currentLevel >= 20) return;

        int killsRequired = plugin.getConfigManager().getKillsForLevel(currentLevel + 1);
        if (turret.getKills() >= killsRequired) {
            turret.setLevel(currentLevel + 1);
            plugin.getHologramManager().updateHologram(turret);
            saveTurrets();

            Player owner = plugin.getServer().getPlayer(turret.getOwnerId());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(plugin.getMessageManager().getMessage("turret.level_up")
                        .replace("{level}", String.valueOf(turret.getLevel())));
            }
        }
    }
}