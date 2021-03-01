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

package org.gorpipe.gor.function;

import gorsat.process.GenericSessionFactory;
import gorsat.process.PipeInstance;
import gorsat.process.PipeOptions;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.model.Row;

import java.io.Serializable;
import java.util.function.Function;
import java.util.stream.Stream;

public class GorRowQueryFunction implements Function<Row, Stream<Row>>, AutoCloseable, Serializable {
    String query;
    protected String header;
    protected ListRowAdaptor lra;
    protected gorsat.Commands.Analysis bufferedPipeStep;

    public GorRowQueryFunction(String query) {
        this(query, "CHROM\tPOS");
    }

    public GorRowQueryFunction(String query, String inputHeader) {
        this.query = query;
        this.header = inputHeader;
    }

    public String getHeader(String inputHeader) {
        PipeInstance pi = init(inputHeader);
        return pi.getHeader();
    }

    public String getHeader() {
        return getHeader(header);
    }

    public PipeInstance init(String header) {
        String[] args = {query,"-stdin"};
        PipeOptions pipeOptions = new PipeOptions();
        pipeOptions.parseOptions(args);

        GenericSessionFactory gsf = new GenericSessionFactory();
        GorSession gps = gsf.create();
        PipeInstance pi = new PipeInstance(gps.getGorContext());
        pi.init(pipeOptions.query(), pipeOptions.stdIn(), header);
        return pi;
    }

    public void initAdaptor() {
        PipeInstance pi = init(header);
        gorsat.Commands.Analysis pipeStep = pi.thePipeStep();
        lra = new ListRowAdaptor();
        bufferedPipeStep = pipeStep != null ? pipeStep.$bar(lra) : lra;
        bufferedPipeStep.securedSetup(null);

    }

    @Override
    public Stream<Row> apply(Row row) {
        if( lra == null ) initAdaptor();
        else lra.clear();

        if( row.pos == -1 ) close();
        bufferedPipeStep.process(row);
        return lra.stream();
    }

    @Override
    public void close() {
        bufferedPipeStep.securedFinish(null);
    }
}
