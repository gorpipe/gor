package org.gorpipe.gor.model;


import java.util.HashMap;

import static org.gorpipe.gor.util.ConsoleColors.*;

public class RowTypeColorize implements RowColorize {
    private HashMap<String, String> formats = new HashMap() {
        {
            put("S", GREEN);
            put("I", CYAN);
            put("D", PURPLE);
            put("L", YELLOW);
        }
    };

    @Override
    public String formatColumn(int index, String value, String type) {
        if (type == null) {
            return value;
        }

        var t = type.toUpperCase();
        if (formats.containsKey(t)) {
            return formats.get(t) + value + RESET;
        } else {
            return value;
        }
    }

    @Override
    public String formatHeaderColumn(int index, String value) {
        return value;
    }
}
