package gorsat.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.gorpipe.model.genome.files.gor.Row;

import java.util.*;

/**
 * Translates a gor/nor row to corresponding format in a parquet file
 */
public class GorParquetWriteSupport extends WriteSupport<Row> {
    RecordConsumer currentRecordConsumer;
    List<Type> types;
    boolean ordered;
    boolean nor;
    String[] schemaSplit;

    @Override
    public WriteContext init(Configuration configuration) {
        types = new ArrayList<>();
        ordered = configuration.getBoolean("ordered", false);
        nor = configuration.getBoolean("nor", false);
        String header = configuration.get("header");
        String schema = configuration.get("schema");
        String[] headerSplit = header.split("\t");
        schemaSplit = schema.split("\t");
        int start = nor ? 2 : 0;
        for(int i = start; i < headerSplit.length; i++) {
            String colName = headerSplit[i];
            String typeName = schemaSplit[i];
            if(typeName.equals("I")) {
                types.add(Types.primitive(PrimitiveType.PrimitiveTypeName.INT32, Type.Repetition.REQUIRED).named(colName));
            } else if(typeName.equals("D")) {
                types.add(Types.primitive(PrimitiveType.PrimitiveTypeName.DOUBLE, Type.Repetition.REQUIRED).named(colName));
            } else if(typeName.equals("L")) {
                types.add(Types.primitive(PrimitiveType.PrimitiveTypeName.INT64, Type.Repetition.REQUIRED).named(colName));
            } else {
                types.add(Types.primitive(PrimitiveType.PrimitiveTypeName.BINARY, Type.Repetition.REQUIRED).named(colName));
            }
        }
        if(nor) schemaSplit = Arrays.copyOfRange(schemaSplit,2,schemaSplit.length);
        MessageType msgType = new MessageType(nor?"nor":"gor", types);
        Map<String,String> meta = new HashMap<>();
        return new WriteContext(msgType, meta);
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        currentRecordConsumer = recordConsumer;
    }

    @Override
    public void write(Row row) {
        currentRecordConsumer.startMessage();
        int start = 0;
        if(!nor) {
            currentRecordConsumer.startField(types.get(0).getName(), 0);
            Binary chr = Binary.fromCharSequence(row.chr);
            currentRecordConsumer.addBinary(chr);
            currentRecordConsumer.endField(types.get(0).getName(), 0);

            currentRecordConsumer.startField(types.get(1).getName(), 1);
            currentRecordConsumer.addInteger(row.pos);
            currentRecordConsumer.endField(types.get(1).getName(), 1);

            start = 2;
        }
        for(int i = start; i < types.size(); i++) {
            currentRecordConsumer.startField(types.get(i).getName(), i);
            int offset = i+2-start;
            if(schemaSplit[i].equals("I")) currentRecordConsumer.addInteger(row.colAsInt(offset));
            else if(schemaSplit[i].equals("L")) currentRecordConsumer.addLong(row.colAsLong(offset));
            else if(schemaSplit[i].equals("D")) currentRecordConsumer.addDouble(row.colAsDouble(offset));
            else {
                Binary bin = Binary.fromCharSequence(row.colAsString(offset));
                currentRecordConsumer.addBinary(bin);
            }
            currentRecordConsumer.endField(types.get(i).getName(), 0);
        }
        currentRecordConsumer.endMessage();
    }
}
