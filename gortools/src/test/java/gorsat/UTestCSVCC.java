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

package gorsat;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sigmar on 19/12/2016.
 */
public class UTestCSVCC {
    @Test
    public void testCvscc() {
        String[] args = new String[]{"create #dummy# = gorrow chr1,1,2 | signature -timeres 1 | calc x '0,1,2,3,4,5,6,7,8,9' | calc y x | calc z x | split x | split y | split z | calc rownum int(x)+10*int(y)+100*int(z) | sort 1 -c rownum:n;\n" +
                "    \n" +
                "    create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,100)) | rename rownum PN | select PN,bucket | top 352 | sort -c bucket,pn;\n" +
                "    \n" +
                "    create #pheno# = nor [#pnbuck#] | select #1 | where random() < 0.5 | calc pheno if(random()<0.8,'A1,A2','C1,C2') | split pheno;\n" +
                "    \n" +
                "    create #gt# = gorrow chr1,1,2 | calc x '1,2,3' | split x | select chrom,x | calc ref 'G' | calc alt 'A'  | select 1-4 | distinct | top 100 | multimap -cartesian -h [#pnbuck#] | calc gt mod(pn,3);\n" +
                "    \n" +
                "    create #allvars# = gor [#gt#] | select 1-4 | distinct;\n" +
                "    \n" +
                "    create #hor# = gor [#gt#] | sort 1 -c #3,#4,bucket,PN\n" +
                "    | group 1 -gc #3,#4,bucket -lis -sc gt | rename lis_gt values;\n" +
                "    \n" +
                "    gor [#hor#] | csvcc  -gc #3,#4 [#pnbuck#] [#pheno#] | calc method 'csvcc'\n" +
                "    | merge <(gor [#allvars#] | multimap -cartesian -h [#pnbuck#] | join -snpsnp -l -e 0 -xl #3,#4,pn -xr #3,#4,pn [#gt#]\n" +
                "    | multimap -c PN -h [#pheno#] | group 1 -gc #3,#4,pheno,gt -count\n" +
                "    | rename pheno CC | rename allcount GTcount | calc method 'regular' )\n" +
                "    | group 1 -gc 3-method[-1] -dis -set -sc method | where gtcount != 0 | where dis_method != 2\n"};

        int count = TestUtils.runGorPipeCount(args);
        Assert.assertEquals(0, count);
    }
}
