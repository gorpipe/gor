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

package org.gorpipe.exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.UncheckedExecutionException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@SuppressWarnings("unchecked")
public final class ExceptionUtilities {

    private ExceptionUtilities() {
    }

    private static boolean showStackTrace = Boolean.parseBoolean(System.getProperty("gor.server.stacktrace.enabled", "true"));
    private static final Logger log = LoggerFactory.getLogger(ExceptionUtilities.class);

    public static void setShowStackTrace(Boolean show) {
        showStackTrace = show;
    }

    public static String gorExceptionToString(Throwable exception) {

        if (exception instanceof GorParsingException) {
            return gorParsingExceptionToString((GorParsingException) exception);
        } else if (exception instanceof GorResourceException) {
            return gorResourceExceptionToString((GorResourceException) exception);
        } else if (exception instanceof GorDataException) {
            return gorDataExceptionToString((GorDataException) exception);
        } else if (exception instanceof GorSystemException) {
            return gorSystemExceptionToString((GorSystemException) exception);
        } else if (exception instanceof GorSecurityException) {
            return gorSecurityExceptionToString((GorSecurityException) exception);
        } else {
            // Unsupported exception, aka not a GorException
            return "An error has occurred.\n" + exception.getMessage();
        }
    }

    private static String gorSecurityExceptionToString(GorSecurityException exception) {
        StringBuilder builder = new StringBuilder();
        builder.append("==== Security Error ====\n");
        builder.append(exception.getMessage());
        printCommandSource(exception, builder);
        printCommonStack(exception, builder);

        return builder.toString();
    }

    private static String gorParsingExceptionToString(GorParsingException exception) {
        String option = exception.getOption();
        String command = exception.getCommandName();
        String payload = exception.getPayload();
        String commandString = exception.getCommandStep();
        int commandIndex = exception.getCommandIndex();

        StringBuilder builder = new StringBuilder();

        builder.append("==== Parsing Error ====\n");

        if (isNullOrEmpty(command)) {
            if (commandIndex > 0) {
                builder.append(String.format("Command at pipe step #%1$s has some issues:%n%2$s", commandIndex, exception.getMessage()));
            } else {
                builder.append(exception.getMessage());
            }
        } else {
            if (isNullOrEmpty(option)) {
                builder.append(String.format("Command %1$S in pipe step #%2$s has some issues:%n%3$s", command, commandIndex, exception.getMessage()));
            } else {
                builder.append(String.format("Command %1$S in pipe step #%2$s has some issues in option %3$s:%n%4$s", command, commandIndex, payload.equals("") ? option : option + " " + payload, exception.getMessage()));
            }

            builder.append("\n\n");
            if (commandIndex > 1) {
                builder.append(String.format(" .. | %1$s | ..", commandString));
            } else {
                builder.append(String.format("  %1$s | ..", commandString));
            }
        }

        printCommandSource(exception, builder);
        printCommonStack(exception, builder);

        return builder.toString();
    }

    private static String gorResourceExceptionToString(GorResourceException exception) {
        String command = exception.getCommandName();
        int commandIndex = exception.getCommandIndex();
        String commandString = exception.getCommandStep();

        StringBuilder builder = new StringBuilder();

        builder.append("==== Resource Error ====\n");
        if (commandIndex < 0) {
            builder.append(exception.getMessage());
            if (!isNullOrEmpty(exception.getUri())) {
                builder.append("\n");
                builder.append("URI: ");
                builder.append(exception.getUri());
            }
        } else {
            builder.append(String.format("Command %1$S in pipe step #%2$s has a missing resource:%n%3$s", command, commandIndex, exception.getMessage()));
            if (!isNullOrEmpty(exception.getUri())) {
                builder.append("\n");
                builder.append("URI: ");
                builder.append(exception.getUri());
            }
            builder.append("\n\n");
            if (commandIndex > 1) {
                builder.append(String.format(" .. | %1$s | ..", commandString));
            } else {
                builder.append(String.format("  %1$s | ..", commandString));
            }
        }

        printCommandSource(exception, builder);
        printCommonStack(exception, builder);

        return builder.toString();
    }

    private static String gorDataExceptionToString(GorDataException exception) {
        StringBuilder builder = new StringBuilder();

        builder.append("==== Data Error ====\n");

        if (exception.getColumnNumber() == -1) {
            builder.append(exception.getMessage());
        } else {
            builder.append(String.format("An error has occurred in data column %2$d:%n%1$s", exception.getMessage(), exception.getColumnNumber()));
        }

        builder.append("\n");

        if (!isNullOrEmpty(exception.getHeader())) {
            builder.append("\n");
            builder.append("Header: ");
            builder.append(exception.getHeader());
        }

        if (!isNullOrEmpty(exception.getRow())) {
            builder.append("\n");
            builder.append("Row: ");
            builder.append(exception.getRow());
        }

        printCommandSource(exception, builder);
        printCommonStack(exception, builder);

        return builder.toString();
    }

    private static String gorSystemExceptionToString(GorSystemException exception) {
        StringBuilder builder = new StringBuilder();

        builder.append("==== System Error ====\n");
        builder.append(exception.getMessage());
        builder.append("\n");

        printCommonStack(exception, builder);

        return builder.toString();
    }



    private static final String ERROR_TYPE = "errorType";
    private static final String GOR_MESSAGE = "gorMessage";
    private static final String MESSAGE = "message";
    private static final String REQUEST_ID = "requestId";
    private static final String QUERY = "query";
    private static final String COMMAND_NAME = "commandName";
    private static final String COMMAND_INDEX = "commandIndex";
    private static final String COMMAND_SOURCE = "commandSource";
    private static final String COMMAND = "command";
    private static final String EXTRA_INFO = "extraInfo";
    private static final String OPTION = "option";
    private static final String OPTION_VALUE = "optionValue";
    private static final String URI = "uri";
    private static final String HEADER = "header";
    private static final String ROW = "row";
    private static final String COLUMN_NUMBER = "columnNumber";
    private static final String STACK_TRACE = "stackTrace";

    @SuppressWarnings("unused") // Used by gor-services (gorserver and gorworker)
    public static synchronized String gorExceptionToJson(Throwable exception) {

        JSONObject obj = new JSONObject();

        obj.put(ERROR_TYPE, getErrorType(exception));
        obj.put(GOR_MESSAGE, gorExceptionToString(exception));
        obj.put(MESSAGE, exception.getMessage());

        if (showStackTrace) {
            obj.put(STACK_TRACE, getStackTrace(exception));
        }

        if (exception instanceof GorException) {
            obj.put(REQUEST_ID, ((GorException) exception).getRequestID());
        }

        if (exception instanceof GorUserException) {
            gorUserExceptionToJson((GorUserException) exception, obj);
        }

        if (exception instanceof GorParsingException) {
            gorParsingExceptionToJson((GorParsingException) exception, obj);
        } else if (exception instanceof GorResourceException) {
            gorResourceExceptionToJson((GorResourceException) exception, obj);
        } else if (exception instanceof GorDataException) {
            gorDataExceptionToJson((GorDataException) exception, obj);
        }

        return obj.toString();
    }

    private static void gorUserExceptionToJson(GorUserException exception, JSONObject obj) {
        addJsonEntry(QUERY, exception.getQuery(), obj);
        addJsonEntry(COMMAND_NAME, exception.getCommandName(), obj);
        addJsonEntry(COMMAND_INDEX, exception.getCommandIndex(), obj);
        addJsonEntry(COMMAND_SOURCE, exception.getQuerySource(), obj);
        addJsonEntry(COMMAND, exception.getCommandStep(), obj);
        addJsonEntry(EXTRA_INFO, exception.getExtraInfo(), obj);
    }

    private static void gorParsingExceptionToJson(GorParsingException exception, JSONObject obj) {
        addJsonEntry(OPTION, exception.getOption(), obj);
        addJsonEntry(OPTION_VALUE, exception.getPayload(), obj);
    }

    private static void gorResourceExceptionToJson(GorResourceException exception, JSONObject obj) {
        addJsonEntry(URI, exception.getUri(), obj);
    }

    private static void gorDataExceptionToJson(GorDataException exception, JSONObject obj) {
        addJsonEntry(HEADER, exception.getHeader(), obj);
        addJsonEntry(ROW, exception.getRow(), obj);
        addJsonEntry(COLUMN_NUMBER, exception.getColumnNumber(), obj);
    }

    private static void addJsonEntry(String name, String value, JSONObject obj) {
        if (!isNullOrEmpty(value)) obj.put(name, value);
    }

    private static void addJsonEntry(String name, Integer value, JSONObject obj) {
        if (value >= 0) obj.put(name, value);
    }

    private static String getErrorType(Throwable exception) {
        Class<?> enclosingClass = exception.getClass().getEnclosingClass();
        return Objects.requireNonNullElseGet(enclosingClass, exception::getClass).getSimpleName();
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }

    private static void printCommonStack(GorException exception, StringBuilder builder) {
        printRequestId(exception, builder);
        printGorVersion(builder);
        printStackTrace(exception, builder);
        printEndMessage(builder);
    }

    public static void printGorVersion(StringBuilder builder) {
        builder.append("\n");
        builder.append("GOR Version: ");

        String version = ExceptionUtilities.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }

        builder.append(version);
        builder.append("\n");
    }

    public static void printStackTrace(Throwable exception, StringBuilder builder) {

        if (!showStackTrace) return;

        builder.append("\n\n");
        builder.append("Stack Trace:\n");
        builder.append(getStackTrace(exception));
    }

    private static void printRequestId(GorException exception, StringBuilder builder) {

        String requestId = exception.getRequestID();
        if (isNullOrEmpty(requestId)) return;

        builder.append("\n\n");
        builder.append("Request ID: ");
        builder.append(requestId);

    }

    private static void printCommandSource(GorUserException exception, StringBuilder builder) {

        String commandSource = exception.getQuerySource();
        if (isNullOrEmpty(commandSource) || commandSource.equalsIgnoreCase("thepgorquery")) return;

        builder.append("\n\n");
        builder.append("Part of create statement: create ");
        builder.append(commandSource);
        builder.append(" = ...");

    }

    private static void printEndMessage(StringBuilder builder) {
        builder.append("\n\n");
    }

    @SuppressWarnings("unused") // Used by gor-services (ResqueJob)
    public synchronized static GorException gorExceptionFromJson(String error) {
        GorException exception = null;
        JSONParser parser = new JSONParser();

        if (error == null || error.isEmpty()) {
            exception =  new GorSystemException("Got error with null or empty json", null);
        } else {
            // Asssume we have json.
            try {
                JSONObject obj = (JSONObject) parser.parse(error);
                String errorType = obj.containsKey(ERROR_TYPE) ? (String) obj.get(ERROR_TYPE) : "";

                if (errorType.startsWith("GorParsingException")) {
                    exception = createGorParsingExceptionFromJSON(obj);
                } else if (errorType.startsWith("GorDataException")) {
                    exception = createGorDataExceptionFromJSON(obj);
                } else if (errorType.startsWith("GorResourceException")) {
                    exception = createGorResourceException(obj);
                } else {
                    exception = createGorSystemException(obj);
                }

                exception.requestID = getStringValue(obj, REQUEST_ID);

                if (obj.containsKey(STACK_TRACE)) {
                    setStackTrace(exception, obj);
                }
            } catch (Exception e) {
                exception = new GorSystemException("Got error: '" + error + "'\n" +
                        "Trying to parse this error as json error resulted in an exception.", e);
            }
        }

        return exception;
    }

    public static GorResourceException mapGorResourceException(String resourceName, String uri, Exception e) {
        if (uri.trim().startsWith("[") && uri.trim().endsWith("]")) {
            return new GorMissingRelationException(String.format("Virtual relation '%s' is missing", resourceName), uri, e);
        } else {
            return new GorResourceException("Input source does not exist: " + resourceName, uri, e);
        }
    }

    public static String getFullCause(Throwable e) {
        var t = e;
        var builder = new StringBuilder();

        while (t != null && (t.getCause() != null || t.getCause() != t)) {
            var message = t.getMessage();
            if (ExceptionUtilities.isNullOrEmpty(message)) break;
            builder.append(t.getMessage());
            builder.append("\n");
            t = t.getCause();
        }

        return builder.toString().trim();
    }

    // Find the cause by ignoring ExecutionExceptions and GORExceptions (unless it is last one)
    public static Throwable getUnderlyingCause(Exception ex) {
        Throwable cause = ex;
        while (true) {
            if (cause.getCause() == null) {
                return cause;
            }

            if (cause instanceof ExecutionException
                    || cause instanceof UncheckedExecutionException
                    || cause instanceof CompletionException
                    || cause instanceof GorException) {
                cause = cause.getCause();
            } else {
                return cause;
            }
        }
    }

    /**
     * Attempt to deserialize the stacktrace from obj into array of StackTraceElements[] that is set on the exception.
     * If we do not overwrite the stacktrace this way then Java will generate a useless stacktrace from the callsite
     * where this exception is thrown from.
     * We are not updating cause when recreating GOR exception from json so we prefer the last cause stracktrace.
     */
    private static void setStackTrace(GorException exception, JSONObject obj) {

        try {
            String stackTraceText = obj.get(STACK_TRACE).toString();
            String[] exceptions = stackTraceText.split("Caused by:");
            String[] stackTrace = exceptions[exceptions.length-1].split("\n");
            List<StackTraceElement> stackTraceElements = new ArrayList<>();
            StackTraceElement stackTraceElement;
            for (String st : stackTrace) {
                if (st.startsWith("\tat ")) {
                    if (st.contains("(")) {
                        stackTraceElement = parseStacktraceLineWithFile(st);
                    } else {
                        stackTraceElement = parseStacktraceLineWithoutFile(st);
                    }
                    stackTraceElements.add(stackTraceElement);
                }
            }
            exception.setStackTrace(stackTraceElements.toArray(new StackTraceElement[0]));
        } catch (Exception e) {
            log.error("Unexpected error when parsing stacktrace of a json-fied exception during deserialization into a GorException", e);
        }
    }

    private static StackTraceElement parseStacktraceLineWithoutFile(String st) {
        String methodName = st.substring(st.indexOf(' ') + 1);
        String declaringClass = methodName.substring(0, methodName.lastIndexOf('.'));
        return new StackTraceElement(declaringClass, methodName, null, -1);
    }

    private static StackTraceElement parseStacktraceLineWithFile(String st) {
        String methodName = st.substring(st.indexOf(' ') + 1, st.indexOf('('));
        String declaringClass = methodName.substring(0, methodName.lastIndexOf('.'));
        if (st.contains(":")) {
            String fileName = st.substring(st.indexOf('(') + 1, st.indexOf(':'));
            int lineNumber = Integer.parseInt(st.substring(st.indexOf(':') + 1, st.indexOf(')')));
            return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        } else {
            String fileName = st.substring(st.indexOf('(') + 1, st.indexOf(')'));
            return new StackTraceElement(declaringClass, methodName, fileName, -1);
        }
    }

    private static GorException createGorSystemException(JSONObject obj) {
        return new GorSystemException(getStringValue(obj, MESSAGE), null);
    }

    private static GorException createGorResourceException(JSONObject obj) {
        GorResourceException exception = new GorResourceException(getStringValue(obj, MESSAGE),
                getStringValue(obj, URI),
                null, false);

        updateGorUserException(obj, exception);

        return exception;
    }

    private static GorException createGorDataExceptionFromJSON(JSONObject obj) {
        GorDataException exception = new GorDataException(getStringValue(obj, MESSAGE),
                getIntValue(obj, COLUMN_NUMBER),
                getStringValue(obj, HEADER),
                getStringValue(obj, ROW),
                null, false);

        updateGorUserException(obj, exception);

        return exception;
    }

    private static GorException createGorParsingExceptionFromJSON(JSONObject obj) {
        GorParsingException exception = new GorParsingException(getStringValue(obj, MESSAGE),
                getStringValue(obj, OPTION),
                getStringValue(obj, OPTION_VALUE));

        updateGorUserException(obj, exception);

        return exception;
    }

    private static void updateGorUserException(JSONObject obj, GorUserException exception) {
        exception.setQuery(getStringValue(obj, QUERY));
        exception.setCommandName(getStringValue(obj, COMMAND_NAME));
        exception.setCommandIndex(getIntValue(obj, COMMAND_INDEX));
        exception.setCommandStep(getStringValue(obj, COMMAND));
        exception.setQuerySource(getStringValue(obj, COMMAND_SOURCE));
        exception.setExtraInfo(getStringValue(obj, EXTRA_INFO));
    }

    private static String getStringValue(JSONObject obj, String key) {
        return obj != null && obj.containsKey(key) ? (String) obj.get(key) : "";
    }

    private static Integer getIntValue(JSONObject obj, String key) {
        return obj != null && obj.containsKey(key) ? Integer.parseInt(obj.get(key).toString()) : -1;
    }
}
