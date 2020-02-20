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

package org.gorpipe.gor.cli;

import com.google.common.collect.Lists;
import org.gorpipe.base.config.annotations.Documentation;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.DefaultValue;
import org.aeonbits.owner.Config.Key;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;

/**
 * A main class to scan through the runtime classpath and find all descendants of {@link Config}. Each of them
 * is then analyzed through reflection and documentation for all config options that are declared in each of them
 * is presented as a simple ASCII table.
 */
public class GorConfigDoc {
    public static void main(String[] args) {
        Reflections reflections = new Reflections("com.nextcode", "com.decode", "org.gorpipe");
        Set<Class<? extends Config>> configs = reflections.getSubTypesOf(Config.class);

        for (Class config : configs) {
            if (!config.isInterface()) {
                // We don't care about concrete classes. They are probably test implementations of a config interface.
                continue;
            }

            // Filter out any methods that do not have the @Key annotation
            List<Method> methods = Lists.newLinkedList(Arrays.asList(config.getDeclaredMethods()));
            methods.removeIf(m -> m.getAnnotation(Key.class) == null);

            if (methods.isEmpty()) {
                System.err.println(String.format("Class %s is a Config interface but doesn't have any @Key annotations.", config.getName()));
                continue;
            }

            String name = config.getSimpleName().length() > 0 ? config.getSimpleName() : config.getName();
            Table table = new Table("### " + name);
            methods.sort((m1, m2) -> {
                Key k1 = m1.getAnnotation(Key.class);
                Key k2 = m2.getAnnotation(Key.class);
                return k1.value().compareTo(k2.value());
            });

            table.addHeader("key", "default value", "description");

            for (Method method : methods) {
                Key key = method.getAnnotation(Key.class);
                DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
                Documentation documentation = method.getAnnotation(Documentation.class);

                table.addRow(key == null ? "" : key.value(), defaultValue == null ? "" : defaultValue.value(),
                        documentation == null ? "" : documentation.value());
            }
            System.out.println(table.toString());
        }
    }
}

class Table {
    private String header;
    private List<String[]> rows = new LinkedList<>();
    private List<Integer> colwidths = new LinkedList<>();

    Table(String header) {
        this.header = header;
    }

    void addHeader(String... header) {
        addRow(header);
        String[] underlines = new String[header.length];
        for (int i = 0; i < header.length; i++) {
            int length = header[i].length();
            StringBuilder sb = new StringBuilder(length);
            for (int k = 0; k < length; k++) {
                sb.append("=");
            }
            underlines[i] = sb.toString();
        }
        addRow(underlines);
    }

    void addRow(String... cols) {
        rows.add(cols);
        for (int i = 0; i < cols.length; i++) {
            int width = cols[i].length();
            if (i >= colwidths.size()) {
                colwidths.add(width);
            } else {
                colwidths.set(i, Math.max(colwidths.get(i), width));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder formatBuilder = new StringBuilder();
        for (int width : colwidths) {
            formatBuilder.append("%-").append(width).append(".").append(width).append("s  ");
        }
        formatBuilder.replace(formatBuilder.length() - 2, formatBuilder.length(), "%n");
        String format = formatBuilder.toString();

        try (Formatter formatter = new Formatter()) {
            for (String[] row : rows) {
                formatter.format(format, (Object[]) row);
            }

            return header + "\n" + formatter.out().toString();
        }
    }
}
