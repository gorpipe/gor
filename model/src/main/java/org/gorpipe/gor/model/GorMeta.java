package org.gorpipe.gor.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class GorMeta {
    String minChr = null;
    int minPos = -1;
    String maxChr = null;
    int maxPos = -1;
    String md5 = null;
    long lineCount = 0;
    String cardColName = null;
    int cardColIndex = -1;
    Set<String> cardSet = new TreeSet<>();

    static String MD5_HEADER = "## MD5";
    static String CARDCOL_HEADER = "## CARDCOL";
    static String RANGE_HEADER = "## RANGE";
    static String LINES_HEADER = "## LINES";

    public static void writeDictionaryFromMeta(Path outfolderpath, Path dictionarypath) throws IOException {
        List<Path> metapaths = Files.walk(outfolderpath).filter(p -> p.getFileName().toString().endsWith(".meta")).collect(Collectors.toList());
        int i = 0;
        for(Path p : metapaths) {
            Optional<String> omd5 = Files.lines(p).filter(s -> s.startsWith(MD5_HEADER)).map(s -> s.substring(s.indexOf(':')+1).trim()).findFirst();
            Optional<String> cc = Files.lines(p).filter(s -> s.startsWith(CARDCOL_HEADER)).map(s -> s.substring(s.indexOf(':')+1).trim()).findFirst();
            Optional<String> range = Files.lines(p).filter(s -> s.startsWith(RANGE_HEADER)).map(s -> s.substring(s.indexOf(':')+1).trim()).findFirst();
            if(range.isPresent()) {
                String s = range.get();
                var outfile = omd5.orElseGet(() -> {
                    String o = outfolderpath.relativize(p).toString();
                    return o.substring(0,o.length()-10);
                });
                outfile = outfile+".gorz";
                i+=1;
                String gordline;
                if(cc.isPresent()) {
                    String ccstr = cc.get();
                    gordline = outfile+"\t"+i+"\t"+s+"\t"+ccstr;
                } else {
                    gordline = outfile+"\t"+i+"\t"+s;
                }
                Files.writeString(dictionarypath, gordline+"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            if(omd5.isPresent()) {
                String md5 = omd5.get();
                Path dm = p.getParent().resolve(md5+".gorz.meta");
                if(!Files.exists(dm)) Files.move(p, dm);
                else Files.delete(p);

                String fn = p.getFileName().toString();
                Path g = p.getParent().resolve(fn.substring(0,fn.length()-5));
                Path d = p.getParent().resolve(md5+".gorz");
                if(!Files.exists(d)) Files.move(g, d);
                else Files.delete(g);
            }
        }
    }

    public boolean linesWritten() {
        return minChr != null;
    }

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
        if(lineCount!=0) ret += LINES_HEADER + ": " + lineCount + "\n";
        if(cardColIndex != -1) {
            String cardStr = cardSet.toString();
            ret += CARDCOL_HEADER + "["+cardColName+"]: " + cardStr.substring(1,cardStr.length()-1).replace(" ","");
        }
        return ret;
    }
}
