package fr.majestycraft.ritualpulse.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public final class TimeUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {}

    public static String formatDuration(long millis) {
        if (millis <= 0) return "0s";

        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (days == 0 && seconds > 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    public static String formatTimestamp(long epochMillis, ZoneId zoneId) {
        if (epochMillis <= 0) return "never";
        return DATE_FMT.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis));
    }
}
