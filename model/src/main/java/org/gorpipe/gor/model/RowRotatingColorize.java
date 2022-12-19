package org.gorpipe.gor.model;


import static org.gorpipe.gor.util.ConsoleColors.*;

public class RowRotatingColorize implements RowColorize {
    private String[] formats = {
            GREEN,
            PURPLE,
            CYAN,
            YELLOW
    };

    private String[] formatsUnderlined = {
            GREEN_UNDERLINED,
            PURPLE_UNDERLINED,
            CYAN_UNDERLINED,
            YELLOW_UNDERLINED
    };

    @Override
    public String formatColumn(int index, String value, String type) {
        var i = index % formats.length;
        return formats[i] + value + RESET;
    }

    @Override
    public String formatHeaderColumn(int index, String value) {
        var i = index % formatsUnderlined.length;
        return formatsUnderlined[i] + value + RESET;
    }
}
