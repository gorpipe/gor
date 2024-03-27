package org.gorpipe.gor.table;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryCache;
import org.gorpipe.gor.table.files.GorTable;
import org.gorpipe.gor.table.files.NorTable;
import org.gorpipe.gor.util.DataUtil;


import java.io.IOException;

public class TableFactory {

    private TableFactory() {}

    /**
     * Factory method to create table instances from file name.
     *
     * Note:   This factory is used tables we are going to update (in most cases) and hence the use of cache is
     *         very tricky and should not be used.  In addition, when entering trans we reload everything from disk
     *         anyway so use of cache is pointless.
     */
    public static Table getTable(String tablePath, FileReader fileReader) {
        Table table;
        if (DataUtil.isGord(tablePath)) {
            try {
                table = GorDictionaryCache.dictCache.getOrCreateTable(tablePath, fileReader, false);
            } catch (IOException ioe) {
                throw new GorSystemException(ioe);
            }
        } else if (DataUtil.isNord(tablePath)) {
            table = new NorDictionaryTable(tablePath, fileReader);
        } else if (DataUtil.isGor(tablePath) || DataUtil.isGorz(tablePath) || DataUtil.isAnyVcf(tablePath)) {
            table = new GorTable(tablePath, fileReader);
        } else if (DataUtil.isAnyCsv(tablePath) || DataUtil.isNor(tablePath)) {
            table = new NorTable(tablePath, fileReader);
        } else {
            throw new GorSystemException(String.format("File type: %s, is not a supported ", tablePath), null);
        }

        return table;
    }
}
