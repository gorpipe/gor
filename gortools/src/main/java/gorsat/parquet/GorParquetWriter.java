package gorsat.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.gorpipe.model.genome.files.gor.Row;

import java.io.IOException;

/**
 * Writes parquet file from a stream of gor/nor rows
 */
public class GorParquetWriter extends ParquetWriter<Row> {

    public GorParquetWriter(Path file, WriteSupport<Row> writeSupport, CompressionCodecName compressionCodecName, int blockSize, int pageSize) throws IOException {
        super(file, writeSupport, compressionCodecName, blockSize, pageSize);
    }

    public static GorParquetBuilder builder(Path file) {
        return new GorParquetBuilder(file);
    }

    public static class GorParquetBuilder extends ParquetWriter.Builder<Row, GorParquetBuilder> {
        private GorParquetBuilder(Path file) {
            super(file);
        }

        @Override
        protected GorParquetBuilder self() {
            return this;
        }

        @Override
        protected WriteSupport<Row> getWriteSupport(Configuration conf) {
            return new GorParquetWriteSupport();
        }
    }
}
