package gorsat.liftover;

import java.util.List;

/**
 * A chain argument after resolution: its location, the column layout, a unique id of the file contents (empty for
 * a nested query) and the files it reads. For a nested chain, path is the argument and nestedQuery the query.
 */
public record ResolvedChain(String path, String root, ChainColumns cols, String uniqueId, String nestedQuery,
                            List<String> usedFiles) {
}
