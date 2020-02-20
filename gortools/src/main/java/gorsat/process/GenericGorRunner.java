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

package gorsat.process;

import gorsat.Commands.Processor;
import org.gorpipe.gor.GorRunner;
import org.gorpipe.model.gor.iterators.RowSource;
import gorsat.BatchedReadSource;

/**
 * This class handles the most basic gor pipe execution. This includes setup, process and finish phases.
 */
public class GenericGorRunner extends GorRunner {

    @Override
    public void run(RowSource iterator, Processor processor) {
        if (processor != null) {
            runProcessorHelper(iterator, processor);
        } else {
            while (iterator.hasNext()) {
                iterator.next();
            }
        }
    }

    private void runProcessorHelper(RowSource iterator, Processor processor) {
        RowSource brs = iterator.isBuffered() ? iterator : new BatchedReadSource(iterator, GorPipe.brsConfig());
        try {
            processor.rs_$eq(iterator);
            processor.securedSetup(null);
            while (brs.hasNext() && !processor.wantsNoMore()) {
                processor.process(brs.next());
            }
        } catch (Exception ex) {
            brs.setEx(ex);
            throw ex;
        } finally {
            try {
                processor.securedFinish(brs.getEx());
            } finally {
                brs.close();
            }
        }
    }
}
