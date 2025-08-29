package org.misqzy.jailPlus.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PlayerJailData {

    private UUID playerUuid;
    private String playerName;
    private String jailName;
    private long jailTime; // Время заключения в секундах
    private long startTime; // Время начала заключения (timestamp)
    private String reason;
    private String jailedBy;

    private String previousWorldName;
    private double previousX, previousY, previousZ;
    private float previousYaw, previousPitch;

    public PlayerJailData(UUID playerUuid, String playerName, String jailName,
                          long jailTime, String reason, String jailedBy) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.jailName = jailName;
        this.jailTime = jailTime;
        this.startTime = System.currentTimeMillis() / 1000;
        this.reason = reason;
        this.jailedBy = jailedBy;
    }

    public void setPreviousLocation(Location location) {
        if (location != null) {
            this.previousWorldName = location.getWorld().getName();
            this.previousX = location.getX();
            this.previousY = location.getY();
            this.previousZ = location.getZ();
            this.previousYaw = location.getYaw();
            this.previousPitch = location.getPitch();
        }
    }

    public Location getPreviousLocation() {
        if (previousWorldName == null) {
            return null;
        }

        World world = Bukkit.getWorld(previousWorldName);
        if (world == null) {
            return null;
        }

        return new Location(world, previousX, previousY, previousZ, previousYaw, previousPitch);
    }

    public long getRemainingTime() {
        long currentTime = System.currentTimeMillis() / 1000;
        long elapsed = currentTime - startTime;
        return Math.max(0, jailTime - elapsed);
    }

    public boolean isExpired() {
        return getRemainingTime() <= 0;
    }

    public void addTime(long seconds) {
        this.jailTime += seconds;
    }

    public void subtractTime(long seconds) {
        this.jailTime = Math.max(0, this.jailTime - seconds);
    }

    public void setTime(long seconds) {
        this.jailTime = seconds;
        this.startTime = System.currentTimeMillis() / 1000;
    }

    // Getters and setters
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getJailName() { return jailName; }
    public void setJailName(String jailName) { this.jailName = jailName; }
    public long getJailTime() { return jailTime; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getJailedBy() { return jailedBy; }
    public void setJailedBy(String jailedBy) { this.jailedBy = jailedBy; }

    @Override
    public String toString() {
        return "PlayerJailData{" +
                "player='" + playerName + '\'' +
                ", jail='" + jailName + '\'' +
                ", timeLeft=" + getRemainingTime() + "s" +
                ", reason='" + reason + '\'' +
                ", jailedBy='" + jailedBy + '\'' +
                '}';
    }
}