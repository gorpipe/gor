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
package org.gorpipe.querydialogs.argument;

import org.gorpipe.querydialogs.FilteringColumnDefinition;
import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.util.SubjectLists;
import org.gorpipe.querydialogs.util.ValueFormatter;

import java.net.URI;
import java.util.List;

/**
 * Represents an argument that expects a string value.
 *
 * @author arnie
 * @version $Id$
 */
@SuppressWarnings({"javadoc"})
public class StringArgument extends Argument {
    private final boolean singleSelection;
    private ValueFormatter formatter;
    private boolean quoted;
    private String format;
    private FilteringColumnDefinition helpDefinition;
    private SubjectLists persistingValueObject;

    /**
     * Constructor.
     */
    public StringArgument(ArgumentDescription argDescr, boolean quoted, Boolean optional, Object defaultValue,
                          List<? extends Object> allowedValues, URI valuesPath, ValueFormatter formatter, List<String> operators, Boolean advanced,
                          Integer displayWidth, ArgumentType argType, Boolean singleSelection) {
        super(argType, argDescr, optional, defaultValue, allowedValues, valuesPath, operators, advanced, displayWidth);
        this.singleSelection = singleSelection != null && singleSelection.booleanValue();
        setFormattingParameters(formatter, quoted, ValueFormatter.VALUES_FORMAT);
    }

    public StringArgument(final StringArgument arg) {
        super(arg);
        this.singleSelection = arg.singleSelection;
        setFormattingParameters(arg.formatter, arg.quoted, arg.format);
        if (arg.persistingValueObject != null) {
            this.persistingValueObject = new SubjectLists(arg.persistingValueObject);
        }
    }

    /**
     * Get the format for the string argument.
     *
     * @return the format for the string argument
     */
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormattedValue() {
        if (formatter == null) {
            if (getValue() == null) return null;
            return getValue().toString().trim();
        }

        if (getValue() == null || getValue().toString().trim().isEmpty())
            return formatter.format(ValueFormatter.EMPTY_FORMAT, "");

        StringBuilder sb = new StringBuilder();
        String delim = quoted ? "','" : ",";
        if (quoted) sb.append('\'');

        String value = getValue().toString().trim();
        value = escapeQuote(value);

        sb.append(String.join(delim, value.split("\\s*,\\s*")));
        if (quoted) sb.append('\'');

        String newValue = sb.toString();
        String result = formatter.format(format, newValue);

        if (result == null) {
            result = formatter.format(ValueFormatter.DEFAULT_FORMAT, newValue);
        }

        return result;
    }

    /**
     * This method takes in a String and splits it according to ,. Then within those segments escapes
     * all ' that are in the middle of it, not the start and end characters. For example the following Strings
     * handled as:
     * 'rs544101329','rs28970552' > 'rs544101329','rs28970552'
     * 'rs544101329','rs2897'0552' > 'rs544101329','rs2897'\0552'
     * REACTOME||mRNA_3'-end_processing > REACTOME||mRNA_3\'-end_processing
     * REACTOME||mRNA_3-end_processing' > REACTOME||mRNA_3-end_processing\'
     * 'abc',','def'> 'abc',\','def'
     *
     * @param s
     * @return
     */
    private String escapeQuote(String s) {
        if (!s.contains("'")) {
            return s;
        }

        if (!s.contains(",")) {
            return s.replace("'", "\\'");
        }

        StringBuilder sb = new StringBuilder();
        String[] strings = s.split(",");
        for (int i = 0; i < strings.length; i++) {
            String currentString = strings[i];
            if (currentString.contains("'")) {
                int length = currentString.length();
                char start = currentString.charAt(0);
                char end = currentString.charAt(length - 1);
                if (length == 1 || (start != '\'' || end != '\'')) {
                    sb.append(currentString.replace("'", "\\'"));
                } else {
                    String middle = currentString.substring(1, length - 1);
                    String middleEscaped = middle.replace("'", "\\'");
                    sb.append(start).append(middleEscaped).append(end);
                }
            } else {
                sb.append(currentString);
            }

            if (i < strings.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Get help definition for argument.
     *
     * @return the help definition
     */
    public FilteringColumnDefinition getHelpDefinition() {
        return helpDefinition;
    }

    /**
     * Set help definition for argument.
     *
     * @param helpDefinition the help definition to set
     */
    public void setHelpDefinition(final FilteringColumnDefinition helpDefinition) {
        this.helpDefinition = helpDefinition;
    }

    /**
     * Get the persisting value.
     *
     * @return the persisting value
     */
    public Object getPersistingValueObject() {
        return persistingValueObject;
    }

    /**
     * Set the persisting value.
     *
     * @param persistingValueObject the persisting value
     */
    public void setPersistingValueObject(final SubjectLists persistingValueObject) {
        this.persistingValueObject = persistingValueObject;
    }

    @Override
    public StringArgument copyArgument() {
        return new StringArgument(this);
    }

    /**
     * Check if only single selection is allowed for argument.
     *
     * @return {@code true} if only single selection is allowed for argument, otherwise {@code false}
     */
    public boolean isSingleSelection() {
        return singleSelection;
    }

    @Override
    public boolean isEmpty() {
        return getValue() == null || getValue().toString().trim().isEmpty();
    }

    private void setFormattingParameters(final ValueFormatter inpFormatter, final boolean inpQuoted, final String format) {
        this.formatter = inpFormatter;
        this.format = format;
        this.quoted = inpQuoted;
    }

    public ValueFormatter getFormatter() {
        return formatter;
    }

}
