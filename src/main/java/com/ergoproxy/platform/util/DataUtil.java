package com.ergoproxy.platform.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class DataUtil {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter APP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(APP_ZONE);

    private DataUtil() {
    }

    public static boolean isNull(Object value) {
        boolean response = value == null;
        return response;
    }

    public static boolean isNullOrEmpty(String value) {
        boolean response = value == null || value.isEmpty();
        return response;
    }

    public static boolean isNullOrBlank(String value) {
        boolean response = value == null || value.isBlank();
        return response;
    }

    public static boolean isNullOrZero(Number value) {
        boolean response = value == null || value.doubleValue() == 0;
        return response;
    }

    public static boolean isNullOrNegative(Number value) {
        boolean response = value == null || value.doubleValue() < 0;
        return response;
    }

    public static boolean isPositive(Number value) {
        boolean response = value != null && value.doubleValue() > 0;
        return response;
    }

    public static boolean isNegative(Number value) {
        boolean response = value != null && value.doubleValue() < 0;
        return response;
    }

    public static boolean isNullOrEmpty(Collection<?> value) {
        boolean response = value == null || value.isEmpty();
        return response;
    }

    public static boolean isNullOrEmpty(Map<?, ?> value) {
        boolean response = value == null || value.isEmpty();
        return response;
    }

    public static boolean isNullOrEmpty(Object[] value) {
        boolean response = value == null || value.length == 0;
        return response;
    }

    public static boolean isNullOrEmpty(Optional<?> value) {
        boolean response = value == null || value.isEmpty();
        return response;
    }

    public static boolean isPresent(Optional<?> value) {
        boolean response = value != null && value.isPresent();
        return response;
    }

    public static String format(Instant instant) {
        String response = APP_FORMATTER.format(instant);
        return response;
    }

    public static Instant startOfToday() {
        Instant response = LocalDate.now(APP_ZONE).atStartOfDay(APP_ZONE).toInstant();
        return response;
    }

    public static Instant earliest(Instant a, Instant b) {
        Instant response = a.isBefore(b) ? a : b;
        return response;
    }

    public static Instant latest(Instant a, Instant b) {
        Instant response = a.isAfter(b) ? a : b;
        return response;
    }

    public static LocalDate parseDate(String data, String format) {
        LocalDate response = LocalDate.parse(data, DateTimeFormatter.ofPattern(format));
        return response;
    }

    public static LocalTime parseTime(String data, String format) {
        LocalTime response = LocalTime.parse(data, DateTimeFormatter.ofPattern(format));
        return response;
    }

    public static String parseDate(String data, String inputFormat, String outputFormat) {
        String response = LocalDate.parse(data, DateTimeFormatter.ofPattern(inputFormat)).format(DateTimeFormatter.ofPattern(outputFormat));
        return response;
    }

    public static String parseTime(String data, String inputFormat, String outputFormat) {
        String response = LocalTime.parse(data, DateTimeFormatter.ofPattern(inputFormat)).format(DateTimeFormatter.ofPattern(outputFormat));
        return response;
    }

    public static String parseDateTime(String data, String inputFormat, String outputFormat) {
        String response = LocalDateTime.parse(data, DateTimeFormatter.ofPattern(inputFormat)).format(DateTimeFormatter.ofPattern(outputFormat));
        return response;
    }

    public static List<String> splitString(String data, String delimiter) {
        List<String> result = new ArrayList<>();
        if (isNullOrEmpty(data)) {
            return result;
        }

        String[] values = data.split(delimiter);
        for (String value : values) {
            if (!isNullOrEmpty(value)) {
                result.add(value.trim());
            }
        }

        return result;
    }
}
