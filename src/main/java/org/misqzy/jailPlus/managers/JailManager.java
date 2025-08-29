package org.misqzy.jailPlus.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.JailData;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.utils.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class JailManager {

    private final JailPlus plugin;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;

    private final Map<String, JailData> jails;
    private final Map<UUID, PlayerJailData> jailedPlayers;

    private final Set<UUID> jailedPlayerUUIDs;

    private File jailsFile;
    private File playersFile;
    private FileConfiguration jailsConfig;
    private FileConfiguration playersConfig;

    private BukkitRunnable jailTimer;
    private volatile boolean timerRunning = false;

    public JailManager(JailPlus plugin, ConfigManager configManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
        this.jails = new ConcurrentHashMap<>();
        this.jailedPlayers = new ConcurrentHashMap<>();
        this.jailedPlayerUUIDs = ConcurrentHashMap.newKeySet();

        setupDataFiles();
        loadData();
        startJailTimer();
    }

    private void setupDataFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        jailsFile = new File(dataFolder, "jails.yml");
        playersFile = new File(dataFolder, "players.yml");

        if (!jailsFile.exists()) {
            try {
                jailsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating jails.yml: " + e);
            }
        }

        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating players.yml: " + e);
            }
        }

        jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    private void loadData() {
        loadJails();
        loadJailedPlayers();
    }

    private void loadJails() {
        jails.clear();

        if (jailsConfig.getConfigurationSection("jails") == null) {
            return;
        }

        for (String jailName : jailsConfig.getConfigurationSection("jails").getKeys(false)) {
            String path = "jails." + jailName;

            String worldName = jailsConfig.getString(path + ".world");
            double x = jailsConfig.getDouble(path + ".x");
            double y = jailsConfig.getDouble(path + ".y");
            double z = jailsConfig.getDouble(path + ".z");
            float yaw = (float) jailsConfig.getDouble(path + ".yaw");
            float pitch = (float) jailsConfig.getDouble(path + ".pitch");

            JailData jail = new JailData(jailName, worldName, x, y, z, yaw, pitch);
            jails.put(jailName.toLowerCase(), jail);
        }

        plugin.getLogger().fine("Loaded " + jails.size() + " jails");
    }

    private void loadJailedPlayers() {
        jailedPlayers.clear();
        jailedPlayerUUIDs.clear();

        if (playersConfig.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidString : playersConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                String path = "players." + uuidString;

                UUID uuid = UUID.fromString(uuidString);
                String playerName = playersConfig.getString(path + ".name");
                String jailName = playersConfig.getString(path + ".jail");
                long jailTime = playersConfig.getLong(path + ".jail-time");
                long startTime = playersConfig.getLong(path + ".start-time");
                String reason = playersConfig.getString(path + ".reason", "No reason");
                String jailedBy = playersConfig.getString(path + ".jailed-by", "Console");

                PlayerJailData jailData = new PlayerJailData(uuid, playerName, jailName, jailTime, reason, jailedBy);
                jailData.setStartTime(startTime);


                if (playersConfig.contains(path + ".previous-location")) {
                    String prevPath = path + ".previous-location";
                    String worldName = playersConfig.getString(prevPath + ".world");

                    if (worldName != null && Bukkit.getWorld(worldName) != null) {
                        double x = playersConfig.getDouble(prevPath + ".x");
                        double y = playersConfig.getDouble(prevPath + ".y");
                        double z = playersConfig.getDouble(prevPath + ".z");
                        float yaw = (float) playersConfig.getDouble(prevPath + ".yaw");
                        float pitch = (float) playersConfig.getDouble(prevPath + ".pitch");

                        Location prevLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                        jailData.setPreviousLocation(prevLocation);
                    }
                }

                jailedPlayers.put(uuid, jailData);
                jailedPlayerUUIDs.add(uuid);

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in players data: " + uuidString);
            }
        }

        plugin.getLogger().info("Loaded " + jailedPlayers.size() + " prisoners");
    }

    public boolean createJail(String name, Location location) {
        if (!validateLocation(location)) {
            return false;
        }

        String jailName = name.toLowerCase();
        if (jails.containsKey(jailName)) {
            return false;
        }

        JailData jail = new JailData(jailName, location);
        jails.put(jailName, jail);
        saveJails();


        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logJailCreate("Console", jailName, location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ());
        }

        return true;
    }

    public boolean deleteJail(String name) {
        String jailName = name.toLowerCase();
        if (!jails.containsKey(jailName)) {
            return false;
        }


        jailedPlayers.values().stream()
                .filter(data -> data.getJailName().equals(jailName))
                .map(PlayerJailData::getPlayerUuid)
                .forEach(this::unjailPlayer);

        jails.remove(jailName);
        saveJails();


        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logJailDelete("Console", jailName);
        }

        return true;
    }

    public boolean jailPlayer(Player player, String jailName, long time, String reason, String jailedBy) {
        if (isPlayerJailed(player)) {
            return false;
        }

        JailData jail = jails.get(jailName.toLowerCase());
        if (jail == null) {
            return false;
        }

        Location currentLocation = player.getLocation();
        Location jailLocation = jail.getLocation();

        if (!validateLocation(jailLocation)) {
            plugin.getLogger().warning("Invalid jail location for: " + jailName);
            return false;
        }

        PlayerJailData jailData = new PlayerJailData(
                player.getUniqueId(),
                player.getName(),
                jailName.toLowerCase(),
                time,
                reason,
                jailedBy
        );
        jailData.setPreviousLocation(currentLocation);


        player.teleport(jailLocation);


        jailedPlayers.put(player.getUniqueId(), jailData);
        jailedPlayerUUIDs.add(player.getUniqueId());

        savePlayers();


        if (configManager.isSoundsEnabled()) {
            try {
                Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(configManager.getJailSound().toLowerCase()));
                if (sound != null)
                    {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid jail sound: " + configManager.getJailSound());
            }
        }


        if (configManager.isBroadcastJail()) {
            Component comp = localizationManager.getMessage("jail.broadcast",
                    player.getName(), reason, TimeUtils.formatTime(time));
            Bukkit.broadcast(comp);
        }


        localizationManager.sendMessage(player, "jail.jailed",
                jailName, TimeUtils.formatTime(time), reason);


        if (plugin.getStatisticsManager() != null) {
            plugin.getStatisticsManager().addJailRecord(player.getUniqueId(), time, reason);
        }


        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logJail(player.getName(), jailedBy, jailName, time, reason);
        }

        return true;
    }

    public boolean unjailPlayer(UUID playerUuid) {
        PlayerJailData jailData = jailedPlayers.remove(playerUuid);
        if (jailData == null) {
            return false;
        }

        jailedPlayerUUIDs.remove(playerUuid);

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            Location returnLocation = jailData.getPreviousLocation();
            if (returnLocation != null && validateLocation(returnLocation)) {
                player.teleport(returnLocation);
            } else {
                // Fallback to spawn
                player.teleport(player.getWorld().getSpawnLocation());
            }


            if (configManager.isSoundsEnabled()) {
                try {
                    Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(configManager.getUnjailSound().toLowerCase()));
                    if (sound != null) {
                        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid unjail sound: " + configManager.getUnjailSound());
                }
            }

            if (configManager.isParticlesEnabled()) {
                Particle particle = null;

                try {
                    particle = Particle.valueOf(configManager.getPrisonerParticle().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle type: " + configManager.getPrisonerParticle());
                }

                if (particle != null) {
                    Location location = player.getLocation();
                    int minParticleCount = 5;
                    int maxParticleCount = 12;

                    int randomParticleCount = ThreadLocalRandom.current().nextInt(minParticleCount, maxParticleCount + 1);
                    player.spawnParticle(particle,
                            player.getLocation(),
                            randomParticleCount,
                            1.0,1.0,1.0,
                            0.0);
                }
            }


            localizationManager.sendMessage(player, "jail.unjailed");

            if (configManager.isBroadcastUnjail()) {
                Component comp = localizationManager.getMessage("jail.unjail-broadcast", player.getName());
                Bukkit.broadcast(comp);
            }
        }

        savePlayers();


        if (plugin.getLogManager() != null) {
            plugin.getLogManager().logUnjail(jailData.getPlayerName(), "System", "Time expired");
        }

        return true;
    }

    public boolean unjailPlayer(String playerName) {
        UUID uuid = jailedPlayers.values().stream()
                .filter(data -> data.getPlayerName().equalsIgnoreCase(playerName))
                .map(PlayerJailData::getPlayerUuid)
                .findFirst()
                .orElse(null);

        return uuid != null && unjailPlayer(uuid);
    }


    public boolean isPlayerJailed(Player player) {
        return jailedPlayerUUIDs.contains(player.getUniqueId());
    }

    public PlayerJailData getJailData(Player player) {
        return jailedPlayers.get(player.getUniqueId());
    }

    public PlayerJailData getJailData(UUID uuid) {
        return jailedPlayers.get(uuid);
    }

    public JailData getJail(String name) {
        return jails.get(name.toLowerCase());
    }

    public Collection<JailData> getAllJails() {
        return new ArrayList<>(jails.values());
    }

    public Collection<PlayerJailData> getAllJailedPlayers() {
        return new ArrayList<>(jailedPlayers.values());
    }

    private void startJailTimer() {
        timerRunning = true;
        jailTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (timerRunning) {
                    checkJailTimes();
                }
            }
        };

        long interval = configManager.getCheckInterval();
        jailTimer.runTaskTimer(plugin, interval, interval);
    }

    private void checkJailTimes() {
        if (jailedPlayers.isEmpty()) {
            return;
        }

        List<UUID> toRelease = new ArrayList<>();
        long currentTime = System.currentTimeMillis() / 1000;

        for (PlayerJailData jailData : jailedPlayers.values()) {
            if (jailData.getStartTime() + jailData.getJailTime() <= currentTime) {
                toRelease.add(jailData.getPlayerUuid());
            }
        }

        if (!toRelease.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (UUID uuid : toRelease) {
                    unjailPlayer(uuid);
                }
            });
        }
    }

    private boolean validateLocation(Location location) {
        if (location == null) return false;
        if (location.getWorld() == null) return false;

        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            location.getWorld().loadChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        }

        return true;
    }

    public void saveJails() {
        jailsConfig.set("jails", null);

        for (JailData jail : jails.values()) {
            String path = "jails." + jail.getName();
            jailsConfig.set(path + ".world", jail.getWorldName());
            jailsConfig.set(path + ".x", jail.getX());
            jailsConfig.set(path + ".y", jail.getY());
            jailsConfig.set(path + ".z", jail.getZ());
            jailsConfig.set(path + ".yaw", jail.getYaw());
            jailsConfig.set(path + ".pitch", jail.getPitch());
        }

        try {
            jailsConfig.save(jailsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving jails.yml", e);
        }
    }

    public void savePlayers() {
        playersConfig.set("players", null);

        for (PlayerJailData jailData : jailedPlayers.values()) {
            String path = "players." + jailData.getPlayerUuid().toString();
            playersConfig.set(path + ".name", jailData.getPlayerName());
            playersConfig.set(path + ".jail", jailData.getJailName());
            playersConfig.set(path + ".jail-time", jailData.getJailTime());
            playersConfig.set(path + ".start-time", jailData.getStartTime());
            playersConfig.set(path + ".reason", jailData.getReason());
            playersConfig.set(path + ".jailed-by", jailData.getJailedBy());

            Location prevLocation = jailData.getPreviousLocation();
            if (prevLocation != null) {
                String prevPath = path + ".previous-location";
                playersConfig.set(prevPath + ".world", prevLocation.getWorld().getName());
                playersConfig.set(prevPath + ".x", prevLocation.getX());
                playersConfig.set(prevPath + ".y", prevLocation.getY());
                playersConfig.set(prevPath + ".z", prevLocation.getZ());
                playersConfig.set(prevPath + ".yaw", prevLocation.getYaw());
                playersConfig.set(prevPath + ".pitch", prevLocation.getPitch());
            }
        }

        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving players.yml", e);
        }
    }

    public void saveAllData() {
        saveJails();
        savePlayers();
    }

    public void reloadData() {
        if (jailTimer != null) {
            jailTimer.cancel();
            timerRunning = false;
        }

        loadData();
        startJailTimer();
        plugin.getLogger().info("JailPlus data reloaded!");
    }

    public void shutdown() {
        timerRunning = false;
        if (jailTimer != null) {
            jailTimer.cancel();
        }
        saveAllData();
        jails.clear();
        jailedPlayers.clear();
        jailedPlayerUUIDs.clear();
    }
}
