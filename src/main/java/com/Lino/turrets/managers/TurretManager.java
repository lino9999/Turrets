package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import com.Lino.turrets.tasks.TurretIndividualTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.NamespacedKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TurretManager {
    private final Turrets plugin;
    private final Map<UUID, Turret> turrets;
    private final Map<UUID, List<UUID>> playerTurrets;
    private final Map<UUID, BukkitTask> turretTasks;
    private final NamespacedKey turretKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey killsKey;
    private final NamespacedKey ammoKey;

    public TurretManager(Turrets plugin) {
        this.plugin = plugin;
        this.turrets = new ConcurrentHashMap<>();
        this.playerTurrets = new ConcurrentHashMap<>();
        this.turretTasks = new ConcurrentHashMap<>();
        this.turretKey = new NamespacedKey(plugin, "turret");
        this.levelKey = new NamespacedKey(plugin, "turret_level");
        this.killsKey = new NamespacedKey(plugin, "turret_kills");
        this.ammoKey = new NamespacedKey(plugin, "turret_ammo");
    }

    public void loadTurrets() {
        List<Turret> loadedTurrets = plugin.getDatabaseManager().loadTurrets();
        for (Turret turret : loadedTurrets) {
            turrets.put(turret.getId(), turret);
            playerTurrets.computeIfAbsent(turret.getOwnerId(), k -> new ArrayList<>()).add(turret.getId());
            plugin.getHologramManager().createHologram(turret);
            startTurretTask(turret);
        }
    }

    public void saveTurrets() {
        plugin.getDatabaseManager().saveTurrets(new ArrayList<>(turrets.values()));
    }

    private void startTurretTask(Turret turret) {
        TurretIndividualTask task = new TurretIndividualTask(plugin, turret);
        BukkitTask bukkitTask = task.runTaskTimer(plugin, 0L, 1L);
        turretTasks.put(turret.getId(), bukkitTask);
    }

    private void stopTurretTask(UUID turretId) {
        BukkitTask task = turretTasks.remove(turretId);
        if (task != null) {
            task.cancel();
        }
    }

    public void stopAllTasks() {
        for (BukkitTask task : turretTasks.values()) {
            task.cancel();
        }
        turretTasks.clear();
    }

    public Turret createTurret(Player player, Location location, ItemStack item) {
        int level = 1;
        int kills = 0;
        int ammo = 0;

        if (item != null && item.hasItemMeta()) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            if (container.has(levelKey, PersistentDataType.INTEGER)) {
                level = container.get(levelKey, PersistentDataType.INTEGER);
            }
            if (container.has(killsKey, PersistentDataType.INTEGER)) {
                kills = container.get(killsKey, PersistentDataType.INTEGER);
            }
            if (container.has(ammoKey, PersistentDataType.INTEGER)) {
                ammo = container.get(ammoKey, PersistentDataType.INTEGER);
            }
        }

        Turret turret = new Turret(player.getUniqueId(), player.getName(), location, level, kills, ammo);
        turrets.put(turret.getId(), turret);
        playerTurrets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(turret.getId());
        plugin.getHologramManager().createHologram(turret);
        startTurretTask(turret);
        saveTurrets();
        return turret;
    }

    public Turret createTurret(Player player, Location location) {
        return createTurret(player, location, null);
    }

    public void removeTurret(UUID turretId) {
        Turret turret = turrets.remove(turretId);
        if (turret != null) {
            List<UUID> playerTurretList = playerTurrets.get(turret.getOwnerId());
            if (playerTurretList != null) {
                playerTurretList.remove(turretId);
            }
            plugin.getHologramManager().removeHologram(turretId);
            stopTurretTask(turretId);
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
        return createTurretItem(1, 0, 0);
    }

    public ItemStack createTurretItem(int level, int kills, int ammo) {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6Turret §7[§eLv." + level + "§7]");

        List<String> lore = new ArrayList<>();
        lore.add("§7Place to deploy");
        lore.add("§7Right-click to manage");
        lore.add("");
        lore.add("§7Level: §e" + level + "/20");
        lore.add("§7Kills: §c" + kills);
        lore.add("§7Ammo: §a" + ammo);

        meta.setLore(lore);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(turretKey, PersistentDataType.BYTE, (byte) 1);
        container.set(levelKey, PersistentDataType.INTEGER, level);
        container.set(killsKey, PersistentDataType.INTEGER, kills);
        container.set(ammoKey, PersistentDataType.INTEGER, ammo);

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