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

import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.GroupType;
import org.gorpipe.model.gor.RowObj;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("squid:S2160") // equals is implemented correctly in parent
public class ParquetLine extends Line {
    protected Group group;

    ParquetLine() {}

    public ParquetLine(Group grp, GenomicIterator.ChromoLookup lookup) {
        this.group = grp;
        chr = group.getString(0, 0);
        pos = group.getType().getFields().get(1).asPrimitiveType().getPrimitiveTypeName().equals(PrimitiveType.PrimitiveTypeName.INT64) ? (int)group.getLong(1, 0) : group.getInteger(1, 0);
        if (lookup != null) chrIdx = lookup.chrToId(chr);
    }

    @Override
    public void writeRowToStream(OutputStream outputStream) throws IOException {
        outputStream.write(chr.getBytes());
        outputStream.write('\t');
        outputStream.write(String.valueOf(pos).getBytes());
        for (int i = 2; i < numCols(); i++) {
            outputStream.write('\t');
            Type tp = group.getType().getFields().get(i);
            String val = extractGroup(tp, group, i, 0);
            outputStream.write(val.getBytes());
        }
    }

    @Override
    public String stringValue(int col) {
        return group.getString(col, 0);
    }

    @Override
    public int intValue(int col) {
        return group.getInteger(col, 0);
    }

    @Override
    public long longValue(int col) {
        return group.getLong(col, 0);
    }

    @Override
    public double doubleValue(int col) {
        return group.getDouble(col, 0);
    }

    @Override
    public String toColString() {
        return "";
    }

    @Override
    public int colAsInt(int colNum) {
        return group.getInteger(colNum, 0);
    }

    @Override
    public double colAsDouble(int colNum) {
        return group.getDouble(colNum, 0);
    }

    @Override
    public Long colAsLong(int colNum) {
        return group.getLong(colNum, 0);
    }

    public PrimitiveType.PrimitiveTypeName getType(int colNum) {
        Type tp = group.getType().getType(colNum);
        if (tp.isPrimitive()) return tp.asPrimitiveType().getPrimitiveTypeName();
        return PrimitiveType.PrimitiveTypeName.BINARY;
    }

    String extractGroup(Type tp, Group group, int colNum, int idx) {
        if (tp.isPrimitive()) {
            int size = group.getFieldRepetitionCount(colNum);
            String ret = idx < size ? group.getValueToString(colNum, idx) : "";
            return ret;
        } else {
            Group subgroup = group.getGroup(colNum, idx);
            List<Type> types = subgroup.getType().getFields();
            return IntStream.range(0, types.size()).mapToObj(i -> {
                Type st = types.get(i);
                int repcount = subgroup.getFieldRepetitionCount(i);
                return IntStream.range(0, repcount).mapToObj(k -> extractGroup(st, subgroup, i, k)).collect(Collectors.joining(","));
            }).collect(Collectors.joining(","));
        }
    }

    @Override
    public String colAsString(int colNum) {
        Type tp = group.getType().getFields().get(colNum);
        return extractGroup(tp, group, colNum, 0);
    }

    @Override
    public String otherCols() {
        StringBuilder sb = new StringBuilder();
        List<Type> fields = group.getType().getFields();
        if(fields.size() > 2) {
            Type tp = fields.get(2);
            String val = extractGroup(tp, group, 2, 0);
            sb.append(val);
            for (int i = 3; i < numCols(); i++) {
                sb.append('\t');
                tp = fields.get(i);
                val = extractGroup(tp, group, i, 0);
                sb.append(val);
            }
        }
        return sb.toString();
    }

    @Override
    public Row rowWithAddedColumn(CharSequence s) {
        return RowObj.apply(getAllColumns().append('\t').append(s));
    }

    @Override
    public CharSequence colsSlice(int startCol, int stopCol) {
        if (startCol == stopCol) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            if (startCol == 0) {
                sb.append(chr);
                sb.append("\t");
                sb.append(pos);
                startCol++;
            } else if (startCol == 1) {
                sb.append(pos);
            } else {
                sb.append(colAsString(startCol));
            }
            for (int i = startCol + 1; i < stopCol; i++) {
                sb.append("\t");
                sb.append(colAsString(i));
            }
            return sb;
        }
    }

    private StringBuilder getAllColumns() {
        StringBuilder sb = new StringBuilder();
        sb.append(chr);
        sb.append('\t');
        sb.append(pos);
        for (int i = 2; i < numCols(); i++) {
            sb.append('\t');
            Type tp = group.getType().getFields().get(i);
            String val = extractGroup(tp, group, i, 0);
            sb.append(val);
        }
        return sb;
    }

    @Override
    public CharSequence getAllCols() {
        return getAllColumns();
    }

    @Override
    public String toString() {
        return getAllCols().toString();
    }

    @Override
    public int numCols() {
        return group.getType().getFieldCount();
    }

    @Override
    public int length() {
        return getAllCols().length();
    }

    @Override
    public String selectedColumns(int[] columnIndices) {
        return IntStream.of(columnIndices).mapToObj(this::colAsString).collect(Collectors.joining("\t"));
    }

    @Override
    public int otherColsLength() {
        return otherCols().length();
    }

    @Override
    public void addSingleColumnToRow(String rowString) {
        int colNum = this.numCols();
        this.addColumns(1);
        this.setColumn(colNum-2, rowString);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ParquetLine) {
            ParquetLine pObj = (ParquetLine)obj;
            return group.equals(pObj.group);
        }
        return super.equals(obj);
    }

    @Override
    public Row slicedRow(int startCol, int stopCol) {
        return null;
    }

    @Override
    public Row rowWithSelectedColumns(int[] columnIndices) {
        String rowstr = IntStream.of(columnIndices).mapToObj(i -> group.getValueToString(i, 0)).collect(Collectors.joining("\t"));
        return RowObj.apply(rowstr);
    }

    @Override
    public int sa(int i) {
        return 0;
    }

    @Override
    public void resize(int newsize) {
        throw new UnsupportedOperationException("resize not supported in parquet line");
    }

    @Override
    public void setColumn(int i, String val) {
        Binary bin = Binary.fromString(val);
        group.add(i+2,bin);
    }

    @Override
    public void addColumns(int num) {
        GroupType original = group.getType();
        List<Type> types = original.getFields();
        List<Type> newtypes = new ArrayList<>(types);
        newtypes.add(types.get(0));
        GroupType groupType = new GroupType(original.getRepetition(), original.getName(), newtypes);
        SimpleGroup simpleGroup = new SimpleGroup(groupType);
        for(int i = 0; i < group.getType().getFieldCount(); i++) {
            Type type = group.getType().getType(i);
            if(type.asPrimitiveType().getPrimitiveTypeName().equals(PrimitiveType.PrimitiveTypeName.INT32)) {
                simpleGroup.add(i, group.getInteger(i,0));
            } else if(type.asPrimitiveType().getPrimitiveTypeName().equals(PrimitiveType.PrimitiveTypeName.INT64)) {
                simpleGroup.add(i, group.getInteger(i,0));
            } else if(type.asPrimitiveType().getPrimitiveTypeName().equals(PrimitiveType.PrimitiveTypeName.DOUBLE)) {
                simpleGroup.add(i, group.getDouble(i,0));
            } else if(type.asPrimitiveType().getPrimitiveTypeName().equals(PrimitiveType.PrimitiveTypeName.BINARY)) {
                simpleGroup.add(i, group.getBinary(i,0));
            }
        }
        group = simpleGroup;
    }

}
