package org.gorpipe.s3.driver;

import org.gorpipe.gor.util.StringUtil;

//  the HTTP Range header, see https://www.rfc-editor.org/rfc/rfc9110.html#name-range.
public record BytesRange(String range) {

    static String startStoptoRange(long start, long end) {
        return "bytes=" + start + "-" + end;
    }

    static String startLengthtoRange(long start, long length) {
        return "bytes=" + start + "-" + (start + length - 1);
    }

    private static final String PREFIX = "bytes=";
    private static final String DELIMITER = "-";

    public BytesRange {
        if (StringUtil.isEmpty(range)) {
            range = PREFIX + "0" + DELIMITER;
        } else if (!range.startsWith(PREFIX) && !range.contains(DELIMITER)) {
            throw new IllegalArgumentException("Invalid format, must be 'bytes=<start>-[<stop>]' Got: " + range);
        }
    }

    public BytesRange(long start, long end) {
        this(PREFIX + start + DELIMITER + end);

        if (!(0 <= start && start <= end)) {
            throw new IllegalArgumentException("Invalid range, start must be less than or equal to end. Got: " + start + "-" + end);
        }
    }

    public BytesRange(long start) {
        this(PREFIX + start + DELIMITER);
        if (!(0 <= start)) {
            throw new IllegalArgumentException("Invalid range. Got: " + start + "-");
        }
    }

    public long start() {
        return Long.parseLong(range.substring(PREFIX.length()).split(DELIMITER)[0]);
    }

    public long end() {
        var parts = range.substring(PREFIX.length()).split(DELIMITER);
        return parts.length == 2 && !StringUtil.isEmpty(parts[1]) ? Long.parseLong(parts[1]) : Long.MAX_VALUE;
    }
}
