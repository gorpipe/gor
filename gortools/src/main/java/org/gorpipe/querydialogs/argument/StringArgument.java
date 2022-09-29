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
public class StringArgument extends FormatArgument {
    private final boolean singleSelection;
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

}
