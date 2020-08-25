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

package gorsat;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.monitor.GorMonitor;
import org.gorpipe.gor.model.Row;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RowSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper rowSource reading the child rowSource in a thread into a buffer and offering to the main thread in batches
 * <p>
 * Created by sigmar on 21/11/2016.
 */
public class BatchedReadSource extends RowSource {
    private static final Logger log = LoggerFactory.getLogger(BatchedReadSource.class);

    private final Row endRow = RowObj.StoR("chrN\t-1");
    private final Iterator<? extends Row> sourceIterator;
    private PollingThread readerThread;
    private final Duration timeTriggerBufferFlush;
    private final Duration batchOfferTimeout;
    private final Duration timeout;
    private final Duration logInterval;

    private double avgSeekTimeMilliSecond = 0.0;
    private double avgBasesPerMilliSecond = 0.0;
    private double avgRowsPerMilliSecond = 0.0;
    private double avgBatchSize = 0.0;
    private int numberOfRowsRead = 0;
    private long totalTimeNs = 0;
    private int avgCount;
    private int bavgCount = 0;

    private final GorMonitor gorMonitor;

    public void updateTimeMeasurement(long deltaTimeNs, RowBuffer current) {
        ++avgCount;
        numberOfRowsRead += current.size();
        totalTimeNs += deltaTimeNs;
        avgBatchSize = ((avgCount - 1) * avgBatchSize + current.size()) / avgCount;
        if (totalTimeNs != 0.0) avgRowsPerMilliSecond = numberOfRowsRead / (totalTimeNs / 1000000.0);
        Row firstRow = current.get(0);
        Row lastRow = current.get(current.size() - 1);
        if (firstRow.chr.equals(lastRow.chr)) {
            ++bavgCount;
            if (deltaTimeNs != 0.0)
                avgBasesPerMilliSecond = ((bavgCount - 1) * avgBasesPerMilliSecond + (lastRow.pos - firstRow.pos) / (deltaTimeNs / 1000000.0)) / bavgCount;
        }
    }

    private class PollingThread extends Thread {
        SynchronousQueue<RowBuffer> rowQueue = new SynchronousQueue<>();
        RowBuffer rowBuffer1 = new RowBuffer();
        RowBuffer rowBuffer2 = new RowBuffer(rowBuffer1);
        boolean stopProcessingThread = false;
        boolean didStart = false;

        long numberOfPollsBeforeLog;
        long numberOfPollsBeforeTimeout;

        PollingThread() {
            rowBuffer1.setNextRowBuffer(rowBuffer2);

            numberOfPollsBeforeLog = logInterval.toMillis() / batchOfferTimeout.toMillis();
            numberOfPollsBeforeTimeout = timeout.toMillis() / batchOfferTimeout.toMillis();
        }

        void stopProcessing() {
            stopProcessingThread = true;
        }

        RowBuffer poll() {
            return rowQueue.poll();
        }

        public void run() {
            didStart = true;
        }

        void offerBatch(RowBuffer current, Duration batchOfferTimeout) throws InterruptedException {
            int count = 0;
            while (!stopProcessingThread && !rowQueue.offer(current, batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                if (count++ % numberOfPollsBeforeLog == 0) {
                    log.debug("Offering batch for for" + batchOfferTimeout.getSeconds() * count + ", batchsize " + current.size() + " threadid: " + Thread.currentThread().getId());
                }
            }
        }

        int lastCount = 0;
        int pollCount = 0;
        RowBuffer pollBatch() throws InterruptedException {
            RowBuffer rowBuffer = rowQueue.poll(batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
            int count = 0;
            while (!stopProcessingThread && rowBuffer == null) {
                if (count > numberOfPollsBeforeTimeout) {
                    throw new RuntimeException("BatchedReadSource polling for too long " + timeout.getSeconds());
                }
                if (count++ % numberOfPollsBeforeLog == 0) {
                    log.debug("BatchedReadSource polling for" + batchOfferTimeout.getSeconds() * count + ", threadid: " + Thread.currentThread().getId());
                }
                rowBuffer = rowQueue.poll(batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            pollCount += count+1;

            if( gorMonitor != null && rowBuffer != null && pollCount-lastCount > 200 ) {
                if (isCancelled()) {
                    stopProcessing();
                }
                lastCount = pollCount;
            }

            return rowBuffer;
        }
    }

    private class ReaderThread extends PollingThread {
        ReaderThread() {
            super();
        }

        @Override
        public void run() {
            super.run();
            try {
                RowBuffer current = rowBuffer1;
                if (timeTriggerBufferFlush.getNano() < 0) {
                    while (!stopProcessingThread && sourceIterator.hasNext()) {
                        Row next = sourceIterator.next();
                        current.add(next);
                        if (current.isFull()) {
                            if (rowQueue.offer(current)) {
                                current = current.nextRowBuffer();
                                current.reduce(current.getCapacity() / 2);
                            } else if (!current.enlarge(current.getCapacity() * 8)) {
                                offerBatch(current, batchOfferTimeout);
                                current = current.nextRowBuffer();
                            }
                        }
                    }
                } else {
                    long t = System.nanoTime();
                    long timeTriggerBufferFlushNs = timeTriggerBufferFlush.getNano();
                    while (!stopProcessingThread && sourceIterator.hasNext()) {
                        Row next = sourceIterator.next();
                        current.add(next);
                        if (current.isFull()) {
                            long nt = System.nanoTime();
                            if (nt - t > timeTriggerBufferFlushNs) {
                                if (rowQueue.offer(current)) {
                                    updateTimeMeasurement(nt - t, current);
                                    current = current.nextRowBuffer();
                                    current.reduce(current.getCapacity() / 2);
                                    t = System.nanoTime();
                                } else if (!current.enlarge(current.getCapacity() * 2)) {
                                    updateTimeMeasurement(nt - t, current);
                                    offerBatch(current, batchOfferTimeout);
                                    current = current.nextRowBuffer();
                                    t = System.nanoTime();
                                }
                            } else if (!current.enlarge(current.getCapacity() * 8)) {
                                updateTimeMeasurement(nt - t, current);
                                offerBatch(current, batchOfferTimeout);
                                current = current.nextRowBuffer();
                                t = System.nanoTime();
                            }
                        }
                    }
                }
                if (current.isFull()) {
                    offerBatch(current, batchOfferTimeout);
                    current = current.nextRowBuffer();
                }
                current.add(endRow);
                offerBatch(current, batchOfferTimeout);
            } catch (InterruptedException e) {
                log.error("rowQueue put interrupted", e);
                setEx(e);
            } catch (Throwable e) {
                setEx(e);
                stopProcessing();
            } finally {
                closeSourceIterator();
            }
        }
    }

    public BatchedReadSource(RowSource sourceIterator, BatchedReadSourceConfig brsConfig) {
        this(sourceIterator,brsConfig,null,null);
    }

    public BatchedReadSource(Iterator<? extends Row> sourceIterator, BatchedReadSourceConfig brsConfig, String header, GorMonitor gorMonitor) {
        this.sourceIterator = sourceIterator;
        this.gorMonitor = gorMonitor;
        this.setHeader(header);
        timeTriggerBufferFlush = brsConfig.getBufferFlushTimout();
        batchOfferTimeout = brsConfig.getBatchOfferTimeout();
        timeout = Duration.ofSeconds(Long.parseLong(System.getProperty("gor.timeout.rowsource", "1800000")));
        logInterval = brsConfig.getLogInterval();
        this.setHeader(header);
    }

    @Override
    public double getAvgRowsPerMilliSecond() {
        return avgRowsPerMilliSecond;
    }

    @Override
    public double getAvgBasesPerMilliSecond() {
        return avgBasesPerMilliSecond;
    }

    @Override
    public double getAvgSeekTimeMilliSecond() { return avgSeekTimeMilliSecond; }

    @Override
    public double getAvgBatchSize() {
        return avgBatchSize;
    }

    @Override
    public int getCurrentBatchSize() {
        return rowBuffer.size();
    }

    @Override
    public int getCurrentBatchLoc() {
        return rowBuffer.getIndex();
    }

    @Override
    public Row getCurrentBatchRow(int i) {
        return rowBuffer.get(i);
    }

    private RowBuffer rowBuffer = null;

    /**
     * @return true if rowSource has more rows
     */
    @Override
    public boolean hasNext() {
        try {
            if (rowBuffer == null) {
                readerThread = new ReaderThread();
                rowBuffer = readerThread.rowBuffer1;
                readerThread.start();
                rowBuffer = readerThread.pollBatch();
            } else if (!rowBuffer.available()) {
                rowBuffer = readerThread.pollBatch();
            }
            Throwable exception = getEx();
            if (exception != null) {
                if (exception instanceof GorException) {
                    throw (GorException)exception;
                } else {
                    throw new GorSystemException(exception);
                }
            }
            return rowBuffer != null && rowBuffer.hasNext();
        } catch (InterruptedException e) {
            throw new GorSystemException("rowQueue take interrupted", e);
        }
    }

    /**
     * Make sure hasNext is called before this method
     *
     * @return the next row in the iterator
     */
    @Override
    public Row next() {
        return rowBuffer.next();
    }

    int seekCount;

    @Override
    public void setPosition(String seekChr, int seekPos) {
        long t = System.nanoTime();
        try {
            if( sourceIterator instanceof RowSource ) ((RowSource)sourceIterator).setPosition(seekChr, seekPos);
            if (readerThread != null) {
                readerThread.stopProcessing();
                readerThread.poll();
                readerThread.join();
                readerThread = null;
            }
        } catch (InterruptedException e) {
            throw new GorSystemException("rowQueue take interrupted on setPosition", e);
        }
        avgSeekTimeMilliSecond = ((seekCount * avgSeekTimeMilliSecond + (System.nanoTime() - t) / 1000000.0) / (seekCount + 1));
        seekCount++;
    }

    public boolean isCancelled() {
        return gorMonitor.isCancelled();
    }

    private void closeSourceIterator() {
        if (sourceIterator instanceof RowSource) ((RowSource) sourceIterator).close();
    }

    @Override
    public void close() {
        if (readerThread != null && readerThread.didStart) {
            readerThread.stopProcessing();
        } else {
            closeSourceIterator();
        }

        Throwable exception = getEx();

        if (exception != null) {
            if (exception instanceof GorException) {
                throw (GorException)exception;
            } else {
                throw new GorSystemException(exception);
            }
        }
    }

    @Override
    public boolean isBuffered() {
        return true;
    }
}
