package com.Lino.turrets;

import org.bukkit.plugin.java.JavaPlugin;
import com.Lino.turrets.commands.TurretsCommand;
import com.Lino.turrets.listeners.TurretPlaceListener;
import com.Lino.turrets.listeners.TurretInteractListener;
import com.Lino.turrets.managers.*;
import com.Lino.turrets.tasks.TurretShootTask;

public class Turrets extends JavaPlugin {
    private static Turrets instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private TurretManager turretManager;
    private HologramManager hologramManager;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        databaseManager = new DatabaseManager(this);
        turretManager = new TurretManager(this);
        hologramManager = new HologramManager(this);
        guiManager = new GuiManager(this);

        databaseManager.initialize();
        turretManager.loadTurrets();

        getCommand("turrets").setExecutor(new TurretsCommand(this));

        getServer().getPluginManager().registerEvents(new TurretPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new TurretInteractListener(this), this);

        new TurretShootTask(this).runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        turretManager.saveTurrets();
        hologramManager.removeAllHolograms();
        databaseManager.close();
    }

    public static Turrets getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TurretManager getTurretManager() {
        return turretManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
    }
}