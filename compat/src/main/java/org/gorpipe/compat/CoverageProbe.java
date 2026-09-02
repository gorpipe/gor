package org.gorpipe.compat;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.SessionInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads which coverage probes the JVM has hit so far.
 *
 * Used by the nightly mutation generator to keep only mutants that reach code the
 * corpus has not reached: without that feedback, mutation broadens the corpus
 * without making it cover more.
 *
 * The agent is reached by reflection rather than by linking against it, because it
 * is present only when the JVM was started with -javaagent. When it is absent this
 * reports unavailable and callers fall back to plain enumeration — a missing agent
 * must never fail a generation run.
 */
public final class CoverageProbe {

    private CoverageProbe() {
    }

    public static boolean available() {
        return executionData() != null;
    }

    /**
     * The probes hit so far, as "className#probeIndex".
     *
     * Probe state is cumulative for the life of the JVM, so a caller detects what
     * one case reached by diffing a snapshot taken before it against one after.
     */
    public static Set<String> hitProbes() {
        byte[] data = executionData();
        if (data == null) {
            return Collections.emptySet();
        }

        Set<String> hit = new HashSet<>();
        ExecutionDataReader reader = new ExecutionDataReader(new ByteArrayInputStream(data));
        reader.setSessionInfoVisitor(new IgnoredSessions());
        reader.setExecutionDataVisitor(new IExecutionDataVisitor() {
            @Override
            public void visitClassExecution(ExecutionData execution) {
                boolean[] probes = execution.getProbes();
                for (int i = 0; i < probes.length; i++) {
                    if (probes[i]) {
                        hit.add(execution.getName() + '#' + i);
                    }
                }
            }
        });
        try {
            while (reader.read()) {
                // Each read consumes one block; the visitors collect the probes.
            }
        } catch (IOException e) {
            // Malformed or truncated dump: report what was collected rather than
            // failing a generation run over a diagnostic.
            return hit;
        }
        return hit;
    }

    /**
     * A dump of the agent's current execution data, or null when no agent is
     * attached. reset is false so reading never discards coverage another reader
     * (the build's own report) is going to want.
     */
    private static byte[] executionData() {
        try {
            Class<?> rt = Class.forName("org.jacoco.agent.rt.RT");
            Object agent = rt.getMethod("getAgent").invoke(null);
            return (byte[]) agent.getClass()
                    .getMethod("getExecutionData", boolean.class)
                    .invoke(agent, false);
        } catch (ReflectiveOperationException | LinkageError | IllegalStateException e) {
            return null;
        }
    }

    /** Session metadata is irrelevant here; only probe state matters. */
    private static final class IgnoredSessions
            implements org.jacoco.core.data.ISessionInfoVisitor {
        @Override
        public void visitSessionInfo(SessionInfo info) {
            // deliberately empty
        }
    }
}
