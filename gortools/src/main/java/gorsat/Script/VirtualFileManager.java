package gorsat.Script;

import gorsat.Commands.CommandParseUtilities;
import gorsat.Utilities.MacroUtilities;
import org.gorpipe.exceptions.GorParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VirtualFileManager {
    private static final Logger log = LoggerFactory.getLogger(VirtualFileManager.class);
    private final Map<String,VirtualFileEntry> virtualFileMap = new LinkedHashMap<>();
    // Note: "(?!//)" is a hack and should not really be there, but we need it as we have the case where 1) the VR name
    //       includes an alias 2) that alias maps to a url.
    private final Pattern externalVirtualSearchPattern = Pattern.compile("\\[.+?:(?!//).+?]");

    public VirtualFileEntry add(String name) {
        if (name == null || name.isEmpty()) throw new GorParsingException("Supplied virtual file entry name is empty: $name");
        var groupName = MacroUtilities.getVirtualFileGroupName(name);

        return virtualFileMap.computeIfAbsent(groupName, (x) -> {
            var entry = new VirtualFileEntry("[" + groupName + "]");
            var virtualName = name.startsWith("[") ? name : "[" + name + "]";
            entry.isExternal = isExternalVirtualFile(virtualName);
            return entry;
        });
    }

    public int size() {
        return virtualFileMap.size();
    }

    public VirtualFileEntry get(String name) {
        var groupName = MacroUtilities.getVirtualFileGroupName(name);
        return virtualFileMap.get(groupName);
    }

    public synchronized void addRange(Map<String, ExecutionBlock> executionBlocks) {
        executionBlocks.values().forEach(this::add);
    }

    public void add(ExecutionBlock executionBlock) {
        add(executionBlock.groupName());
        addQuery(executionBlock.query());
    }

    public void addQuery(String query) {
        var virtualFiles = MacroUtilities.virtualFiles(query);
        virtualFiles.foreach(this::add);
    }

    public VirtualFileEntry[] getUnusedVirtualFileEntries() {
        return virtualFileMap.values().stream().filter(x -> x.isOriginal).filter(y -> y.fileName == null || y.fileName.isEmpty()).toArray(VirtualFileEntry[]::new);
    }

    public void updateCreatedFile(String name, String fileName) {
        var x = get(name);
        if (x!=null) {
            if (fileName != null && !fileName.isEmpty()) x.fileName = fileName;
            else throw new GorParsingException("Supplied virtual file name is empty: $fileName, for file entry: $name");
        } else {
            throw new GorParsingException("Unable to locate virtual file entry $name for file: $fileName");
        }
    }

    public String replaceVirtualFiles(String query) {
        var virtualFileList = MacroUtilities.virtualFiles(query);
        var outStr = query;

        for (int i = 0; i < virtualFileList.length(); i++) {
            var virtualFile = virtualFileList.apply(i);
            var name = MacroUtilities.getVirtualFileGroupName(virtualFile);

            var x = virtualFileMap.get(name);
            if (x!=null) {
                if (x.fileName != null) {
                    outStr = CommandParseUtilities.quoteSafeReplace(outStr, virtualFile, x.fileName);
                }
            } else {
                VirtualFileManager.log.warn("There was no reference to create statement '{}' in replaceVirtualFiles", virtualFileList);
            }
        }

        return outStr;
    }

    public Map<String,String> getCreatedFiles() {
        return virtualFileMap.values().stream().filter(y -> y.fileName != null && !y.fileName.isEmpty()).collect(Collectors.toMap(x -> x.name, x -> x.fileName));
    }

    public VirtualFileEntry[] getExternalVirtualFiles() {
        return virtualFileMap.values().stream().filter(x -> x.isExternal).toArray(VirtualFileEntry[]::new);
    }

    public boolean areDependenciesReady(String[] dependencies) {
        return Arrays.stream(dependencies).filter(x -> get(x) != null && get(x).fileName != null).count() == dependencies.length;
    }

    public void setAllAsOriginal() {
        virtualFileMap.values().forEach(x -> x.isOriginal = true);
    }

    private boolean isExternalVirtualFile(String virtualFileName) {
        return externalVirtualSearchPattern.matcher(virtualFileName).find();
    }
}
