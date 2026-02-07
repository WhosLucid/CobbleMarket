package com.whoslucid.cobblemarket.util;

import java.util.concurrent.TimeUnit;

public class TimeUtils {

    /**
     * Format milliseconds into a human-readable duration string
     * e.g., "2d 5h 30m" or "45m 20s"
     */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "Expired";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m");
        }
        if (days == 0 && hours == 0) {
            if (minutes > 0) {
                sb.append(" ");
            }
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Parse a duration string like "72h", "3d", "30m" into milliseconds
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        input = input.toLowerCase().trim();
        long total = 0;

        StringBuilder number = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                if (number.length() > 0) {
                    long value = Long.parseLong(number.toString());
                    switch (c) {
                        case 'd' -> total += TimeUnit.DAYS.toMillis(value);
                        case 'h' -> total += TimeUnit.HOURS.toMillis(value);
                        case 'm' -> total += TimeUnit.MINUTES.toMillis(value);
                        case 's' -> total += TimeUnit.SECONDS.toMillis(value);
                    }
                    number = new StringBuilder();
                }
            }
        }

        return total;
    }

    /**
     * Convert hours to milliseconds
     */
    public static long hoursToMillis(int hours) {
        return TimeUnit.HOURS.toMillis(hours);
    }

    /**
     * Convert minutes to milliseconds
     */
    public static long minutesToMillis(int minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    /**
     * Get current timestamp in milliseconds
     */
    public static long now() {
        return System.currentTimeMillis();
    }
}
