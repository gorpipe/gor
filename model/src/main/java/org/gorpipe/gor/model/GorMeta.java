package org.gorpipe.gor.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GorMeta {
    String minChr = null;
    int minPos = -1;
    String maxChr = null;
    int maxPos = -1;
    String md5 = null;
    String cardColName = null;
    int cardColIndex = -1;
    Set<String> cardSet = new TreeSet<>();

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMd5() {
        return md5;
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

        if(cardColIndex >= 0) cardSet.add(ir.colAsString(cardColIndex).toString());
    }

    public String getRange() {
        return minChr!=null ? minChr + "\t" + minPos + "\t" + maxChr + "\t" + maxPos : "";
    }

    @Override
    public String toString() {
        String ret = "";
        if(minChr!=null) ret += "##RANGE: " + getRange() + "\n";
        if(md5!=null) ret += "##MD5: " + md5 + "\n";
        if(cardColIndex != -1) {
            String cardStr = cardSet.toString();
            ret += "##CARDCOL["+cardColName+"]: " + cardStr.substring(1,cardStr.length()-1).replace(" ","");
        }
        return ret;
    }
}
