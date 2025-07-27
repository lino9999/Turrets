package com.Lino.turrets.commands;

import com.Lino.turrets.Turrets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TurretsCommand implements CommandExecutor, TabCompleter {
    private final Turrets plugin;

    public TurretsCommand(Turrets plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (!sender.hasPermission("turrets.give")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                    return true;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("player_only"));
                    return true;
                }

                Player player = (Player) sender;
                player.getInventory().addItem(plugin.getTurretManager().createTurretItem());
                player.sendMessage(plugin.getMessageManager().getMessage("turret.given"));
                return true;

            case "reload":
                if (!sender.hasPermission("turrets.reload")) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("no_permission"));
                    return true;
                }

                plugin.reload();
                sender.sendMessage(plugin.getMessageManager().getMessage("plugin_reloaded"));
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§lTurrets Commands:");
        sender.sendMessage("§e/turrets give §7- Get a turret");
        sender.sendMessage("§e/turrets reload §7- Reload configuration");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> commands = Arrays.asList("give", "reload");

            String input = args[0].toLowerCase();
            for (String cmd : commands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }

            return completions;
        }

        return new ArrayList<>();
    }
}