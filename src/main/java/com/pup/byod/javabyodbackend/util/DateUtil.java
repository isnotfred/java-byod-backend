package com.pup.byod.javabyodbackend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateUtil() {}

    /**
     * Format a LocalDateTime for human-readable display.
     * Example output: 2025-08-01 14:35:00
     */
    public static String toDisplayString(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DISPLAY_FORMATTER);
    }

    /**
     * Format a LocalDateTime as an ISO-8601 string for JSON responses.
     * Example output: 2025-08-01T14:35:00
     */
    public static String toIsoString(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * Return the current server timestamp.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
