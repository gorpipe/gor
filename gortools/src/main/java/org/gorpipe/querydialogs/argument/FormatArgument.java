package org.gorpipe.querydialogs.argument;

import org.gorpipe.querydialogs.Argument;
import org.gorpipe.querydialogs.ArgumentDescription;
import org.gorpipe.querydialogs.ArgumentType;
import org.gorpipe.querydialogs.util.ValueFormatter;

import java.net.URI;
import java.util.List;

public abstract class FormatArgument extends Argument {
    ValueFormatter formatter;
    boolean quoted;
    String format;

    public FormatArgument(ArgumentType type, ArgumentDescription argDescr, Boolean optional, Object defaultValue, List<?> allowedValues, URI valuesPath, List<String> operators, Boolean advanced, Integer displayWidth) {
        super(type, argDescr, optional, defaultValue, allowedValues, valuesPath, operators, advanced, displayWidth);
    }

    public FormatArgument(Argument arg) {
        super(arg);
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
}
