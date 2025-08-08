package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
    private final Turrets plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private final Pattern gradientPattern = Pattern.compile("<gradient:(#[a-fA-F0-9]{6}):(#[a-fA-F0-9]{6})>(.*?)</gradient>");
    private final Pattern variablePattern = Pattern.compile("\\{[^}]+\\}");

    public MessageManager(Turrets plugin) {
        this.plugin = plugin;
        createMessagesFile();
        reload();
    }

    private void createMessagesFile() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    public void reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "Message not found: " + path);
        return translateColors(message);
    }

    public String getMessage(String path, String... replacements) {
        String message = messagesConfig.getString(path, "Message not found: " + path);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        return translateColors(message);
    }

    private String translateColors(String message) {
        Matcher gradientMatcher = gradientPattern.matcher(message);
        StringBuffer result = new StringBuffer();

        while (gradientMatcher.find()) {
            String startColor = gradientMatcher.group(1);
            String endColor = gradientMatcher.group(2);
            String text = gradientMatcher.group(3);

            Matcher varMatcher = variablePattern.matcher(text);
            if (varMatcher.find()) {
                gradientMatcher.appendReplacement(result, Matcher.quoteReplacement(text));
            } else {
                String gradient = applyGradient(text, startColor, endColor);
                gradientMatcher.appendReplacement(result, Matcher.quoteReplacement(gradient));
            }
        }
        gradientMatcher.appendTail(result);
        message = result.toString();

        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, ChatColor.of(color) + "");
        }

        message = message.replace("&", "§");

        return message;
    }

    public String applyGradient(String text, String startHex, String endHex) {
        if (text == null || text.isEmpty()) return text;

        int[] start = hexToRgb(startHex);
        int[] end = hexToRgb(endHex);

        StringBuilder gradientText = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / Math.max(1, length - 1);

            int r = Math.round(start[0] + ratio * (end[0] - start[0]));
            int g = Math.round(start[1] + ratio * (end[1] - start[1]));
            int b = Math.round(start[2] + ratio * (end[2] - start[2]));

            String hex = String.format("#%02x%02x%02x", r, g, b);
            gradientText.append(ChatColor.of(hex)).append(text.charAt(i));
        }

        return gradientText.toString();
    }

    private int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[] {
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}