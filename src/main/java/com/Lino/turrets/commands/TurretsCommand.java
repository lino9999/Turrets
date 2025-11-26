package com.Lino.turrets.commands;

import com.Lino.turrets.Turrets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

                if (args.length < 4) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.usage"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.player_not_found"));
                    return true;
                }

                int level;
                int amount;
                try {
                    level = Integer.parseInt(args[2]);
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.not_number"));
                    return true;
                }

                if (level < 1 || level > 20) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.invalid_level"));
                    return true;
                }

                if (amount < 1 || amount > 64) {
                    sender.sendMessage(plugin.getMessageManager().getMessage("command.invalid_amount"));
                    return true;
                }

                for (int i = 0; i < amount; i++) {
                    target.getInventory().addItem(plugin.getTurretManager().createTurretItem(level, 0, plugin.getConfigManager().getAmmoForLevel(level)));
                }

                String giveMsg = plugin.getMessageManager().getMessage("command.give_sender",
                        "{amount}", String.valueOf(amount),
                        "{level}", String.valueOf(level),
                        "{player}", target.getName());
                sender.sendMessage(giveMsg);

                String receiveMsg = plugin.getMessageManager().getMessage("command.give_receiver",
                        "{amount}", String.valueOf(amount),
                        "{level}", String.valueOf(level));
                target.sendMessage(receiveMsg);
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
        sender.sendMessage(plugin.getMessageManager().getMessage("command.help.header"));
        sender.sendMessage(plugin.getMessageManager().getMessage("command.help.give"));
        sender.sendMessage(plugin.getMessageManager().getMessage("command.help.reload"));
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

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> levels = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String level = String.valueOf(i);
                if (level.startsWith(args[2])) {
                    levels.add(level);
                }
            }
            return levels;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            List<String> amounts = Arrays.asList("1", "5", "10", "16", "32", "64");
            return amounts.stream()
                    .filter(amount -> amount.startsWith(args[3]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}