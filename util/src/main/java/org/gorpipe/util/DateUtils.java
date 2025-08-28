package org.gorpipe.util;

import java.time.Instant;
import java.time.YearMonth;

public class DateUtils {

    /**
     * Parse a date string in ISO 8601 format or epoch milliseconds.
     *
     * Supports partial ISO 8601 dates by appending missing time or timezone information.
     *
     * @param dateStr the date string to parse
     * @param roundUp
     * @return the parsed Instant
     * @throws IllegalArgumentException if the date string is not in a valid format
     */
    public static Instant parseDateISOEpoch(String dateStr, boolean roundUp) {
        try {
            try {
                long epochCandidate = Long.parseLong(dateStr);
                if (epochCandidate < 3000) {
                    // Assume this is a single year.
                    return parseDateISO(dateStr, roundUp);
                } else {
                    return Instant.ofEpochMilli(epochCandidate);
                }
            } catch (Exception ignored) {
                return parseDateISO(dateStr, roundUp);
            }
        } catch(Exception e) {
            throw new IllegalArgumentException(String.format("Value %s supplied is not a valid iso date/epoch", dateStr), e);
        }
    }

    /**
     * Parse a date string in ISO 8601 format.
     *
     * Supports partial ISO 8601 dates by appending missing time or timezone information.
     *
     * @param dateStr the date string to parse
     * @param roundUp
     * @return the parsed Instant
     * @throws IllegalArgumentException if the date string is not in a valid format
     */
    public static Instant parseDateISO(String dateStr, boolean roundUp) {
        try {
            int len = dateStr.length();
            if (len == 4) {
                // Assume this is a single year.
                dateStr += roundUp ? "-12-31T23:59:59Z" : "-01-01T00:00:00Z";
            } else  if (len == 7) {
                // Assume this is a year and month.
                int year = Integer.parseInt(dateStr.substring(0, 4));
                int month = Integer.parseInt(dateStr.substring(5, 7));
                dateStr += roundUp ? String.format("-%sT23:59:59Z", YearMonth.of(year, month).lengthOfMonth()) : "-01T00:00:00Z";
            } else if (len == 10) {
                // Assume this is a full date without time.
                dateStr += roundUp ? "T23:59:59Z" : "T00:00:00Z";
            } else  if (len == 13 && dateStr.charAt(10) == 'T') {
                // Assume this is a date and time without seconds or timezone.
                dateStr += roundUp ? ":59:59Z" : ":00:00Z";
            } else  if (len == 16 && dateStr.charAt(10) == 'T') {
                // Assume this is a date and time without seconds or timezone.
                dateStr += roundUp ? ":59Z" : ":00Z";
            } else if (!dateStr.endsWith("Z")) {
                // Assume this is a date and time without timezone.
                dateStr += "Z";
            }

            return Instant.parse(dateStr);
        } catch(Exception e) {
            throw new IllegalArgumentException(String.format("Value %s supplied is not a valid iso date/epoch", dateStr), e);
        }
    }

}
