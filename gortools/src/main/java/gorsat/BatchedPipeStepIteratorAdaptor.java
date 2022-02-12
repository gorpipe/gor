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

import gorsat.Commands.Analysis;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.binsearch.RowBuffer;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.model.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by sigmar on 05/05/2017.
 */
public class BatchedPipeStepIteratorAdaptor extends GenomicIteratorBase implements Spliterator<Row> {
    private static final Logger log = LoggerFactory.getLogger(BatchedPipeStepIteratorAdaptor.class);

    final Iterator<? extends Row> sourceIterator;
    private final Analysis pipeStep;
    private RowBuffer rowBuffer = null;
    private ReaderThread readerThread;
    private boolean throwOnExit = true;

    boolean nosplit = false;
    String currentChrom;
    private static final Map<String,String> nextChromMap = new HashMap<>();

    private final BatchedReadSourceConfig brsConfig;
    private final boolean autoclose;

    private Throwable ex = null;
    public void setEx(Throwable throwable) {
        if (ex == null || throwable == null) {
            ex = throwable;
        }
    }

    public Throwable getEx() {
        return ex;
    }

    public void setCurrentChrom(String chrom) {
        this.currentChrom = chrom;
    }

    static {
        String[] chroms = {"chr1","chr10","chr11","chr12","chr13","chr14","chr15","chr16","chr17","chr18","chr19","chr20","chr21","chr22","chrM","chrX","chrY"};
        IntStream.range(0,chroms.length-1).forEach(i -> nextChromMap.put(chroms[i],chroms[i+1]));
    }

    @Override
    public double getAvgRowsPerMilliSecond() {
        return readerThread.getAvgRowsPerMilliSecond();
    }

    @Override
    public double getAvgBasesPerMilliSecond() {
        return readerThread.getAvgBasesPerMilliSecond();
    }

    @Override
    public double getAvgBatchSize() {
        return readerThread.getAvgBatchSize();
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
    public BatchedPipeStepIteratorAdaptor clone() throws CloneNotSupportedException {
        GenomicIterator cl = ((GenomicIteratorBase) sourceIterator).clone();
        return new BatchedPipeStepIteratorAdaptor(sourceIterator, pipeStep.clone(), getHeader(), brsConfig);
    }

    @Override
    public Spliterator<Row> trySplit() {
        if( !nosplit && sourceIterator != null && nextChromMap.containsKey(currentChrom) ) {
            String nextChrom = nextChromMap.get(currentChrom);
            BatchedPipeStepIteratorAdaptor splitbpia;
            try {
                splitbpia = clone();
            } catch (CloneNotSupportedException e) {
                throw new GorSystemException("Trying to clone non clonable object", e);
            }
            splitbpia.seek(currentChrom, 0);
            currentChrom = nextChrom;
            return splitbpia;
        }

        if( !nosplit ) {
            this.seek(currentChrom, 0);
            nosplit = true;
        }

        return null;
    }

    @Override
    public long estimateSize() {
        long est = Long.MAX_VALUE;
        return est;
    }

    @Override
    public int characteristics() {
        return ORDERED | SORTED | NONNULL | IMMUTABLE;
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
    }

    @Override
    public boolean hasNext() {
        try {
            if (rowBuffer == null) {
                readerThread = new ReaderThread(brsConfig, this, pipeStep);
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
        SpliteratorAdaptor spliteratorAdaptor = new SpliteratorAdaptor(this,action);
        Analysis bufferedPipeStep = pipeStep != null ? pipeStep.$bar(spliteratorAdaptor) : spliteratorAdaptor;
        Exception ex = null;
        bufferedPipeStep.securedSetup(null);
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

    public boolean seek(String seekChr, int seekPos) {
        return seek(seekChr, seekPos, -1);
    }

    @Override
    public boolean seek(String seekChr, int seekPos, int endPos) {
        long t = System.nanoTime();
        try {
            if (readerThread != null) {
                readerThread.stopProcessing( "Stop processing seeking to " + seekChr + " " + seekPos + " " + endPos );
                readerThread.poll();
                readerThread.join();
                if (sourceIterator instanceof GenomicIterator) {
                    ((GenomicIterator) sourceIterator).seek(seekChr, seekPos, endPos);
                } else {
                    Row next;
                    while( sourceIterator.hasNext() && ((next = sourceIterator.next()).chr.compareTo(seekChr) < 0 || (next.chr.compareTo(seekChr) == 0 && next.pos < seekPos)) );
                }

                readerThread.bufferedPipeStep.wantsNoMore_$eq(true);
                readerThread.bufferedPipeStep.alreadyFinished_$eq(false);
                readerThread.bufferedPipeStep.reset();
                readerThread.bufferAdaptor.reset();

                readerThread = new ReaderThread( this, readerThread.bufferedPipeStep, readerThread.bufferAdaptor, brsConfig);
                readerThread.setUncaughtExceptionHandler((tt, e) -> {
                    // THis is just so that the default handler does not write to std.err
                });
                rowBuffer = readerThread.rowBuffer1;
                readerThread.start();
            }
        } catch (InterruptedException e) {
            throw new GorSystemException("rowQueue take interrupted on seek", e);
        }
        if (readerThread!=null) readerThread.setAvgSeekTimeMilliSecond(((seekCount * readerThread.getAvgSeekTimeMilliSecond() + (System.nanoTime() - t) / 1000000.0) / (seekCount + 1)));
        seekCount++;
        return true;
    }

    private void closeSourceIterator() {
        if (sourceIterator instanceof GenomicIterator) ((GenomicIterator)sourceIterator).close();
    }

    @Override
    public void close() {
        if (readerThread != null && readerThread.didStart) {
            readerThread.stopProcessing("Stop processing closing source");
            try {
                readerThread.join(2000);
            } catch (InterruptedException ie) {
                log.warn("Reader thread join interrupted");
                Thread.currentThread().interrupt();
            }
        } else {
            try {
                pipeStep.securedFinish(null);
            } catch (Exception e) {
                setEx(e);
            } finally {
                closeSourceIterator();
            }
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
