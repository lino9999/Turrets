package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Turrets plugin;
    private FileConfiguration config;
    private Map<Integer, Integer> levelKills;
    private int maxTurretsPerPlayer;

    public ConfigManager(Turrets plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = plugin.getConfig();
        loadLevelKills();
        maxTurretsPerPlayer = config.getInt("max-turrets-per-player", 5);
    }

    private void loadLevelKills() {
        levelKills = new HashMap<>();
        if (config.contains("levels")) {
            for (String key : config.getConfigurationSection("levels").getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int kills = config.getInt("levels." + key);
                    levelKills.put(level, kills);
                } catch (NumberFormatException ignored) {}
            }
        } else {
            for (int i = 2; i <= 20; i++) {
                levelKills.put(i, i * 500);
            }
        }
    }

    public int getKillsForLevel(int level) {
        return levelKills.getOrDefault(level, level * 500);
    }

    public int getMaxTurretsPerPlayer() {
        return maxTurretsPerPlayer;
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }
}