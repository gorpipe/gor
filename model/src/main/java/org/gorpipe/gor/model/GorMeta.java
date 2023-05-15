package org.gorpipe.gor.model;

import org.gorpipe.gor.function.GorRowInferFunction;
import org.gorpipe.gor.table.util.GenomicRange;

import java.nio.file.Path;
import java.util.*;

public class GorMeta extends BaseMeta {

    public static final String HEADER_QUERY_KEY = "QUERY";
    public static final String HEADER_RANGE_KEY = "RANGE";
    public static final String HEADER_CARDCOL_KEY = "CARDCOL";

    public static GorMeta createAndLoad(FileReader fileReader, String metaPath) {
        GorMeta meta = new GorMeta();
        meta.loadAndMergeMeta(fileReader, metaPath);
        return meta;
    }

    public static GorMeta createAndLoad(Path metaPath) {
        GorMeta meta = new GorMeta();
        meta.loadAndMergeMeta(metaPath);
        return meta;
    }

    public GorMeta() {
        super();
    }

    // TODO:  Should really collect these stats on the stats object and only store reuslts in this object.

    String minChr = null;
    int minPos = -1;
    String maxChr = null;
    int maxPos = -1;
    long lineCount = -1;
    String cardColName = null;
    int cardColIndex = -1;
    Set<String> cardSet = new TreeSet<>();
    Row inferRow = null;
    GorRowInferFunction gorRowInferFunction;
    int maxseg = -1;

    public void setQuery(String query) {
        setProperty(HEADER_QUERY_KEY, query);
    }

    public String getQuery() {
        return getProperty(HEADER_QUERY_KEY);
    }

    public void initMetaStats(String cardCol, String header) {
        initMetaStats(cardCol, header, false, false);
    }

    public void initMetaStats(String cardCol, String header, boolean infer, boolean maxseg) {
        if (cardCol != null) {
            List<String> hsplit = Arrays.asList(header.toLowerCase().split("\t"));
            cardColName = cardCol;
            cardColIndex = hsplit.indexOf(cardCol.toLowerCase());
        }
        if (infer) {
           gorRowInferFunction = new GorRowInferFunction();
        }
        if (maxseg) this.maxseg = 0;
        this.setFileHeader(header.split("\t"));

        lineCount = 0;
    }

    public void setSchema(String[] schema) {
        setProperty(HEADER_SCHEMA_KEY, String.join(",", schema));
    }

    public void updateMetaStats(Row ir) {
        if (minChr == null) {
            minChr = ir.chr;
            minPos = ir.pos;
            if (gorRowInferFunction!=null) inferRow = gorRowInferFunction.inferBoth(ir, ir);
        } else if (gorRowInferFunction!=null) {
            gorRowInferFunction.inferOther((RowBase)inferRow, ir);
        }
        maxChr = ir.chr;
        maxPos = ir.pos;

        lineCount++;
        if (maxseg!=-1) maxseg = Math.max(maxseg, ir.colAsInt(2)-maxPos);
        if (cardColIndex >= 0) cardSet.add(ir.colAsString(cardColIndex).toString());
    }

    public GenomicRange getRange() {
        if (minChr != null) {
            return new GenomicRange(minChr, minPos, maxChr, maxPos);
        } else if (containsProperty(HEADER_RANGE_KEY)) {
            return GenomicRange.parseGenomicRange(getProperty(HEADER_RANGE_KEY));
        } else {
            return GenomicRange.EMPTY_RANGE;
        }
    }

    public String[] getCordColTags() {
        if (cardSet.size() > 0) {
            return cardSet.toArray(new String[0]);
        } else {
            return getProperty(GorMeta.HEADER_CARDCOL_KEY, ":").split(":")[1].trim().split(",");
        }
    }

    @Override
    public String formatHeader() {
        updateMeta();
        return super.formatHeader();
    }

    private void updateMeta() {
        if (minChr != null) setProperty(HEADER_RANGE_KEY, getRange().formatAsTabDelimited());
        if (lineCount != -1) setProperty(HEADER_LINE_COUNT_KEY, Long.toString(lineCount));
        if (inferRow != null) setProperty(HEADER_SCHEMA_KEY, inferRow.toString().replace('\t',','));
        if (maxseg >= 0) setProperty(MAXSEG_SCHEMA_KEY, Integer.toString(maxseg));
        if (cardColIndex != -1) {
            String cardStr = cardSet.toString();
            setProperty(HEADER_CARDCOL_KEY, "[" + cardColName + "]: " + cardStr.substring(1, cardStr.length() - 1).replace(" ", ""));
        }
        if (fileHeader != null && fileHeader.length > 0) setColumns(fileHeader);
    }
}
