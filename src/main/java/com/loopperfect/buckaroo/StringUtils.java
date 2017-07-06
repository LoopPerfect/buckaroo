package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    private StringUtils() {

    }

    private static final Pattern PATTERN = Pattern.compile("[^A-Za-z0-9_\\-]");
    private static final int MAX_LENGTH = 1024;

    public static String escapeStringGitHubStyle(final String x) {

        Preconditions.checkNotNull(x);

        final StringBuilder b = new StringBuilder();

        for (final char c : x.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                b.append(c);
            } else {
                if (b.length() == 0 || b.charAt(b.length() - 1) != '-') {
                    b.append('-');
                }
            }
        }

        return b.toString();
    }

    public static String escapeStringAsFilename(final String x) {

        Preconditions.checkNotNull(x);

        final StringBuffer sb = new StringBuffer();

        // Apply the regex
        final Matcher m = PATTERN.matcher(x);

        while (m.find()) {

            // Convert matched character to percent-encoded.
            final String replacement = "%" + Integer.toHexString(m.group().charAt(0)).toUpperCase();

            m.appendReplacement(sb, replacement);
        }

        m.appendTail(sb);

        final String encoded = sb.toString();

        // Truncate the string
        int end = Math.min(encoded.length(), MAX_LENGTH);

        return encoded.substring(0, end);
    }
}
