package org.gorpipe.gor.model;

import java.util.*;

public class GorMeta {
    String minChr = null;
    int minPos = -1;
    String maxChr = null;
    int maxPos = -1;
    String md5 = null;
    String tags = null;
    long lineCount = 0;
    String cardColName = null;
    int cardColIndex = -1;
    Set<String> cardSet = new TreeSet<>();
    String query;

    public static final String MD5_HEADER = "## MD5";
    public static final String QUERY_HEADER = "## QUERY";
    public static final String CARDCOL_HEADER = "## CARDCOL";
    public static final String RANGE_HEADER = "## RANGE";
    public static final String LINES_HEADER = "## LINES";
    public static final String TAGS_HEADER = "## TAGS";

    public boolean linesWritten() {
        return minChr != null;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTags() {
        return tags;
    }

    public void initCardCol(String cardCol, String header) {
        List<String> hsplit = Arrays.asList(header.toLowerCase().split("\t"));
        cardColName = cardCol;
        cardColIndex = hsplit.indexOf(cardCol.toLowerCase());
    }

    public void updateRange(Row ir) {
        if(minChr==null) {
            minChr = ir.chr;
            minPos = ir.pos;
        }
        maxChr = ir.chr;
        maxPos = ir.pos;
        lineCount++;

        if(cardColIndex >= 0) cardSet.add(ir.colAsString(cardColIndex).toString());
    }

    public String getRange() {
        return minChr!=null ? minChr + "\t" + minPos + "\t" + maxChr + "\t" + maxPos : "";
    }

    @Override
    public String toString() {
        String ret = "";
        if(minChr!=null) ret += RANGE_HEADER + ": " + getRange() + "\n";
        if(md5!=null) ret += MD5_HEADER + ": " + md5 + "\n";
        if(query!=null) ret += QUERY_HEADER + ": " + query + "\n";
        if(tags!=null&&tags.length()>0) ret += TAGS_HEADER + ": " + tags + "\n";
        if(lineCount!=0) ret += LINES_HEADER + ": " + lineCount + "\n";
        if(cardColIndex != -1) {
            String cardStr = cardSet.toString();
            ret += CARDCOL_HEADER + "["+cardColName+"]: " + cardStr.substring(1,cardStr.length()-1).replace(" ","");
        }
        return ret;
    }
}
