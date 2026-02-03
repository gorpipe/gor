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

package org.gorpipe.gor.driver.providers.stream.datatypes.cram.reference;

import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UTestCompositeReferenceSource {

    private CRAMReferenceSource mockSource1;
    private CRAMReferenceSource mockSource2;
    private CRAMReferenceSource mockSource3;
    private SAMSequenceRecord testRecord;
    private byte[] testBases;

    @Before
    public void setUp() {
        mockSource1 = mock(CRAMReferenceSource.class);
        mockSource2 = mock(CRAMReferenceSource.class);
        mockSource3 = mock(CRAMReferenceSource.class);
        testRecord = new SAMSequenceRecord("chr1", 1000);
        testBases = "ACGTACGT".getBytes();
    }

    @Test
    public void getReferenceBasesReturnsResultFromSingleSource() {
        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(testBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Collections.singletonList(mockSource1));

        byte[] result = composite.getReferenceBases(testRecord, false);

        assertArrayEquals(testBases, result);
        verify(mockSource1, times(1)).getReferenceBases(testRecord, false);
    }

    @Test
    public void getReferenceBasesReturnsResultFromFirstSourceWhenMultipleSources() {
        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(testBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Arrays.asList(mockSource1, mockSource2));

        byte[] result = composite.getReferenceBases(testRecord, false);

        assertArrayEquals(testBases, result);
        verify(mockSource1, times(1)).getReferenceBases(testRecord, false);
        verify(mockSource2, never()).getReferenceBases(testRecord, false);
    }

    @Test
    public void getReferenceBasesFallsBackToSecondSourceWhenFirstFails() {
        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(null);
        when(mockSource2.getReferenceBases(testRecord, false)).thenReturn(testBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Arrays.asList(mockSource1, mockSource2));

        byte[] result = composite.getReferenceBases(testRecord, false);

        assertArrayEquals(testBases, result);
        verify(mockSource1, times(1)).getReferenceBases(testRecord, false);
        verify(mockSource2, times(1)).getReferenceBases(testRecord, false);
    }

    @Test
    public void getReferenceBasesThrowsExceptionWhenAllSourcesFail() {
        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(null);
        when(mockSource2.getReferenceBases(testRecord, false)).thenReturn(null);
        when(mockSource3.getReferenceBases(testRecord, false)).thenReturn(null);

        CompositeReferenceSource composite = new CompositeReferenceSource(Arrays.asList(mockSource1, mockSource2, mockSource3));

        assertEquals(null, composite.getReferenceBases(testRecord, false));
    }

    @Test
    public void getReferenceBasesPassesTryNameVariantsParameter() {
        when(mockSource1.getReferenceBases(testRecord, true)).thenReturn(testBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Collections.singletonList(mockSource1));

        byte[] result = composite.getReferenceBases(testRecord, true);

        assertArrayEquals(testBases, result);
        verify(mockSource1, times(1)).getReferenceBases(testRecord, true);
    }


    @Test
    public void getReferenceBasesThrowsExceptionWhenSourcesListIsEmpty() {
        CompositeReferenceSource composite = new CompositeReferenceSource(new ArrayList<>());
        assertEquals(null, composite.getReferenceBases(testRecord, false));
    }

    @Test
    public void getReferenceBasesHandlesDifferentSequences() {
        SAMSequenceRecord record1 = new SAMSequenceRecord("chr1", 1000);
        SAMSequenceRecord record2 = new SAMSequenceRecord("chr2", 2000);
        byte[] bases1 = "ACGT".getBytes();
        byte[] bases2 = "TGCA".getBytes();

        when(mockSource1.getReferenceBases(record1, false)).thenReturn(bases1);
        when(mockSource1.getReferenceBases(record2, false)).thenReturn(bases2);

        CompositeReferenceSource composite = new CompositeReferenceSource(Collections.singletonList(mockSource1));

        assertArrayEquals(bases1, composite.getReferenceBases(record1, false));
        assertArrayEquals(bases2, composite.getReferenceBases(record2, false));
    }

    @Test
    public void getReferenceBasesReturnsFirstSuccessfulResult() {
        byte[] bases1 = "FIRST".getBytes();
        byte[] bases2 = "SECOND".getBytes();

        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(bases1);
        when(mockSource2.getReferenceBases(testRecord, false)).thenReturn(bases2);

        CompositeReferenceSource composite = new CompositeReferenceSource(Arrays.asList(mockSource1, mockSource2));

        byte[] result = composite.getReferenceBases(testRecord, false);

        assertArrayEquals(bases1, result);
        verify(mockSource1, times(1)).getReferenceBases(testRecord, false);
        verify(mockSource2, never()).getReferenceBases(testRecord, false);
    }

    @Test
    public void getReferenceBasesCanBeCalledMultipleTimes() {
        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(testBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Collections.singletonList(mockSource1));

        assertArrayEquals(testBases, composite.getReferenceBases(testRecord, false));
        assertArrayEquals(testBases, composite.getReferenceBases(testRecord, false));
        verify(mockSource1, times(2)).getReferenceBases(testRecord, false);
    }

    @Test
    public void getReferenceBasesHandlesLargeSequences() {
        byte[] largeBases = new byte[10000];
        Arrays.fill(largeBases, (byte) 'A');

        when(mockSource1.getReferenceBases(testRecord, false)).thenReturn(largeBases);

        CompositeReferenceSource composite = new CompositeReferenceSource(Collections.singletonList(mockSource1));

        byte[] result = composite.getReferenceBases(testRecord, false);

        assertEquals(largeBases.length, result.length);
        assertArrayEquals(largeBases, result);
    }
}
