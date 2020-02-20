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

import org.junit.Test;


public class UTestGtGenAndLd {

    String testSetupGORQL =
            "create #dummy# = nor <(norrows 1000 | sort -c rownum:n);" +
                    "create #buckets# = nor [#dummy#] | rename #1 PN | calc bucket 'b_'+str(div(PN,50)+1);" +
                    "create #loci# = gorrow chr1,1,2 | multimap -cartesian -h <(norrows 20000) | calc Npos #2+RowNum | select 1,Npos | sort genome | rename #2 Pos | calc ref 'G' |  calc alt 'C';" +
                    "create #gt# = gor [#loci#] | multimap -cartesian -h [#buckets#] | distloc 20000 | hide bucket | calc gt mod(PN,4) | where random()<0.05;" +
                    "create #cov# = gorrow chr1,0,3000 | multimap -cartesian -h [#buckets#] | where not(bucket='b_2');";

    String basicGtGen = "gor [#gt#] | replace gt 0 | gtgen -gc ref,alt [#buckets#] [#cov#] | top 1000";
    String basicGtLd = " | replace values fsvmap(values, 1, 'if(sin(pos)>0.5, \"2\", \"0\")', '') | gtld -sum -f 100";

    
    @Test
    public void testGtGen() {
        String query = basicGtGen +
                " | csvsel -gc ref,alt -u 3 -vs 1 [#buckets#] <(nor [#buckets#] | select #1) " +
                " | gtld -sum -f 100 -calc" +
                " | merge <(" + basicGtGen + " | gtld -sum -f 100 -calc) " +
                " | group 1 -gc 3- -count| throwif allcount != 2 | where 2=3";

        TestUtils.runGorPipe(testSetupGORQL + query );
    }

    @Test
    public void testGtGen2() {
        String query = "create yyy = " + basicGtGen + basicGtLd + ";" +
                "gor [yyy] | gtld -calc" +
                " | merge <(" + basicGtGen + basicGtLd + " -calc) " +
                " | group 1 -gc 3- -count | throwif allcount != 2 | where 2=3";

        TestUtils.runGorPipe(testSetupGORQL + query);
    }

    @Test
    public void testGtGen3() {
        String query = "create yyy = " + basicGtGen + basicGtLd + ";" +
                "gor [yyy] | replace LD_x11 3 | replace LD_x12 1| replace LD_x21 1 | replace LD_x22 3" +
                " | gtld -calc " +
                " | throwif abs(ld_dp-0.5)>0.01 or abs(ld_r-0.5)>0.01 | where 2=3";

        TestUtils.runGorPipe(testSetupGORQL + query);
    }

}
