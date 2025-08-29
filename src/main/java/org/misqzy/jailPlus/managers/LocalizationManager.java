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
import java.util.regex.Pattern;

public class LocalizationManager {

    private final JailPlus plugin;
    private final ConfigManager configManager;
    private Map<String, FileConfiguration> messages;
    private String currentLanguage;

    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^<>]*>");
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

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
            plugin.getLogger().info("Loaded " + language + " localization with mixed format support");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading language file: " + language, e);
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


    public Component getMessage(String key, Object... placeholders) {
        String message = getRawMessage(key, placeholders);

        return parseMessage(message);
    }

    public Component parseMessage(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        try {
            boolean hasLegacy = LEGACY_PATTERN.matcher(message).find();
            boolean hasMiniMessage = MINI_MESSAGE_PATTERN.matcher(message).find();

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Parsing message: '" + message + "' (Legacy: " + hasLegacy + ", MiniMessage: " + hasMiniMessage + ")");
            }

            Component result;

            if (hasLegacy && hasMiniMessage) {
                result = parseMixedFormat(message);
            } else if (hasMiniMessage) {
                result = MINI_MESSAGE.deserialize(message);
            } else if (hasLegacy) {
                result = LEGACY_SERIALIZER.deserialize(message);
            } else {
                result = Component.text(message);
            }

            return result;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing message: '" + message + "'", e);
            return Component.text(message);
        }
    }


    private Component parseMixedFormat(String message) {
        try {
            String strategy = "convert-to-mini";

            if ("legacy-first".equals(strategy)) {
                return parseLegacyFirstApproach(message);
            } else {

                String convertedMessage = convertLegacyToMiniMessage(message);

                return MINI_MESSAGE.deserialize(convertedMessage);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing mixed format message: '" + message + "'", e);

            try {
                Component legacyComponent = LEGACY_SERIALIZER.deserialize(message);
                String plainText = PLAIN_SERIALIZER.serialize(legacyComponent);

                if (MINI_MESSAGE_PATTERN.matcher(plainText).find()) {
                    return MINI_MESSAGE.deserialize(plainText);
                } else {
                    return legacyComponent;
                }
            } catch (Exception fallbackError) {
                plugin.getLogger().log(Level.WARNING, "Fallback parsing also failed for: '" + message + "'", fallbackError);
                return Component.text(message);
            }
        }
    }

    private String convertLegacyToMiniMessage(String message) {
        Map<String, String> legacyToMiniMap = new HashMap<>();

        legacyToMiniMap.put("&0", "<black>");
        legacyToMiniMap.put("&1", "<dark_blue>");
        legacyToMiniMap.put("&2", "<dark_green>");
        legacyToMiniMap.put("&3", "<dark_aqua>");
        legacyToMiniMap.put("&4", "<dark_red>");
        legacyToMiniMap.put("&5", "<dark_purple>");
        legacyToMiniMap.put("&6", "<gold>");
        legacyToMiniMap.put("&7", "<gray>");
        legacyToMiniMap.put("&8", "<dark_gray>");
        legacyToMiniMap.put("&9", "<blue>");
        legacyToMiniMap.put("&a", "<green>");
        legacyToMiniMap.put("&b", "<aqua>");
        legacyToMiniMap.put("&c", "<red>");
        legacyToMiniMap.put("&d", "<light_purple>");
        legacyToMiniMap.put("&e", "<yellow>");
        legacyToMiniMap.put("&f", "<white>");

        legacyToMiniMap.put("&k", "<obfuscated>");
        legacyToMiniMap.put("&l", "<bold>");
        legacyToMiniMap.put("&m", "<strikethrough>");
        legacyToMiniMap.put("&n", "<underlined>");
        legacyToMiniMap.put("&o", "<italic>");
        legacyToMiniMap.put("&r", "<reset>");

        String result = message;
        for (Map.Entry<String, String> entry : legacyToMiniMap.entrySet()) {
            String legacyCode = entry.getKey();
            String miniCode = entry.getValue();

            result = result.replace(legacyCode, miniCode);
            result = result.replace(legacyCode.toUpperCase(), miniCode);
        }

        return result;
    }


    private Component parseLegacyFirstApproach(String message) {
        try {

            Component component = LEGACY_SERIALIZER.deserialize(message);

            String intermediateText = PLAIN_SERIALIZER.serialize(component);

            if (MINI_MESSAGE_PATTERN.matcher(intermediateText).find()) {
                return MINI_MESSAGE.deserialize(intermediateText);
            } else {
                return component;
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Legacy-first parsing failed for: '" + message + "'", e);
            return Component.text(message);
        }
    }

    private String replacePlaceholders(String message, Object... placeholders) {
        if (message.contains("{prefix}")) {
            if (configManager.isUsePrefix()) {
                message = message.replace("{prefix}", getPrefix() + " ");
            } else {
                message = message.replace("{prefix}", "");
            }
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
        plugin.getLogger().info("Messages reloaded for language: " + currentLanguage + " with mixed format support");
    }

    public String getPrefix() {
        return getRawMessage("prefix");
    }

    public void sendMessage(CommandSender sender, String key, Object... placeholders) {
        if (sender instanceof Player) {
            sender.sendMessage(getMessage(key, placeholders));
        } else {
            plugin.getLogger().info(PLAIN_SERIALIZER.serialize(getMessage(key, placeholders)));
        }
    }


    public void sendFormattedMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            sender.sendMessage(parseMessage(message));
        } else {
            plugin.getLogger().info(PLAIN_SERIALIZER.serialize(parseMessage(message)));
        }
    }


    public String getFormatInfo(String message) {
        boolean hasLegacy = LEGACY_PATTERN.matcher(message).find();
        boolean hasMiniMessage = MINI_MESSAGE_PATTERN.matcher(message).find();

        if (hasLegacy && hasMiniMessage) {
            return "Mixed (Legacy + MiniMessage)";
        } else if (hasMiniMessage) {
            return "MiniMessage only";
        } else if (hasLegacy) {
            return "Legacy only";
        } else {
            return "Plain text";
        }
    }


    public boolean hasLegacyFormat(String message) {
        return LEGACY_PATTERN.matcher(message).find();
    }


    public boolean hasMiniMessageFormat(String message) {
        return MINI_MESSAGE_PATTERN.matcher(message).find();
    }


    public String stripFormatting(String message) {
        try {
            Component component = parseMessage(message);
            return PLAIN_SERIALIZER.serialize(component);
        } catch (Exception e) {
            String result = message;
            result = LEGACY_PATTERN.matcher(result).replaceAll("");
            result = MINI_MESSAGE_PATTERN.matcher(result).replaceAll("");
            return result;
        }
    }
}