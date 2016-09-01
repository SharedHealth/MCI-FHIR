package org.sharedhealth.mci.web.util;

public class StringUtils {
    public static String ensureSuffix(String value, String pattern) {
        String trimmedValue = value.trim();
        if (trimmedValue.endsWith(pattern)) {
            return trimmedValue;
        } else {
            return trimmedValue + pattern;
        }
    }

    public static String ensurePrefix(String value, String pattern) {
        String trimmedValue = value.trim();
        if (trimmedValue.startsWith(pattern)) {
            return trimmedValue;
        } else {
            return pattern + trimmedValue;
        }
    }

    public static String removeSuffix(String value, String pattern) {
        String trimmedValue = value.trim();
        if (trimmedValue.endsWith(pattern)) {
            return trimmedValue.substring(0, trimmedValue.lastIndexOf(pattern));
        } else {
            return trimmedValue;
        }
    }

    public static String removePrefix(String value, String prefix) {
        String trimmedValue = value.trim();
        if (trimmedValue.startsWith(prefix)) {
            return trimmedValue.substring(prefix.length());
        } else {
            return trimmedValue;
        }
    }

}
