package gorsat;

import gorsat.Commands.Analysis;
import org.gorpipe.gor.binsearch.RowBuffer;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;

public class BufferAdaptor extends Analysis {
    private final Row endRow = RowObj.StoR("chrN\t-1");

    RowBuffer current;
    ReaderThread readerThread;
    long t;

    double avgSeekTimeMilliSecond = 0.0;
    double avgBasesPerMilliSecond = 0.0;
    double avgRowsPerMilliSecond = 0.0;
    double avgBatchSize = 0.0;

    int numberOfRowsRead = 0;
    long totalTimeNs = 0;
    int avgCount = 0;
    int bavgCount = 0;

    public BufferAdaptor(ReaderThread readerThread) {
        setReaderThread(readerThread);
        t = System.nanoTime();
    }

    public void setReaderThread(ReaderThread rt) {
        readerThread = rt;
        current = readerThread.rowBuffer1;
    }

    @Override
    public void process(Row r) {
        try {
            if (!this.wantsNoMore()) {
                current.add(r);
                if (current.isFull()) {
                    if (readerThread.offer(current)) {
                        current = current.nextRowBuffer();
                        current.reduce(current.size() / 2);
                    } else if (!current.enlarge(current.size() * 8)) {
                        readerThread.offerBatch(current);
                        current = current.nextRowBuffer();
                    }
                }
            } else {
                readerThread.stopProcessing("Stop processing adaptor wantsNoMore");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void finish() {
        try {
            if( !isInErrorState() ) {
                if (current.isFull()) {
                    readerThread.offerBatch(current);
                    current = current.nextRowBuffer();
                }
                current.add(endRow);
                readerThread.offerBatch(current);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
