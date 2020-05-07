package gorsat.parquet;

import gorsat.Commands.Output;
import gorsat.RowBuffer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.function.GorRowInferFunction;
import org.gorpipe.model.genome.files.gor.Row;

import java.io.IOException;

public class GorParquetFileOut extends Output {
    ParquetWriter<Row> parquetWriter;
    Configuration conf;
    Path path;
    String header;
    GorRowInferFunction gorRowInferFunction;
    RowBuffer rowBuffer;
    boolean nor;

    public GorParquetFileOut(String filePath, String header, boolean nor) {
        conf = new Configuration();
        path = new Path(filePath);
        this.header = header;
        this.nor = nor;
    }

    @Override
    public void setup() {
        gorRowInferFunction = new GorRowInferFunction();
        rowBuffer = new RowBuffer(1000);
    }

    private void flushTypeInferBuffer() throws IOException {
        if(rowBuffer.available()) {
            Row row = rowBuffer.get(0);
            Row prev = row;
            boolean ordered = !row.chr.equals("chrN") && row.pos != 0;
            if(rowBuffer.size()==1) row = gorRowInferFunction.apply(row, row);
            else for(int i = 1; i < rowBuffer.size(); i++) {
                Row next = rowBuffer.get(i);
                ordered &=  prev.atPriorPos(next) || prev.atSamePos(next);
                prev = next;
                row = gorRowInferFunction.apply(row, next);
            }
            conf.setBoolean("ordered",ordered);
            conf.setBoolean("nor", nor);
            conf.set("header",header);
            conf.set("schema",row.toString());
            parquetWriter = GorParquetWriter.builder(path).withConf(conf).withWriteMode(ParquetFileWriter.Mode.OVERWRITE).build();
            while(rowBuffer.hasNext()) parquetWriter.write(rowBuffer.next());
        }
    }

    @Override
    public void process(Row r) {
        if(!rowBuffer.isFull()) rowBuffer.add(r);
        else {
            try {
                flushTypeInferBuffer();
                parquetWriter.write(r);
            } catch (IOException e) {
                throw new GorSystemException("Error in parquet file out process", e);
            }
        }
    }

    @Override
    public void finish() {
        try {
            flushTypeInferBuffer();
            parquetWriter.close();
        } catch (IOException e) {
            throw new GorSystemException("Error in parquet file out finish", e);
        }
    }
}
