package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Turrets plugin;
    private FileConfiguration config;
    private Map<Integer, LevelConfig> levelConfigs;
    private int maxTurretsPerPlayer;

    public ConfigManager(Turrets plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = plugin.getConfig();
        loadLevelConfigs();
        maxTurretsPerPlayer = config.getInt("max-turrets-per-player", 5);
    }

    private void loadLevelConfigs() {
        levelConfigs = new HashMap<>();
        if (config.contains("levels")) {
            for (String key : config.getConfigurationSection("levels").getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    String path = "levels." + key;

                    int kills = config.getInt(path + ".kills", level * 500);
                    double damage = config.getDouble(path + ".damage", 2.0 + (level - 1) * 0.5);
                    int ammo = config.getInt(path + ".ammo", 100 + (level - 1) * 20);
                    double range = config.getDouble(path + ".range", 10.0 + (level - 1) * 0.5);
                    int shootDelay = config.getInt(path + ".shoot-delay", 1000);

                    levelConfigs.put(level, new LevelConfig(kills, damage, ammo, range, shootDelay));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            for (int i = 1; i <= 20; i++) {
                int kills = i == 1 ? 0 : i * 500;
                double damage = 2.0 + (i - 1) * 0.5;
                int ammo = 100 + (i - 1) * 20;
                double range = 10.0 + (i - 1) * 0.5;
                int shootDelay = 1000;
                levelConfigs.put(i, new LevelConfig(kills, damage, ammo, range, shootDelay));
            }
        }
    }

    public int getKillsForLevel(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.kills : level * 500;
    }

    public double getDamageForLevel(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.damage : 2.0 + (level - 1) * 0.5;
    }

    public int getAmmoForLevel(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.ammo : 100 + (level - 1) * 20;
    }

    public double getRangeForLevel(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.range : 10.0 + (level - 1) * 0.5;
    }

    public int getShootDelayForLevel(int level) {
        LevelConfig config = levelConfigs.get(level);
        return config != null ? config.shootDelay : 1000;
    }

    public int getMaxTurretsPerPlayer() {
        return maxTurretsPerPlayer;
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }

    private static class LevelConfig {
        final int kills;
        final double damage;
        final int ammo;
        final double range;
        final int shootDelay;

        LevelConfig(int kills, double damage, int ammo, double range, int shootDelay) {
            this.kills = kills;
            this.damage = damage;
            this.ammo = ammo;
            this.range = range;
            this.shootDelay = shootDelay;
        }
    }
}