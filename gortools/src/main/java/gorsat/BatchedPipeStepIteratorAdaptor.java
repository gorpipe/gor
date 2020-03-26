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

import org.gorpipe.model.genome.files.gor.Row;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import gorsat.Commands.Analysis;
import org.gorpipe.model.gor.RowObj;
import org.gorpipe.model.gor.iterators.RowSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by sigmar on 05/05/2017.
 */
public class BatchedPipeStepIteratorAdaptor extends RowSource implements Spliterator<Row> {
    private static final Logger log = LoggerFactory.getLogger(BatchedPipeStepIteratorAdaptor.class);

    private Iterator<? extends Row> sourceIterator;
    private Analysis pipeStep;
    private RowBuffer rowBuffer = null;
    private Row endRow = RowObj.StoR("chrN\t-1");
    private final Duration timeTriggerBufferFlush;
    private final Duration batchOfferTimeout;
    private final Duration timeout;
    private final Duration logInterval;
    private ReaderThread readerThread;
    private boolean throwOnExit = true;

    private double avgSeekTimeMilliSecond = 0.0;
    private double avgBasesPerMilliSecond = 0.0;
    private double avgRowsPerMilliSecond = 0.0;
    private double avgBatchSize = 0.0;
    private int numberOfRowsRead = 0;
    private long totalTimeNs = 0;
    private int avgCount = 0;
    private int bavgCount = 0;

    private long est = Long.MAX_VALUE;
    private boolean nosplit = false;
    private String currentChrom;
    private static Map<String,String> nextChromMap = new HashMap<>();

    private BatchedReadSourceConfig brsConfig;
    private boolean autoclose;

    public void setCurrentChrom(String chrom) {
        this.currentChrom = chrom;
    }

    static {
        String[] chroms = {"chr1","chr10","chr11","chr12","chr13","chr14","chr15","chr16","chr17","chr18","chr19","chr20","chr21","chr22","chrM","chrX","chrY"};
        IntStream.range(0,chroms.length-1).forEach(i -> nextChromMap.put(chroms[i],chroms[i+1]));
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

    public Stream<Row> getStream() {
        return getStream(false);
    }

    public Stream<Row> getStream(boolean parallel) {
        Stream<Row> ret = StreamSupport.stream(this, parallel);
        ret.onClose(this::close);
        return ret;
    }

    @Override
    public Comparator<Row> getComparator() {
        return null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Row> action) {
        if( hasNext() ) {
            action.accept(next());
            return true;
        }
        return false;
    }

    @Override
    public BatchedPipeStepIteratorAdaptor clone() {
        RowSource cl = ((RowSource) sourceIterator).clone();
        return new BatchedPipeStepIteratorAdaptor(cl, pipeStep.clone(), getHeader(), brsConfig);
    }

    @Override
    public Spliterator<Row> trySplit() {
        if( !nosplit && sourceIterator != null && nextChromMap.containsKey(currentChrom) ) {
            String nextChrom = nextChromMap.get(currentChrom);
            BatchedPipeStepIteratorAdaptor splitbpia = clone();
            splitbpia.setPosition(currentChrom, 0);
            currentChrom = nextChrom;
            return splitbpia;
        }

        if( !nosplit ) {
            this.setPosition(currentChrom, 0);
            nosplit = true;
        }

        return null;
    }

    @Override
    public long estimateSize() {
        return est;
    }

    @Override
    public int characteristics() {
        return ORDERED | SORTED | NONNULL | IMMUTABLE;
    }

    private class TimeoutBufferAdaptor extends BufferAdaptor {
        long timeTriggerBufferFlushNs;

        public TimeoutBufferAdaptor(ReaderThread readerThread) {
            super(readerThread);
            timeTriggerBufferFlushNs = timeTriggerBufferFlush.getNano();
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

    private class SpliteratorAdaptor extends Analysis implements Consumer<Row> {
        Consumer<? super Row> cns;

        SpliteratorAdaptor(Consumer<? super Row> cns) {
            this.cns = cns;
        }

        @Override
        public void process(Row r) {
            if (nosplit && !currentChrom.equals(r.chr)) reportWantsNoMore();
            else cns.accept(r);
        }

        @Override
        public void accept(Row row) {
            process(row);
        }

        @Override
        public void finish() {
            super.finish();
        }
    }

    private class BufferAdaptor extends Analysis {
        RowBuffer current;
        ReaderThread readerThread;
        long t;

        public BufferAdaptor(ReaderThread readerThread) {
            setReaderThread(readerThread);
            current = readerThread.rowBuffer1;
            t = System.nanoTime();
        }

        public void setReaderThread(ReaderThread rt) {
            readerThread = rt;
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

    /**
     * Reads from the sourceIterator into a buffer
     */
    private class ReaderThread extends Thread {
        private SynchronousQueue<RowBuffer> rowQueue = new SynchronousQueue<>();
        private RowBuffer rowBuffer1 = new RowBuffer();
        private RowBuffer rowBuffer2 = new RowBuffer(rowBuffer1);
        private Analysis bufferedPipeStep;
        private BufferAdaptor bufferAdaptor;
        private boolean stopProcessing = false;
        private boolean didStart = false;

        private long numberOfPollsBeforeLog;
        private long numberOfPollsBeforeTimeout;

        public ReaderThread( Analysis pipeStep, BufferAdaptor bufferAdaptor) {
            this.bufferAdaptor = bufferAdaptor;
            bufferAdaptor.setReaderThread(this);
            bufferedPipeStep = pipeStep;
            bufferedPipeStep.wantsNoMore_$eq(false);
            init();
        }

        public ReaderThread() {
            initPipeStep();
            init();
        }

        private void init() {
            this.setName(Thread.currentThread().getName() + "::ReaderThread");
            rowBuffer1.setNextRowBuffer(rowBuffer2);
            numberOfPollsBeforeLog = logInterval.toMillis() / batchOfferTimeout.toMillis();
            numberOfPollsBeforeTimeout = timeout.toMillis() / batchOfferTimeout.toMillis();
        }

        private void initPipeStep() {
            if (timeTriggerBufferFlush.getNano() < 0) {
                bufferAdaptor = new BufferAdaptor(this);
                bufferedPipeStep = pipeStep != null ? pipeStep.$bar(bufferAdaptor) : bufferAdaptor;
            } else {
                bufferAdaptor = new TimeoutBufferAdaptor(this);
                bufferedPipeStep = pipeStep != null ? pipeStep.$bar(bufferAdaptor) : bufferAdaptor;
            }
            bufferedPipeStep.securedSetup(null);
        }

        public void stopProcessing( String message ) {
            log.debug(message);
            stopProcessing = true;
            if (bufferedPipeStep != null) bufferedPipeStep.wantsNoMore_$eq(true);
        }

        public void finish() {
            try {
                bufferedPipeStep.securedFinish(getEx());
            } catch (Throwable e) {
                setEx(e);
                stopProcessing("Stop processing error in finish " + e.getMessage());
            }
        }

        public void run() {
            didStart = true;
            Row r = null;
            try {
                while (sourceIterator.hasNext() && !bufferedPipeStep.wantsNoMore()) {
                    r = sourceIterator.next();
                    bufferedPipeStep.process(r);
                }
            } catch (Throwable e) {
                setEx(e);
                stopProcessing("Stop processinng cause error " + e.getMessage() + " last row " + r);
            } finally {
                finish();
                // we don't want to stop processing even though sourceIterator has been read
                closeSourceIterator();
            }
        }

        public boolean offer(RowBuffer rowBuffer) {
            return rowQueue.offer(rowBuffer);
        }

        public RowBuffer poll() {
            return rowQueue.poll();
        }

        public void offerBatch(RowBuffer current) throws InterruptedException {
            int count = 0;
            while (!stopProcessing && !rowQueue.offer(current, batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                if (count > numberOfPollsBeforeTimeout) {
                    throw new GorSystemException("BatchedIteratorAdaptor polling for too long " + timeout.getSeconds(), null);
                }
                if (count++ % numberOfPollsBeforeLog == 0)
                    log.debug("Offering batch for {}, batch size {}, query {}", numberOfPollsBeforeLog * count, current.size(), BatchedPipeStepIteratorAdaptor.this);
            }
        }

        public RowBuffer pollBatch() throws InterruptedException {
            RowBuffer ret = rowQueue.poll(batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
            int count = 0;
            while (!stopProcessing && ret == null) {
                ret = rowQueue.poll(batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS);
                if (count > numberOfPollsBeforeTimeout) {
                    throw new GorSystemException("BatchedIteratorAdaptor polling for too long " + timeout.getSeconds(), null);
                }
                if (count++ % numberOfPollsBeforeLog == 0)
                    log.debug("Polling batch for {} time {}, query {}", numberOfPollsBeforeLog * count, Thread.currentThread().getId(), BatchedPipeStepIteratorAdaptor.this);
            }
            return ret;
        }
    }

    @Override
    public String toString() {
        return pipeStep == null ? sourceIterator.toString()  : sourceIterator + " | " + pipeStep;
    }

    public BatchedPipeStepIteratorAdaptor(Iterator<? extends Row> sourceIterator, gorsat.Commands.Analysis pipeStep, String theHeader, BatchedReadSourceConfig brsConfig) {
        this(sourceIterator, pipeStep, false, theHeader, brsConfig);
    }

    public BatchedPipeStepIteratorAdaptor(Iterator<? extends Row> sourceIterator, gorsat.Commands.Analysis pipeStep, boolean autoclose, String theHeader, BatchedReadSourceConfig brsConfig) {
        this.sourceIterator = sourceIterator;
        this.pipeStep = pipeStep;
        this.brsConfig = brsConfig;
        this.autoclose = autoclose;
        setHeader(theHeader);

        timeTriggerBufferFlush = brsConfig.getBufferFlushTimout();
        batchOfferTimeout = brsConfig.getBatchOfferTimeout();

        timeout = Duration.ofSeconds(Long.parseLong(System.getProperty("gor.timeout.rowsource", "1800000")));
        logInterval = brsConfig.getLogInterval();
    }

    @Override
    public boolean hasNext() {
        try {
            if (rowBuffer == null) {
                readerThread = new ReaderThread();
                readerThread.setUncaughtExceptionHandler((tt, e) -> {
                    // THis is just so that the default handler does not write to std.err
                });
                readerThread.start();
                rowBuffer = readerThread.pollBatch();
            } else if (!rowBuffer.available()) {
                rowBuffer = readerThread.pollBatch();
            }

            Throwable exception = getEx();
            if (exception != null) {
                throwOnExit = false;
                if (exception instanceof GorException) {
                    throw (GorException)exception;
                } else {
                    throw new GorSystemException(exception);
                }
            }
            boolean ret = rowBuffer != null && rowBuffer.hasNext();
            if( !ret && autoclose ) {
                close();
            }
            return ret;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public Row next() {
        return rowBuffer.next();
    }

    @Override
    public void remove() {}

    @Override
    public void forEachRemaining(Consumer<? super Row> action) {
        SpliteratorAdaptor spliteratorAdaptor = new SpliteratorAdaptor(action);
        Analysis bufferedPipeStep = pipeStep != null ? pipeStep.$bar(spliteratorAdaptor) : spliteratorAdaptor;
        Exception ex = null;
        bufferedPipeStep.securedSetup(ex);
        try {
            while (sourceIterator.hasNext() && !bufferedPipeStep.wantsNoMore()) {
                Row r = sourceIterator.next();
                bufferedPipeStep.process(r);
            }
        } catch(Exception e) {
            ex = e;
        } finally {
            bufferedPipeStep.securedFinish(ex);
        }
        close();
    }

    int seekCount = 0;

    @Override
    public void setPosition(String seekChr, int seekPos) {
        long t = System.nanoTime();
        try {
            if (readerThread != null) {
                readerThread.stopProcessing( "Stop processing seeking to " + seekChr + " " + seekPos );
                readerThread.poll();
                readerThread.join();
                if (sourceIterator instanceof RowSource) {
                    ((RowSource) sourceIterator).setPosition(seekChr, seekPos);
                } else {
                    Row next;
                    while( sourceIterator.hasNext() && ((next = sourceIterator.next()).chr.compareTo(seekChr) < 0 || (next.chr.compareTo(seekChr) == 0 && next.pos < seekPos)) );
                }
                readerThread = new ReaderThread( readerThread.bufferedPipeStep, readerThread.bufferAdaptor);
                readerThread.setUncaughtExceptionHandler((tt, e) -> {
                    // THis is just so that the default handler does not write to std.err
                });
                rowBuffer = readerThread.rowBuffer1;
                readerThread.start();
            }
        } catch (InterruptedException e) {
            throw new GorSystemException("rowQueue take interrupted on setPosition", e);
        }
        avgSeekTimeMilliSecond = ((seekCount * avgSeekTimeMilliSecond + (System.nanoTime() - t) / 1000000.0) / (seekCount + 1));
        seekCount++;
    }

    private void closeSourceIterator() {
        if (sourceIterator instanceof RowSource) ((RowSource)sourceIterator).close();
    }

    @Override
    public void close() {
        if (readerThread != null && readerThread.didStart) {
            readerThread.stopProcessing("Stop processing closing source");
            try {
                readerThread.join(2000);
                closeSourceIterator();
            } catch (InterruptedException ie) {
                log.warn("Reader thread join interrupted");
                Thread.currentThread().interrupt();
            }
        } else {
            closeSourceIterator();
        }

        if (throwOnExit) {
            Throwable exception = getEx();

            if (exception != null) {
                if (exception instanceof GorException) {
                    throw (GorException) exception;
                } else {
                    throw new GorSystemException("Got exception in bufferedPipeStep process thread", exception);
                }
            }
        }
    }

    @Override
    public boolean isBuffered() {
        return true;
    }
}
