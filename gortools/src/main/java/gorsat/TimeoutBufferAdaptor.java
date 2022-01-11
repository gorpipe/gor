package gorsat;

import org.gorpipe.gor.model.Row;

public class TimeoutBufferAdaptor extends BufferAdaptor {
    long timeTriggerBufferFlushNs;

    public TimeoutBufferAdaptor(ReaderThread readerThread, long timeTriggerBufferFlushNs) {
        super(readerThread);
        this.timeTriggerBufferFlushNs = timeTriggerBufferFlushNs;
    }

    public void updateTimeMeasurement(long deltaTimeNs, RowBuffer current) {
        ++avgCount;
        numberOfRowsRead += current.size();
        totalTimeNs += deltaTimeNs;
        avgBatchSize = ((avgCount - 1) * avgBatchSize + current.size()) / avgCount;
        avgRowsPerMilliSecond = numberOfRowsRead / (totalTimeNs / 1000000.0);
        Row firstRow = current.get(0);
        Row lastRow = current.get(current.size() - 1);
        if (firstRow.chr.equals(lastRow.chr)) {
            ++bavgCount;
            avgBasesPerMilliSecond = ((bavgCount - 1) * avgBasesPerMilliSecond + (lastRow.pos - firstRow.pos) / (deltaTimeNs / 1000000.0)) / bavgCount;
        }
    }

    @Override
    public void process(Row r) {
        try {
            if (!this.wantsNoMore()) {
                current.add(r);
                if (current.isFull()) {
                    long nt = System.nanoTime();
                    if (nt - t > timeTriggerBufferFlushNs) {
                        if (readerThread.offer(current)) {
                            updateTimeMeasurement(nt - t, current);
                            current = current.nextRowBuffer();
                            current.reduce(current.size() / 2);
                            t = System.nanoTime();
                        } else if (!current.enlarge(current.size() * 2)) {
                            updateTimeMeasurement(nt - t, current);
                            readerThread.offerBatch(current);
                            current = current.nextRowBuffer();
                            t = System.nanoTime();
                        }
                    } else if (!current.enlarge(current.size() * 8)) {
                        updateTimeMeasurement(nt - t, current);
                        readerThread.offerBatch(current);
                        current = current.nextRowBuffer();
                        t = System.nanoTime();
                    }
                }
            } else {
                readerThread.stopProcessing("Stop processing adaptor wantsNoMore");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

