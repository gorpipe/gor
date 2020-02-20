/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorParsingException;
import gorsat.Commands.CommandParseUtilities;
import gorsat.MacroUtilities;
import gorsat.Script.ExecutionBlock;
import gorsat.Script.ExecutionGraph;

import java.util.*;

public class GorScriptAnalyzer {
    private Map<String, GorScriptTask> tasksByName = new HashMap<>();
    private List<List<GorScriptTask>> tasksByLevel = new ArrayList<>();
    private List<GorScriptTask> extraTasks = new ArrayList<>();

    private GorException exception;

    public void parse(String input) {
        tasksByName = new HashMap<>();
        tasksByLevel = new ArrayList<>();

        try {
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.validate(input);

            String[] rawCommands = CommandParseUtilities.quoteSafeSplitAndTrim(input, ';');
            for( int i = 0; i < rawCommands.length; i++) {
                rawCommands[i] = CommandParseUtilities.cleanupQuery(rawCommands[i]);
            }
            Map<String, String> aliases = MacroUtilities.extractAliases(rawCommands);
            String[] commands = MacroUtilities.applyAliases(rawCommands, aliases);
            ExecutionGraph executionGraph = new ExecutionGraph(commands);
            convertExecutionGraph(executionGraph);
        } catch (GorParsingException e) {
            exception = e;
        }
    }

    public GorException getException() {
        return exception;
    }

    public Map<String, GorScriptTask> getTasksByName() {
        return tasksByName;
    }

    public List<List<GorScriptTask>> getTasksByLevel() {
        return tasksByLevel;
    }

    public Collection<GorScriptTask> getTasks() {
        return tasksByName.values();
    }

    public List<GorScriptTask> getExtraTasks() {
        return extraTasks;
    }

    private void convertExecutionGraph(ExecutionGraph executionGraph) {
        scala.collection.immutable.List<ExecutionBlock>[] levels = executionGraph.levels();
        for (int level = 0; level < levels.length; level++) {
            scala.collection.immutable.List<ExecutionBlock> blocksInLevel = levels[level];
            List<GorScriptTask> tasksInLevel = new ArrayList<>();
            final int levelForLambda = level + 1;
            blocksInLevel.foreach(block -> {
                GorScriptTask task = getGorScriptTask(block);
                task.level = levelForLambda;
                tasksByName.put(task.name, task);
                tasksInLevel.add(task);
                return null;
            });
            tasksByLevel.add(tasksInLevel);
        }
        extraTasks = new ArrayList<>();
        executionGraph.remainingBlocks().values().foreach(block -> {
            extraTasks.add(getGorScriptTask(block));
            return null;
        });
    }

    private GorScriptTask getGorScriptTask(ExecutionBlock block) {
        GorScriptTask task = new GorScriptTask();
        task.name = "[" + block.groupName() + "]";
        task.query = block.query();
        task.dependsOn = block.dependencies();
        return task;
    }

}
