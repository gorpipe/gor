/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package gorsat.process;

import scala.Function0;
import scala.Unit;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class encapsulates a general execution in parallel of the pgor command when a
 * standard parallel query handler is not used.
 */
public class ParallelExecutor {

    private Throwable firstException;
    private final Thread[] threads;
    private final Function0<Unit>[] commands;

    public ParallelExecutor(int workers, Function0<Unit>[] commands) {
        this.commands = commands;
        this.threads = new Thread[workers];
        this.firstException = null;
    }

    @SuppressWarnings("squid:S00112") // We need to handle Throwable here, sorry
    public void parallelExecute() throws Throwable {
        ConcurrentLinkedQueue<Function0<Unit>> clq = new ConcurrentLinkedQueue<>(Arrays.asList(commands));
        for( int i = 0; i < threads.length; i++ ) {
            Thread t = new Thread(() -> {
                Function0<Unit> func = clq.poll();
                while( func != null ) {
                    func.apply();
                    func = clq.poll();
                }
            }, "ParallelExecutorThread-" + i);
            t.setUncaughtExceptionHandler(this::parallelExcecuteUncaughtExceptionHandler);
            t.start();
            threads[i] = t;
        }
        for( Thread t : threads ) {
            t.join();
            if (firstException != null) {
                throw firstException;
            }
        }
    }

    private synchronized void parallelExcecuteUncaughtExceptionHandler(Thread thread, Throwable throwable) {
        if (firstException == null) {
            firstException = throwable;
            for (Thread t : threads) {
                if (t != thread) {
                    t.interrupt();
                }
            }
        }
    }

}
