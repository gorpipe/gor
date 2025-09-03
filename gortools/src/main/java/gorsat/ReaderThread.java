package gorsat;

import gorsat.Commands.Analysis;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class ReaderThread extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ReaderThread.class);

    private final BatchedPipeStepIteratorAdaptor batchedPipeStepIteratorAdaptor;

    private final Duration batchOfferTimeout;
    private final Duration timeout;
    private final Duration logInterval;
    private final Duration timeTriggerBufferFlush;

    private final SynchronousQueue<RowBuffer> rowQueue = new SynchronousQueue<>();
    final RowBuffer rowBuffer1 = new RowBuffer();
    final RowBuffer rowBuffer2 = new RowBuffer(rowBuffer1);
    Analysis bufferedPipeStep;
    BufferAdaptor bufferAdaptor;
    private boolean stopProcessing = false;
    boolean didStart = false;

    private long numberOfPollsBeforeLog;
    private long numberOfPollsBeforeTimeout;

    Map<String, String> contextMap;

    public ReaderThread(BatchedPipeStepIteratorAdaptor batchedPipeStepIteratorAdaptor, Analysis pipeStep, BufferAdaptor bufferAdaptor, BatchedReadSourceConfig brsConfig) {
        this.batchedPipeStepIteratorAdaptor = batchedPipeStepIteratorAdaptor;
        this.bufferAdaptor = bufferAdaptor;
        bufferAdaptor.setReaderThread(this);
        bufferedPipeStep = pipeStep;
        bufferedPipeStep.wantsNoMore_$eq(false);

        batchOfferTimeout = brsConfig.getBatchOfferTimeout();
        timeout = Duration.ofSeconds(Long.parseLong(System.getProperty("gor.timeout.rowsource", "1800000")));
        logInterval = brsConfig.getLogInterval();
        timeTriggerBufferFlush = brsConfig.getBufferFlushTimout();

        init();
    }

    public ReaderThread(BatchedReadSourceConfig brsConfig, BatchedPipeStepIteratorAdaptor batchedPipeStepIteratorAdaptor, Analysis pipeStep) {
        this.batchedPipeStepIteratorAdaptor = batchedPipeStepIteratorAdaptor;
        batchOfferTimeout = brsConfig.getBatchOfferTimeout();
        timeout = Duration.ofSeconds(Long.parseLong(System.getProperty("gor.timeout.rowsource", "1800000")));
        logInterval = brsConfig.getLogInterval();
        timeTriggerBufferFlush = brsConfig.getBufferFlushTimout();

        init();
        initPipeStep(pipeStep);
    }

    public double getAvgRowsPerMilliSecond() {
        return bufferAdaptor.avgRowsPerMilliSecond;
    }

    public double getAvgBasesPerMilliSecond() {
        return bufferAdaptor.avgBasesPerMilliSecond;
    }

    public double getAvgBatchSize() {
        return bufferAdaptor.avgBatchSize;
    }

    public double getAvgSeekTimeMilliSecond() {
        return bufferAdaptor.avgSeekTimeMilliSecond;
    }

    public void setAvgSeekTimeMilliSecond(double avgSeekTimeMilliSecond) {
        bufferAdaptor.avgSeekTimeMilliSecond = avgSeekTimeMilliSecond;
    }

    public void setMDC(Map<String, String> contextMap) {
        this.contextMap = contextMap;
    }

    private void init() {
        this.setName(Thread.currentThread().getName() + "::ReaderThread");
        rowBuffer1.setNextRowBuffer(rowBuffer2);
        numberOfPollsBeforeLog = logInterval.toMillis() / batchOfferTimeout.toMillis();
        numberOfPollsBeforeTimeout = timeout.toMillis() / batchOfferTimeout.toMillis();
    }

    private void initPipeStep(Analysis pipeStep) {
        long timeTriggerBufferFlushNs = timeTriggerBufferFlush.getNano();
        if (timeTriggerBufferFlushNs < 0) {
            bufferAdaptor = new BufferAdaptor(this);
        } else {
            bufferAdaptor = new TimeoutBufferAdaptor(this, timeTriggerBufferFlushNs);
        }
        bufferedPipeStep = pipeStep != null ? pipeStep.$bar(bufferAdaptor) : bufferAdaptor;
        bufferedPipeStep.securedSetup(null);
    }

    public void stopProcessing( String message ) {
        log.warn(message);
        stopProcessing = true;
        if (bufferedPipeStep != null) {
            bufferedPipeStep.wantsNoMore_$eq(true);
        }
        if (bufferAdaptor != null) {
            bufferAdaptor.reportWantsNoMore();
        }
    }

    public void finish() {
        try {
            bufferedPipeStep.securedFinish(batchedPipeStepIteratorAdaptor.getEx());
        } catch (Throwable e) {
            batchedPipeStepIteratorAdaptor.setEx(e);
            stopProcessing("Stop processing error in finish " + e.getMessage());
        }
    }

    private void closeSourceIterator() {
        Iterator<? extends Row> sourceIterator = batchedPipeStepIteratorAdaptor.sourceIterator;
        if (sourceIterator instanceof GenomicIterator) ((GenomicIterator)sourceIterator).close();
    }

    public void run() {
        if (contextMap != null) {
            MDC.setContextMap(contextMap);
        }
        didStart = true;
        Row r = null;
        try {
            Iterator<? extends Row> sourceIterator = batchedPipeStepIteratorAdaptor.sourceIterator;
            while (sourceIterator.hasNext() && !bufferedPipeStep.wantsNoMore()) {
                r = sourceIterator.next();
                bufferedPipeStep.process(r);
            }
        } catch (Throwable e) {
            batchedPipeStepIteratorAdaptor.setEx(e);
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
        while (didStart && !stopProcessing && !rowQueue.offer(current, batchOfferTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
            if (count > numberOfPollsBeforeTimeout) {
                throw new GorSystemException("BatchedIteratorAdaptor polling for too long " + timeout.getSeconds(), null);
            }
            if (count++ % numberOfPollsBeforeLog == 0)
                log.debug("Offering batch for {}, batch size {}, query {}", numberOfPollsBeforeLog * count, current.size(), this);
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
                log.debug("Polling batch for {} time {}, query {}", numberOfPollsBeforeLog * count, Thread.currentThread().getId(), this);
        }
        return ret;
    }
}
