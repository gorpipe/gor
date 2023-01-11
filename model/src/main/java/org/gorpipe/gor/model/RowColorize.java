package org.gorpipe.gor.model;

public interface RowColorize {
    String formatColumn(int index, String value, String type);
    String formatHeaderColumn(int index, String value, String type);
}
