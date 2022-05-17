package gorsat.Script;

import gorsat.Commands.CommandParseUtilities;
import gorsat.Utilities.AnalysisUtilities;
import gorsat.Utilities.MacroUtilities;
import gorsat.Utilities.StringUtilities;
import gorsat.process.GorPipeMacros;
import gorsat.process.GorPrePipe;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.session.GorContext;
import org.gorpipe.gor.session.GorSession;
import scala.Tuple2;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BaseScriptExecutionEngine {
    Map<String,ExecutionBlock> executionBlocks = new ConcurrentHashMap<>();
    Map<String,String> aliases = new ConcurrentHashMap<>();
    Map<String,String> fileSignatureMap = new ConcurrentHashMap<>();
    Map<String,String> singleFileSignatureMap = new ConcurrentHashMap<>();
    VirtualFileManager virtualFileManager = new VirtualFileManager();

    public List<String> getUsedFiles(String commandToExecute, GorSession session) {
        List<String> usedFiles = new ArrayList<>();
        if (CommandParseUtilities.isDictionaryQuery(commandToExecute)) {
            // The header does not matter here
            var w = commandToExecute.split(" ");
            var i = 1;
            while (i < w.length - 1) {
                usedFiles.add(w[i]);
                i += 2;
            }
        } else {
            var slist = GorPrePipe.getUsedFiles(commandToExecute, session);
            slist.foreach(usedFiles::add);
        }
        Collections.sort(usedFiles);
        return usedFiles;
    }

    /**
     * Resolve parent path of fork template path
     * @param res
     * @return
     */
    private Optional<Tuple2<String,Boolean>> resolveForkPathParent(String res) {
        var i = res.indexOf("#{");
        if(i != -1) {
            var k = res.lastIndexOf('/', i);
            var ret = k == -1 ? "." : res.substring(0,k);
            return Optional.of(Tuple2.apply(ret, true));
        } else {
            return Optional.of(Tuple2.apply(res, false));
        }
    }

    private Optional<Tuple2<String,Boolean>> resolveCache(String lastCommand) {
        var fields = lastCommand.split(" ");
        var lastField = fields[fields.length-1].trim();
        return !lastField.startsWith("-") ? resolveForkPathParent(lastField) : Optional.empty();
    }

    public Optional<Tuple2<String,Boolean>> getExplicitWrite(String query) {
        var lastCommand = MacroUtilities.getLastCommand(query);
        if (lastCommand.toLowerCase().startsWith("write ")) {
            return resolveCache(lastCommand);
        } else {
            return Optional.empty();
        }
    }

    private String getGorDictSignature(GorSession gorPipeSession, String fileName) {
        var fileReader = gorPipeSession.getProjectContext().getFileReader();
        var hasTags = fileName.contains("#gortags#");
        var dictFile = fileName.substring("#gordict#".length(), hasTags ? fileName.indexOf("#gortags#") : fileName.length());
        var dictTags = hasTags ? fileName.substring(fileName.indexOf("#gortags#") + "#gortags#".length()).split(",") : null;
        try {
            if (dictTags != null && dictTags.length > 9) {
                return fileReader.getFileSignature(dictFile);
            } else {
                return fileReader.getDictionarySignature(dictFile, dictTags);
            }
        } catch(IOException e) {
            throw new GorResourceException("Could not get signature for file", dictFile, e);
        }
    }

    public String fileFingerPrint(String fileName, GorSession gorPipeSession) {
        return singleFileSignatureMap.computeIfAbsent(fileName, (k) -> {
            if (fileName.startsWith("#gordict#")) {
                return getGorDictSignature(gorPipeSession, fileName);
            } else {
                var fileReader = gorPipeSession.getProjectContext().getFileReader();
                var cacheDirectory = AnalysisUtilities.theCacheDirectory(gorPipeSession);
                // TODO: Get a gor config instance somehow into gorpipeSession or gorContext to skip using system.getProperty here
                var fnameSplit = fileName.split("/");
                var x_f_name = fnameSplit[fnameSplit.length-1].split("\\.")[0];
                if (Boolean.parseBoolean(System.getProperty("gor.caching.md5.enabled", "false"))
                        && x_f_name.split("\\.")[0].endsWith("_md5")) {
                    // cache files with md5 have the md5 sum encoded in the filename
                    return x_f_name.split("\\.")[0];
                } else if (fileName.startsWith(cacheDirectory)) {
                    return "0";
                } else {
                    try {
                        return fileReader.getFileSignature(fileName);
                    } catch(GorException ge) {
                        throw ge;
                    } catch(Exception e) {
                        throw new GorResourceException("Could not get file signature for:  " + fileName, fileName, e);
                    }
                }
            }
        });
    }

    public String getFileSignatureAndUpdateSignatureMap(GorSession session, String commandToExecute, List<String> usedFiles) {
        var fileSignature = "";
        if (CommandParseUtilities.isDictionaryQuery(commandToExecute)) {
            var usedFilesConcatStr = String.join(" ", usedFiles);
            fileSignature = StringUtilities.createMD5(usedFilesConcatStr);
        } else {
            var signatureKey = AnalysisUtilities.getSignature(commandToExecute);
            var fileListKey = String.join(" ", usedFiles) + signatureKey;
            fileSignature = fileSignatureMap.computeIfAbsent(fileListKey, (k) -> StringUtilities.createMD5(usedFiles.stream().map(x -> fileFingerPrint(x, session)).collect(Collectors.joining(" ")) + signatureKey));
        }

        return fileSignature;
    }

    private Map<String, ExecutionBlock> expandMacros(GorContext context, Map<String,ExecutionBlock> creates, boolean valid) {
        var activeCreates = creates;
        var macroCreated = false;
        do {
            var newCreates = new HashMap<String,ExecutionBlock>();
            macroCreated = false;
            // Go through each command
            for (Map.Entry<String,ExecutionBlock> create : activeCreates.entrySet()) {
                // Parse each command and get the macro name
                var commands = CommandParseUtilities.quoteSafeSplit(create.getValue().query(), '|');

                if (commands.length > 0) {
                    // Get the macro object and process the executionblock
                    var commandOptions = CommandParseUtilities.quoteSafeSplit(commands[0], ' ');
                    var macroEntry = GorPipeMacros.getInfo(commandOptions[0]);

                    if (macroEntry.isEmpty()) {
                        newCreates.put(create.getKey(), create.getValue());
                    } else {
                        macroCreated = true;
                        var macroResult = macroEntry.get().init(create.getKey(),
                                create.getValue(),
                                context,
                                false,
                                Arrays.copyOfRange(commandOptions, 1, commandOptions.length),
                                !valid);

                        newCreates.putAll(macroResult.createCommands());
                        if (macroResult.aliases() != null) {
                            aliases.putAll(macroResult.aliases());
                        }
                    }
                }
            }
            activeCreates = newCreates;
        } while (macroCreated);

        // return the expanded macros
        return activeCreates;
    }

    private Tuple2<Map<String,ExecutionBlock>,Map<String,ExecutionBlock>> splitBasedOnDependencies(Map<String,ExecutionBlock> executionBlocks) {
        var activeExecutionBlocks = new HashMap<String, ExecutionBlock>();
        var dependantExecutionBlocks = new HashMap<String, ExecutionBlock>();

        for (Map.Entry<String,ExecutionBlock> executionBlock : executionBlocks.entrySet()) {
            var virt = virtualFileManager.get(executionBlock.getKey());
            if (virt != null) {
                if (virt.fileName == null) {
                    var dependencies = executionBlock.getValue().dependencies();
                    if (dependencies.length == 0 || virtualFileManager.areDependenciesReady(dependencies)) {
                        activeExecutionBlocks.put(executionBlock.getKey(), executionBlock.getValue());
                    } else {
                        dependantExecutionBlocks.put(executionBlock.getKey(), executionBlock.getValue());
                    }
                }
            } else {
                var dependencies = executionBlock.getValue().dependencies();
                if (dependencies.length == 0 || virtualFileManager.areDependenciesReady(dependencies)) {
                    activeExecutionBlocks.put(executionBlock.getKey(), executionBlock.getValue());
                } else {
                    dependantExecutionBlocks.put(executionBlock.getKey(), executionBlock.getValue());
                }
            }
        }

        return Tuple2.apply(activeExecutionBlocks, dependantExecutionBlocks);
    }

    public Tuple2<String,List<String>> processBlocks(GorContext context, boolean suggestName, ExecutionBatch executionBatch, boolean validate, String currentGorCmd) {
        var session = context.getSession();
        var eventLogger = session.getEventLogger();
        var gorCommand = new String[] {currentGorCmd};
        var allUsedFiles = new ArrayList<String>();
        var cAllUsedFiles = Collections.synchronizedList(allUsedFiles);
        Arrays.stream(executionBatch.getBlocks()).parallel().forEach(firstLevelBlock -> {
            firstLevelBlock.query_$eq(virtualFileManager.replaceVirtualFiles(firstLevelBlock.query()));

            var query = firstLevelBlock.query();
            var queryLower = query.toLowerCase();
            var isParallelQuery = queryLower.startsWith("pgor ") || queryLower.startsWith("partgor ") || queryLower.startsWith("parallel ");
            if (validate&&firstLevelBlock.signature()==null&&firstLevelBlock.query()!=null&&isParallelQuery) {
                var usedFiles = getUsedFiles(query, session);
                var fileSignature = getFileSignatureAndUpdateSignatureMap(session, query, usedFiles);
                var querySignature = StringUtilities.createMD5(query + fileSignature);
                firstLevelBlock.signature_$eq(querySignature);

                var cachePath = getExplicitWrite(query);
                if (cachePath.isPresent()) {
                    Tuple2<String,Boolean> cp = cachePath.get();
                    firstLevelBlock.cachePath_$eq(cp._1());
                    firstLevelBlock.hasForkWrite_$eq(cp._2());
                }
            }

            // Expand the executionBlock with macros
            var newExecutionBlocks = expandMacros(context, Map.of(firstLevelBlock.groupName(), firstLevelBlock), validate);

            // We need to determine if there is any dependency in the new executions, remove dependent blocks and
            // add them to the executionBlocks map
            var tmpExecutionBlocks = splitBasedOnDependencies(newExecutionBlocks);
            var activeExecutionBlocks = tmpExecutionBlocks._1;
            var dependentExecutionBlocks = tmpExecutionBlocks._2;

            virtualFileManager.addRange(activeExecutionBlocks);

            for (Map.Entry<String,ExecutionBlock> newExecutionBlock : activeExecutionBlocks.entrySet()) {
                // Get the command to finally execute
                var commandToExecute = newExecutionBlock.getValue().query();
                var cacheFile = newExecutionBlock.getValue().cachePath();
                var hasForkWrite = newExecutionBlock.getValue().hasForkWrite();

                String cachePath;
                boolean hasFork;
                if (cacheFile == null) {
                    var ocache = getExplicitWrite(commandToExecute);
                    if(ocache.isPresent()) {
                        var tup = ocache.get();
                        cachePath = tup._1;
                        hasFork = tup._2;
                    } else {
                        cachePath = cacheFile;
                        hasFork = hasForkWrite;
                    }
                } else {
                    cachePath = cacheFile;
                    hasFork = hasForkWrite;
                }

                // Extract used files from the final gor command
                var usedFiles = getUsedFiles(commandToExecute, session);

                // Create the split manager to use from the query (might contain -split option)
                var splitManager = SplitManager.createFromCommand(newExecutionBlock.getKey(), commandToExecute, context);

                // Expand execution blocks based on the active split
                var commandGroup = splitManager.expandCommand(commandToExecute, newExecutionBlock.getKey(), cachePath);

                // Remove this command from the execution blocks if needed
                if (commandGroup.removeFromCreate()) {
                    executionBlocks.remove(firstLevelBlock.groupName());
                }

                // Update gorcommand and add new queries if needed
                for (var cte : commandGroup.commandEntries()) {
                    if (cte.createName().equals("[]")) {
                        // This is the final command, we apply it and remove it from the execution blocks
                        gorCommand[0] = cte.query();
                        executionBlocks.remove(firstLevelBlock.groupName());
                    } else {
                        // We need to create a new dictionary query to the batch to get the results from expanded queries
                        var commandLower = commandToExecute.toLowerCase();
                        var gordictfolder = commandLower.startsWith("gordictfolder") || commandLower.startsWith("gordictfolderpart");
                        String querySignature;
                        if (firstLevelBlock.signature()!=null&&gordictfolder) {
                            querySignature = firstLevelBlock.signature();
                        } else {
                            var fileSignature = validate ? getFileSignatureAndUpdateSignatureMap(session, commandToExecute, usedFiles) : "";
                            querySignature = StringUtilities.createMD5(cte.query() + fileSignature);
                        }
                        var ctequery = cte.query();
                        if (!gordictfolder && !hasFork && cte.cacheFile()!=null && cte.cacheFile().endsWith(".gord")) {
                            var gordResultsPath = cte.cacheFile()+"/"+querySignature+".gorz";
                            executionBatch.createNewCommand(querySignature, ctequery.replace(cte.cacheFile(),gordResultsPath), cte.batchGroupName(), cte.createName(), gordResultsPath);
                        } else {
                            executionBatch.createNewCommand(querySignature, ctequery, cte.batchGroupName(), cte.createName(), cte.cacheFile());
                        }
                        eventLogger.commandCreated(cte.createName(), firstLevelBlock.groupName(), querySignature, cte.query());
                    }
                }

                // Collect files if we are suggesting virtual file name
                if (suggestName) cAllUsedFiles.addAll(usedFiles.stream().filter(x -> !x.startsWith("[")).collect(Collectors.toList()));
            }

            // Add dictionary entries back to the execution blocks lists but process other entries
            executionBlocks.putAll(dependentExecutionBlocks);
            // Replace any virtual file in the current query
        });
        Collections.sort(allUsedFiles);
        return Tuple2.apply(gorCommand[0],allUsedFiles);
    }
}
