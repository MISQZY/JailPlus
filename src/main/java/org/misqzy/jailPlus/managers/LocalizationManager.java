package org.misqzy.jailPlus.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.misqzy.jailPlus.JailPlus;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class LocalizationManager {

    private final JailPlus plugin;
    private final ConfigManager configManager;
    private Map<String, FileConfiguration> messages;
    private String currentLanguage;

    public LocalizationManager(JailPlus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messages = new HashMap<>();

        setupMessages();
    }


    private void setupMessages() {
        currentLanguage = configManager.getLanguage();

        File messagesDir = new File(plugin.getDataFolder(), "messages");
        if (!messagesDir.exists()) {
            messagesDir.mkdirs();
        }

        loadLanguageFiles();
    }

    private void loadLanguageFiles() {
        String[] languages = {"en", "ru"};

        for (String lang : languages) {
            loadLanguageFile(lang);
        }
    }


    private void loadLanguageFile(String language) {
        File messageFile = new File(plugin.getDataFolder(), "messages/messages_" + language + ".yml");

        if (!messageFile.exists()) {
            plugin.saveResource("messages/messages_" + language + ".yml", false);
        }

        try {
            FileConfiguration messageConfig = YamlConfiguration.loadConfiguration(messageFile);

            InputStream defaultStream = plugin.getResource("messages/messages_" + language + ".yml");
            if (defaultStream != null) {
                FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
                );
                messageConfig.setDefaults(defaultConfig);
                messageConfig.options().copyDefaults(true);
                messageConfig.save(messageFile);
            }

            messages.put(language, messageConfig);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error when try to load: " + language, e);
        }
    }

    public String getRawMessage(String key, Object... placeholders) {
        FileConfiguration messageConfig = messages.get(currentLanguage);
        if (messageConfig == null) messageConfig = messages.get("en");
        if (messageConfig == null) return "Missing message: " + key;

        String message = messageConfig.getString(key, "Missing message: " + key);
        message = replacePlaceholders(message, placeholders);

        return message;
    }

    public Component getMessage(String key, Object... placeholders)
    {
       String message = getRawMessage(key,placeholders);

        if (message.contains("<")) {
            return MiniMessage.miniMessage().deserialize(message);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }
    }

    private String replacePlaceholders(String message, Object... placeholders) {
        if (message.contains("{prefix}")) {
            if (configManager.isUsePrefix()) {
                message = message.replace("{prefix}", getPrefix() + " ");
            }
            else
                message = message.replace("{prefix}", "");
        }
        if (placeholders.length == 0) {
            return message;
        }

        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(placeholders[i]));
        }

        return message;
    }


    public void reloadMessages() {
        messages.clear();
        currentLanguage = configManager.getLanguage();
        loadLanguageFiles();
        plugin.getLogger().info("Messages reloaded for language: " + currentLanguage);
    }


    public String getPrefix() {
        return getRawMessage("prefix");
    }


    public void sendMessage(CommandSender sender, String key, Object... placeholders) {
        if (sender instanceof Player) {
            sender.sendMessage(getMessage(key, placeholders));
        } else
        {
            plugin.getLogger().info(PlainTextComponentSerializer.plainText().serialize(getMessage(key, placeholders)));
        }
    }

}
