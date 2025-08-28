package org.misqzy.jailPlus.utils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdw]?)");

    public static long parseTime(String timeString) throws IllegalArgumentException {
        if (timeString == null || timeString.trim().isEmpty()) {
            throw new IllegalArgumentException("Null time string");
        }
        timeString = timeString.toLowerCase().trim();
        if (timeString.equals("permanent") || timeString.equals("perm")) {
            return Long.MAX_VALUE;
        }
        Matcher matcher = TIME_PATTERN.matcher(timeString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Wrong time format: " + timeString);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null || unit.isEmpty()) {
            unit = "s";
        }
        switch (unit) {
            case "s":
                return amount;
            case "m":
                return amount * 60;
            case "h":
                return amount * 3600;
            case "d":
                return amount * 86400;
            case "w":
                return amount * 604800;
            default:
                throw new IllegalArgumentException("Unknown time uom: " + unit);
        }
    }
    public static String formatTime(long seconds) {
        if (seconds == Long.MAX_VALUE) {
            return "permanent";
        }
        if (seconds <= 0) {
            return "0 s";
        }
        StringBuilder result = new StringBuilder();
        long weeks = seconds / 604800;
        if (weeks > 0) {
            result.append(weeks).append("w ");
            seconds %= 604800;
        }
        long days = seconds / 86400;
        if (days > 0) {
            result.append(days).append("d ");
            seconds %= 86400;
        }
        long hours = seconds / 3600;
        if (hours > 0) {
            result.append(hours).append("h ");
            seconds %= 3600;
        }
        long minutes = seconds / 60;
        if (minutes > 0) {
            result.append(minutes).append("m ");
            seconds %= 60;
        }
        if (seconds > 0) {
            result.append(seconds).append("s");
        }
        return result.toString().trim();
    }
    public static long roundToSeconds(long milliseconds) {
        return milliseconds / 1000;
    }
    public static boolean isValidTime(String timeString) {
        try {
            parseTime(timeString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}