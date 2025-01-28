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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Iterator that is based on stream coming from the Supplier provided.
 * Supports reinitialization of the stream and iterator when seek is called.
 */
public abstract class BaseStreamIterator extends gorsat.Iterators.IteratorSource {

    private final Supplier<Stream<String>> streamSupplier;
    private Iterator<String> dbit;
    private String header;
    private Stream<String> strstr;

    public BaseStreamIterator(Supplier<Stream<String>> streamSupplier) {
        this.streamSupplier = streamSupplier;
        initIteratorAndHeader();
    }

    protected void initIteratorAndHeader() {
        try {
            strstr = streamSupplier.get();
            dbit = strstr.iterator();
            header = produceHeaderFromData();
        } catch (GorResourceException e) {
            throw new GorSystemException("Unable to initialize iterator from stream.", e);
        } catch (NoSuchElementException e){
            throw new GorSystemException("Unable to initialize iterator from stream.", e);
        }
    }

    public String produceHeaderFromData() {
        //todo handle calling hasNext before getting header
        var header =  dbit.next();
        return header.startsWith("#") ? header.substring(1) : header;
    }

    @Override
    public boolean hasNext() {
        return dbit.hasNext();
    }

    @Override
    public String next() {
        return dbit.next();
    }

    @Override
    public void setPosition(String seekChr, int seekPos) {
        strstr.close();
        initIteratorAndHeader();
    }

    @Override
    public String getHeader() {
        return header;
    }

    @Override
    public void close() {
        strstr.close();
    }

}
