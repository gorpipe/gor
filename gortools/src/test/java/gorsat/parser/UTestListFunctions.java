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

package gorsat.parser;

import gorsat.TestUtils;
import org.junit.Assert;
import org.junit.Test;

public class UTestListFunctions {
    @Test
    public void testListIndex() {
        TestUtils.assertCalculated("listIndex('','')",1);
        TestUtils.assertCalculated("listIndex('a','b')", -1);
        TestUtils.assertCalculated("listIndex('a,b,c,d,e,f','c')",3);
        TestUtils.assertCalculated("listIndex('a,b,c','c')",3);
        TestUtils.assertCalculated("listIndex('a,b,c','a')",1);
    }

    @Test
    public void testListIndexWithSep() {
        TestUtils.assertCalculated("listIndex('','',',,,')",1);
        TestUtils.assertCalculated("listIndex('a','b',',,')", -1);
        TestUtils.assertCalculated("listIndex('a,,,b,,,c,,,d,,,e,,,f','c',',,,')",3);
        TestUtils.assertCalculated("listIndex('a,,,b,,,c','c',',,,')",3);
        TestUtils.assertCalculated("listIndex('a,,,b,,,c','a',',,,')",1);
    }

    @Test
    public void testListLast() {
        TestUtils.assertCalculated("listlast('')", "");
        TestUtils.assertCalculated("listlast('A')", "A");
        TestUtils.assertCalculated("listlast('1,2,3,4,5')", "5");
        TestUtils.assertCalculated("listlast('one,two,three')", "three");
    }

    @Test
    public void testListLastWithSep() {
        TestUtils.assertCalculated("listlast('',';')", "");
        TestUtils.assertCalculated("listlast('A',';')", "A");
        TestUtils.assertCalculated("listlast('12345','')", "5");
        TestUtils.assertCalculated("listlast('one1two1three','1')", "three");
        TestUtils.assertCalculated("listlast('a,,,b,,,c,,,',',,,')", "");
    }

    @Test
    public void testListTail() {
        TestUtils.assertCalculated("listtail('')", "");
        TestUtils.assertCalculated("listtail('A')", "");
        TestUtils.assertCalculated("listtail('1,2,3,4,5')", "2,3,4,5");
        TestUtils.assertCalculated("listtail('one,two,three')", "two,three");
    }

    @Test
    public void testListTailWithSep() {
        TestUtils.assertCalculated("listtail('',';')", "");
        TestUtils.assertCalculated("listtail('A',';')", "");
        TestUtils.assertCalculated("listtail('1 2 3 4 5',' ')", "2 3 4 5");
        TestUtils.assertCalculated("listtail('one-two-three','-')", "two-three");
        TestUtils.assertCalculated("listtail('a,,,b,,,c,,,,',',,,')", "b,,,c,,,,");
        TestUtils.assertCalculated("listtail('a,,,',',,,')", "");
    }

    @Test
    public void testListReverse() {
        TestUtils.assertCalculated("listreverse('')", "");
        TestUtils.assertCalculated("listreverse('A')", "A");
        TestUtils.assertCalculated("listreverse('1,2,3,4,5')", "5,4,3,2,1");
        TestUtils.assertCalculated("listreverse('one,two,three')", "three,two,one");
    }

    @Test
    public void testListReverseWithSep() {
        TestUtils.assertCalculated("listreverse('a,,,b,,,c,,,',',,,')", ",,,c,,,b,,,a");
        TestUtils.assertCalculated("listreverse('',',,,')", "");
    }

    @Test
    public void testListSortAsc() {
        TestUtils.assertCalculated("listsortasc('')", "");
        TestUtils.assertCalculated("listsortasc('A')", "A");
        TestUtils.assertCalculated("listsortasc('1,2,3,4,5')", "1,2,3,4,5");
        TestUtils.assertCalculated("listsortasc('one,two,three')", "one,three,two");
    }

    @Test
    public void testListSortDesc() {
        TestUtils.assertCalculated("listsortdesc('')", "");
        TestUtils.assertCalculated("listsortdesc('A')", "A");
        TestUtils.assertCalculated("listsortdesc('1,2,3,4,5')", "5,4,3,2,1");
        TestUtils.assertCalculated("listsortdesc('one,two,three')", "two,three,one");
    }

    @Test
    public void testListNumSortAsc() {
        TestUtils.assertCalculated("listnumsortasc('1,2,3')", "1,2,3");
        TestUtils.assertCalculated("listnumsortasc('100,9,17,88,5')", "5,9,17,88,100");
        TestUtils.assertCalculated("listnumsortasc('0,10,-10,100,-100')", "-100,-10,0,10,100");
        TestUtils.assertCalculated("listnumsortasc('0.5,1,0.3,-0.5')", "-0.5,0.3,0.5,1");
    }

    @Test
    public void testListNumSortDesc() {
        TestUtils.assertCalculated("listnumsortdesc('1,2,3')", "3,2,1");
        TestUtils.assertCalculated("listnumsortdesc('100,9,17,88,5')", "100,88,17,9,5");
        TestUtils.assertCalculated("listnumsortdesc('0,10,-10,100,-100')", "100,10,0,-10,-100");
        TestUtils.assertCalculated("listnumsortdesc('0.5,1,0.3,-0.5')", "1,0.5,0.3,-0.5");
    }

    @Test
    public void testListTrim() {
        TestUtils.assertCalculated("listtrim('   ')", "");
        TestUtils.assertCalculated("listtrim(' A ')", "A");
        TestUtils.assertCalculated("listtrim(' 1, 2,3, 4, 5 ')", "1,2,3,4,5");
        TestUtils.assertCalculated("listtrim('   one, two,  three ')", "one,two,three");
    }

    @Test
    public void testListDist() {
        TestUtils.assertCalculated("listdist('')", "");
        TestUtils.assertCalculated("listdist('A,A,A')", "A");
        TestUtils.assertCalculated("listdist('1,1,1,2,2,3')", "1,2,3");
        TestUtils.assertCalculated("listdist('one,two,three,one,three,three')", "one,two,three");
    }

    @Test
    public void testListMax() {
        TestUtils.assertCalculated("listmax('')", "");
        TestUtils.assertCalculated("listmax('A,A,A')", "A");
        TestUtils.assertCalculated("listmax('1,2,3,4,5')", "5");
        TestUtils.assertCalculated("listmax('one,two,three')", "two");
    }

    @Test
    public void testListMin() {
        TestUtils.assertCalculated("listmin('')", "");
        TestUtils.assertCalculated("listmin('A,A,A')", "A");
        TestUtils.assertCalculated("listmin('1,2,3,4,5')", "1");
        TestUtils.assertCalculated("listmin('one,two,three')", "one");
    }

    @Test
    public void testListSize() {
        TestUtils.assertCalculated("listsize('')", 0);
        TestUtils.assertCalculated("listsize('A')", 1);
        TestUtils.assertCalculated("listsize('1,2,3,4,5')", 5);
        TestUtils.assertCalculated("listsize('one,two,three')", 3);
        TestUtils.assertCalculated("listsize('onextwoxthree', 'x')", 3);
    }

    @Test
    public void testListNumMax() {
        TestUtils.assertCalculated("listnummax('1,2,3')", 3.0);
        TestUtils.assertCalculated("listnummax('100,9,17,88,5')", 100.0);
        TestUtils.assertCalculated("listnummax('0,10,-10,100,-100')", 100.0);
        TestUtils.assertCalculated("listnummax('0.5,1,0.3,-0.5')", 1.0);
    }

    @Test
    public void testListNumMin() {
        TestUtils.assertCalculated("listnummin('1,2,3')", 1.0);
        TestUtils.assertCalculated("listnummin('100,9,17,88,5')", 5.0);
        TestUtils.assertCalculated("listnummin('0,10,-10,100,-100')", -100.0);
        TestUtils.assertCalculated("listnummin('0.5,1,0.3,-0.5')", -0.5);
    }

    @Test
    public void testListNumSum() {
        TestUtils.assertCalculated("listnumsum('1,2,3')", 6.0);
        TestUtils.assertCalculated("listnumsum('100,9,17,88,5')", 219.0);
        TestUtils.assertCalculated("listnumsum('0,10,-10,100,-100')", 0.0);
        TestUtils.assertCalculated("listnumsum('0.5,1,0.3,-0.5')", 1.3);
    }

    @Test
    public void testListNumAvg() {
        TestUtils.assertCalculated("listnumavg('1,2,3')", 2.0);
        TestUtils.assertCalculated("listnumavg('100,9,17,88,5')", 43.8);
        TestUtils.assertCalculated("listnumavg('0,10,-10,100,-100')", 0.0);
        TestUtils.assertCalculated("listnumavg('0.5,1,0.3,-0.5')", 0.325);
    }

    @Test
    public void testListNumStd() {
        TestUtils.assertCalculated("listnumstd('1,2,3')", 1.0);
        TestUtils.assertCalculated("listnumstd('100,9,17,88,5')", 46.22445240346283);
        TestUtils.assertCalculated("listnumstd('0,10,-10,100,-100')", 71.06335201775947);
        TestUtils.assertCalculated("listnumstd('0.5,1,0.3,-0.5')", 0.6238322424070967);
    }

    @Test
    public void testListComb() {
        TestUtils.assertCalculated("listcomb('a,b,c,d', 2,3)", "a,b,c;a,b,d;a,c,d;b,c,d;a,b;a,c;a,d;b,c;b,d;c,d");
        TestUtils.assertCalculated("listcomb('a,a,b,b,c,c',2,4)", "a,a,b,b;a,a,b,c;a,a,c,c;a,b,b,c;a,b,c,c;b,b,c,c;a,a,b;a,a,c;a,b,b;a,b,c;a,c,c;b,b,c;b,c,c;a,a;a,b;a,c;b,b;b,c;c,c");
        TestUtils.assertCalculated("lIsTcOmB('a,b,c,d,e,f', 1,1)", "a;b;c;d;e;f");
        TestUtils.assertCalculated("listcomb('a,s,d,f,g,h,j,k,l', 4)", "a,s,d,f;a,s,d,g;a,s,d,h;a,s,d,j;a,s,d,k;a,s,d,l;a,s,f,g;a,s,f,h;a,s,f,j;a,s,f,k;a,s,f,l;a,s,g,h;a,s,g,j;a,s,g,k;a,s,g,l;a,s,h,j;a,s,h,k;a,s,h,l;a,s,j,k;a,s,j,l;a,s,k,l;a,d,f,g;a,d,f,h;a,d,f,j;a,d,f,k;a,d,f,l;a,d,g,h;a,d,g,j;a,d,g,k;a,d,g,l;a,d,h,j;a,d,h,k;a,d,h,l;a,d,j,k;a,d,j,l;a,d,k,l;a,f,g,h;a,f,g,j;a,f,g,k;a,f,g,l;a,f,h,j;a,f,h,k;a,f,h,l;a,f,j,k;a,f,j,l;a,f,k,l;a,g,h,j;a,g,h,k;a,g,h,l;a,g,j,k;a,g,j,l;a,g,k,l;a,h,j,k;a,h,j,l;a,h,k,l;a,j,k,l;s,d,f,g;s,d,f,h;s,d,f,j;s,d,f,k;s,d,f,l;s,d,g,h;s,d,g,j;s,d,g,k;s,d,g,l;s,d,h,j;s,d,h,k;s,d,h,l;s,d,j,k;s,d,j,l;s,d,k,l;s,f,g,h;s,f,g,j;s,f,g,k;s,f,g,l;s,f,h,j;s,f,h,k;s,f,h,l;s,f,j,k;s,f,j,l;s,f,k,l;s,g,h,j;s,g,h,k;s,g,h,l;s,g,j,k;s,g,j,l;s,g,k,l;s,h,j,k;s,h,j,l;s,h,k,l;s,j,k,l;d,f,g,h;d,f,g,j;d,f,g,k;d,f,g,l;d,f,h,j;d,f,h,k;d,f,h,l;d,f,j,k;d,f,j,l;d,f,k,l;d,g,h,j;d,g,h,k;d,g,h,l;d,g,j,k;d,g,j,l;d,g,k,l;d,h,j,k;d,h,j,l;d,h,k,l;d,j,k,l;f,g,h,j;f,g,h,k;f,g,h,l;f,g,j,k;f,g,j,l;f,g,k,l;f,h,j,k;f,h,j,l;f,h,k,l;f,j,k,l;g,h,j,k;g,h,j,l;g,h,k,l;g,j,k,l;h,j,k,l");
        TestUtils.assertCalculated("listcomb('a,b,c,d,e',1)", "a;b;c;d;e");
        TestUtils.assertCalculated("listcomb('a,b,c,d,e', 2)", "a,b;a,c;a,d;a,e;b,c;b,d;b,e;c,d;c,e;d,e");
    }

    @Test
    public void testListAdd() {
        TestUtils.assertCalculated("listadd('', '')", "");
        TestUtils.assertCalculated("listadd('', 'a')", "a");
        TestUtils.assertCalculated("listadd('a', 'b')", "a,b");
        TestUtils.assertCalculated("listadd('a,b,c,d', 'e')", "a,b,c,d,e");
        TestUtils.assertCalculated("listadd('', 'a', ':')", "a");
        TestUtils.assertCalculated("listadd('a:b:c:d', 'e', ':')", "a:b:c:d:e");
    }

    // https://nextcode.atlassian.net/browse/GOP-631
    @Test
    public void Gop631() {
        final String result = TestUtils.runGorPipe("gorrow 1,1 | calc list 'a,b,c,d' | calc x listmap(list, 'squote(x)')");
        final String expected = "chrom\tpos\tlist\tx\n" +
                "chr1\t1\ta,b,c,d\t'a','b','c','d'\n";
        Assert.assertEquals(expected, result);
    }
}
