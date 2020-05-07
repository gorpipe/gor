/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.model.genome.files.gor;

import org.apache.parquet.schema.Type;
import org.gorpipe.model.gor.RowObj;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.OriginalType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NorParquetLine extends ParquetLine {
    private int[] sortCols;

    public NorParquetLine(Group grp, int[] sortCols) {
        this.group = grp;
        chr = "chrN";
        pos = 0;
        this.sortCols = sortCols;
    }

    @Override
    public Row rowWithSelectedColumns(int[] columnIndices) {
        String rowstr = IntStream.of(columnIndices).skip(2).mapToObj(i -> group.getValueToString(i-2, 0)).collect(Collectors.joining("\t"));
        return RowObj.apply(rowstr);
    }

    @Override
    public CharSequence getAllCols() {
        StringBuilder sb = new StringBuilder();
        int c = group.getFieldRepetitionCount(0);
        String val = c > 0 ? group.getValueToString(0, 0) : "";
        sb.append(val);
        for( int i = 1; i < numCols(); i++ ) {
            sb.append('\t');
            c = group.getFieldRepetitionCount(i);
            val = c > 0 ? group.getValueToString(i, 0) : "";
            sb.append(val);
        }
        return sb;
    }

    @Override
    public String otherCols() {
        StringBuilder sb = new StringBuilder();
        List<Type> fields = group.getType().getFields();
        if(fields.size() > 0) {
            Type tp = fields.get(0);
            String val = extractGroup(tp, group, 0, 0);
            sb.append(val);
            for (int i = 1; i < numCols(); i++) {
                sb.append('\t');
                tp = fields.get(i);
                val = extractGroup(tp, group, i, 0);
                sb.append(val);
            }
        }
        return sb.toString();
    }

    private int compareGroups(int i, OriginalType t, Group group, Group cgroup) {
        if(t == OriginalType.INT_32) {
            return Integer.compare(group.getInteger(i, 0),cgroup.getInteger(i,0));
        } else if(t == OriginalType.DECIMAL) {
            return Double.compare(group.getDouble(i, 0),cgroup.getDouble(i,0));
        } else {
            String c1 = group.getValueToString(i, 0);
            String c2 = cgroup.getValueToString(i, 0);
            return c1.compareTo(c2);
        }
    }

    @Override
    public int compareTo(Row r) {
        if( sortCols != null ) {
            NorParquetLine nrow = (NorParquetLine)r;
            GroupType gt = group.getType();
            // jdk13: return Arrays.stream(sortCols).map(i -> i-1).map(i -> compareGroups(i, gt.getFields().get(i).getOriginalType(), group, nrow.group)).dropWhile(i -> i == 0).findFirst().orElse(0);
            for(int i : sortCols) {
                int k = compareGroups(i-1, gt.getFields().get(i-1).getOriginalType(), group, nrow.group);
                if( k != 0 ) return k;
            }
            return 0;
        }
        return super.compareTo(r);
    }
}
