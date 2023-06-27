package org.gorpipe.gor.driver.providers.stream.datatypes.vcf;

import org.gorpipe.gor.model.ContigDataScheme;

import java.util.HashMap;
import java.util.Map;

public class VcfContigDataScheme implements ContigDataScheme {
    final Map<Integer, String> id2chr = new HashMap<>();
    final Map<Integer, Integer> order2id = new HashMap<>();
    final Map<Integer, Integer> id2order = new HashMap<>();
    @Override
    public String id2chr(int i) {
        return id2chr.get(i);
    }

    @Override
    public byte[] id2chrbytes(int i) {
        return id2chr.get(i).getBytes();
    }

    @Override
    public int id2order(int i) {
        return id2order.get(i);
    }

    @Override
    public void setId2order(int i, int val) {
        id2order.put(i, val);
        order2id.put(val, i);
    }

    @Override
    public void setId2chr(int i, String chr) {
        id2chr.put(i, chr);
    }

    @Override
    public void newOrder(int[] neworder) {
        for (int i = 0; i < neworder.length; i++) {
            id2order.put(i, neworder[i]);
            order2id.put(neworder[i], i);
        }
    }

    @Override
    public void newId2Chr(String[] newid2chr) {
        for (int i = 0; i < newid2chr.length; i++) {
            id2chr.put(i, newid2chr[i]);
        }
    }

    @Override
    public int order2id(int i) {
        return order2id.get(i);
    }

    @Override
    public int length() {
        return id2chr.size();
    }
}
