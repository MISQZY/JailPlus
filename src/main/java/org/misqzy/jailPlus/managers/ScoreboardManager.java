package org.misqzy.jailPlus.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.utils.TimeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;


public class ScoreboardManager {

    private final JailPlus plugin;
    private final LocalizationManager localizationManager;
    private final ConfigManager configManager;

    private final Map<UUID, Scoreboard> playerScoreboards;
    private final Map<UUID, BukkitTask> updateTasks;

    private final org.bukkit.scoreboard.ScoreboardManager bukkitScoreboardManager;

    public ScoreboardManager(JailPlus plugin) {
        this.plugin = plugin;
        this.localizationManager = plugin.getLocalizationManager();
        this.configManager = plugin.getConfigManager();
        this.playerScoreboards = new HashMap<>();
        this.updateTasks = new HashMap<>();

        this.bukkitScoreboardManager = Bukkit.getScoreboardManager();

        if (bukkitScoreboardManager == null) {
            plugin.getLogger().severe("ScoreboardManager unavailable! Scoreboard functions disabled.");
        }

        plugin.getLogger().fine("ScoreboardManager initialized");
    }


    public void showJailScoreboard(Player player, PlayerJailData jailData) {
        if (!configManager.isScoreboardEnabled() || bukkitScoreboardManager == null) {
            return;
        }

        try {
            hideJailScoreboard(player);

            Scoreboard scoreboard = bukkitScoreboardManager.getNewScoreboard();

            String title = getFormattedTitle();
            Objective objective = scoreboard.registerNewObjective("jail_info", "dummy",
                    LegacyComponentSerializer.legacySection().serialize(
                            localizationManager.parseMessage(title)
                    ));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            player.setScoreboard(scoreboard);
            playerScoreboards.put(player.getUniqueId(), scoreboard);

            updateScoreboardContent(player, jailData, objective);

            startUpdateTimer(player);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().fine("Jail scoreboard displayed for player: " + player.getName());
            }

        } catch (Exception e) {
            plugin.getLogger().warning( "Error when create scoreboard for player " + player.getName() + ": " + e);
        }
    }


    public void hideJailScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = updateTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        playerScoreboards.remove(uuid);

        if (bukkitScoreboardManager != null) {
            player.setScoreboard(bukkitScoreboardManager.getMainScoreboard());
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().fine("Jail scoreboard hide for player: " + player.getName());
        }
    }


    private void updateScoreboardContent(Player player, PlayerJailData jailData, Objective objective) {
        if (jailData == null) {
            return;
        }

        try {
            for (String entry : objective.getScoreboard().getEntries()) {
                objective.getScoreboard().resetScores(entry);
            }

            String jailName = jailData.getJailName();
            String reason = jailData.getReason();
            String jailedBy = jailData.getJailedBy();
            long remainingTime = jailData.getRemainingTime();
            String timeFormatted = TimeUtils.formatTime(remainingTime);

            String timeKey = remainingTime <= 300 ? "scoreboard.time-line-urgent" : "scoreboard.time-line";
            if (remainingTime == Long.MAX_VALUE) {
                timeKey = "scoreboard.time-line-permanent";
                timeFormatted = "";
            }

            int line = 15;

            setScoreboardLine(objective, " ", line--);

            String timeLine = formatScoreboardLine(timeKey, timeFormatted);
            setScoreboardLine(objective, timeLine, line--);

            setScoreboardLine(objective, "  ", line--);

            String shortReason = reason.length() > 20 ? reason.substring(0, 17) + "..." : reason;
            String reasonLine = formatScoreboardLine("scoreboard.reason-line", shortReason);
            setScoreboardLine(objective, reasonLine, line--);

            setScoreboardLine(objective, "   ", line--);


            String jailLine = formatScoreboardLine("scoreboard.jail-line", jailName);
            setScoreboardLine(objective, jailLine, line--);

            String separator = formatScoreboardLine("scoreboard.separator", "");
            setScoreboardLine(objective, separator, line--);

            if (configManager.isScoreboardShowJailedBy()) {
                String jailedByLine = formatScoreboardLine("scoreboard.jailed-by-line", jailedBy);
                setScoreboardLine(objective, jailedByLine, line--);

                setScoreboardLine(objective, "    ", line--);
            }

            if (configManager.isScoreboardShowExtraInfo()) {
                long totalTime = jailData.getJailTime();
                if (totalTime > 0 && totalTime != Long.MAX_VALUE) {
                    int percentage = (int) ((totalTime - remainingTime) * 100 / totalTime);
                    String progressLine = formatScoreboardLine("scoreboard.progress-line", String.valueOf(percentage));
                    setScoreboardLine(objective, progressLine, line--);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка обновления scoreboard для " + player.getName(), e);
        }
    }


    private void setScoreboardLine(Objective objective, String text, int score) {
        if (text.length() > 40) {
            text = text.substring(0, 37) + "...";
        }

        Score scoreEntry = objective.getScore(text);
        scoreEntry.setScore(score);
    }


    private String formatScoreboardLine(String key, String value) {
        String rawMessage = localizationManager.getRawMessage(key, value);

        Component component = localizationManager.parseMessage(rawMessage);
        return LegacyComponentSerializer.legacySection().serialize(component);
    }


    private String getFormattedTitle() {
        return localizationManager.getRawMessage("scoreboard.title");
    }

    private void startUpdateTimer(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask existingTask = updateTasks.get(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    updateTasks.remove(uuid);
                    playerScoreboards.remove(uuid);
                    return;
                }

                PlayerJailData jailData = plugin.getJailManager().getJailData(player);
                if (jailData == null) {
                    hideJailScoreboard(player);
                    cancel();
                    return;
                }

                Scoreboard scoreboard = playerScoreboards.get(uuid);
                if (scoreboard != null) {
                    Objective objective = scoreboard.getObjective("jail_info");
                    if (objective != null) {
                        updateScoreboardContent(player, jailData, objective);
                    }
                }
            }
        }.runTaskTimer(plugin, configManager.getScoreboardUpdateInterval(), configManager.getScoreboardUpdateInterval());

        updateTasks.put(uuid, updateTask);
    }


    public void updateAllScoreboards() {
        for (UUID uuid : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                PlayerJailData jailData = plugin.getJailManager().getJailData(player);
                if (jailData != null) {
                    Scoreboard scoreboard = playerScoreboards.get(uuid);
                    if (scoreboard != null) {
                        Objective objective = scoreboard.getObjective("jail_info");
                        if (objective != null) {
                            updateScoreboardContent(player, jailData, objective);
                        }
                    }
                }
            }
        }
    }


    public void shutdown() {
        for (BukkitTask task : updateTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        updateTasks.clear();

        if (bukkitScoreboardManager != null) {
            for (UUID uuid : playerScoreboards.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.setScoreboard(bukkitScoreboardManager.getMainScoreboard());
                }
            }
        }

        playerScoreboards.clear();

        plugin.getLogger().info("ScoreboardManager disabled");
    }


    public boolean hasJailScoreboard(Player player) {
        return playerScoreboards.containsKey(player.getUniqueId());
    }


    public int getActiveScoreboardCount() {
        return playerScoreboards.size();
    }
}