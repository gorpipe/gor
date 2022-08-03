package org.gorpipe.gor.table;

import gorsat.process.CLIGorExecutionEngine;
import gorsat.process.PipeOptions;
import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class GorPipeUtils {

    private static final Logger log = LoggerFactory.getLogger(GorPipeUtils.class);

    /**
     * Execute gor command for the side effects (all output will be ignored).
     *
     * Useful for running gor pipe commands with write.
     *
     * TODO: This would be better handled by a special executionEngine but until then we will just work with the CLIGorExecutionEngine.
     *
     * @param gorPipeCommand
     */
    public static void executeGorPipeForSideEffects(String gorPipeCommand, int workers, String gorroot, String securityContext) {
        List<String> argsList = new ArrayList<>();
        argsList.add(gorPipeCommand);
        argsList.add("-workers");
        argsList.add(String.valueOf(workers));
        if (gorroot != null) {
            argsList.add("-gorroot");
            argsList.add(gorroot);
        }
        String[] args = argsList.toArray(new String[argsList.size()]);

        log.trace("Calling gor command with command args: {} \"{}\" {} {} {} {}", args);

        PrintStream oldOut = System.out;

        PipeOptions options = new PipeOptions();
        options.parseOptions(args);
        CLIGorExecutionEngine engine = new CLIGorExecutionEngine(options, null, securityContext);

        try (PrintStream newPrintStream = new PrintStream(new NullOutputStream())){
            System.setOut(newPrintStream);
            engine.execute();
        } catch (Exception e) {
            log.error("Calling gor cmmand failed.  Command args: {} \"{}\" {} {} {} {} failed", args);
            throw e;
        } finally {

            System.setOut(oldOut);
        }
    }



}
