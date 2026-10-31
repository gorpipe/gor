package gorsat.liftover;

import org.gorpipe.exceptions.GorParsingException;

import java.util.Locale;

/**
 * What to do when chain blocks overlap on the source (target assembly) side.
 * ERROR rejects the chain at load, BEST picks the block with the highest score (an exact tie is ambiguous).
 */
public enum OverlapPolicy {
    ERROR, BEST;

    public static OverlapPolicy parse(String s) {
        if (s != null) {
            switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "error":
                    return ERROR;
                case "best":
                    return BEST;
                default:
                    break;
            }
        }
        throw new GorParsingException("Invalid overlap policy '" + s + "', must be error or best", "overlap", s);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
