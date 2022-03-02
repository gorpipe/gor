package org.gorpipe.gor.table;

import org.apache.commons.io.FilenameUtils;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.files.GorTable;
import org.gorpipe.gor.table.files.NorTable;

import java.net.URI;

public class TableFactory {

    private TableFactory() {}

    /**
     * Factory method to create table instances from file name.
     */
    public static BaseTable getTable(String tablePath, FileReader fileReader) {

        BaseTable table;
        if (fileReader.isDirectory(tablePath)) {
            table = new DictionaryTable(tablePath);
        } else {

            String extension = FilenameUtils.getExtension(tablePath).toLowerCase();
            switch (extension) {
                case "gord":
                case "gort":
                    table = new DictionaryTable(tablePath);
                    break;
                case "gor":
                case "gorz":
                case "vcf":
                    table = new GorTable(URI.create(tablePath));
                    break;
                default:
                    table = new NorTable(URI.create(tablePath));
            }
        }
        table.setFileReader(fileReader);
        return table;
    }

    public static boolean isDictionary(String tablePath, FileReader fileReader) {
        if (fileReader.isDirectory(tablePath)) {
            return true;
        } else {
            String extension = FilenameUtils.getExtension(tablePath).toLowerCase();
            switch (extension) {
                case "gord":
                case "gort":
                    return true;
                default:
                    return false;
            }
        }
    }
}
