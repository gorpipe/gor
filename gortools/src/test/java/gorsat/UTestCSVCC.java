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

    @Test
    public void automaticUnzipOfValues() {
        String[] normalQueryLines = {
                "create #dummy# = gorrow chr1,1,2 | signature -timeres 1 | calc x '0,1,2,3,4,5,6,7,8,9' | calc y x | calc z x | split x | split y | split z | calc rownum int(x)+10*int(y)+100*int(z) | sort 1 -c rownum:n;",
                "create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,100)) | rename rownum PN | select PN,bucket | top 352 | sort -c bucket,pn;",
                "create #pheno# = nor [#pnbuck#] | select #1 | where random() < 0.5 | calc pheno if(random()<0.8,'A1,A2','C1,C2') | split pheno;",
                "create #gt# = gorrow chr1,1,2 | calc x '1,2,3' | split x | select chrom,x | calc ref 'G' | calc alt 'A'  | select 1-4 | distinct | top 100 | multimap -cartesian -h [#pnbuck#] | calc gt mod(pn,3);",
                "create #allvars# = gor [#gt#] | select 1-4 | distinct;",
                "create #hor# = gor [#gt#] | sort 1 -c #3,#4,bucket,PN",
                "| group 1 -gc #3,#4,bucket -lis -sc gt | rename lis_gt values;",
                "gor [#hor#] | csvcc  -gc #3,#4 [#pnbuck#] [#pheno#] | calc method 'csvcc' ",
                "| merge <(gor [#allvars#] | multimap -cartesian -h [#pnbuck#] | join -snpsnp -l -e 0 -xl #3,#4,pn -xr #3,#4,pn [#gt#] ",
                "| multimap -c PN -h [#pheno#] | group 1 -gc #3,#4,pheno,gt -count ",
                "| rename pheno CC | rename allcount GTcount | calc method 'regular' ) ",
                "| group 1 -gc 3-method[-1] -dis -set -sc method | where gtcount != 0 | where dis_method != 2 "
        };

        String[] inflatedQueryLines = {
                "create #dummy# = gorrow chr1,1,2 | signature -timeres 1 | calc x '0,1,2,3,4,5,6,7,8,9' | calc y x | calc z x | split x | split y | split z | calc rownum int(x)+10*int(y)+100*int(z) | sort 1 -c rownum:n;",
                "create #pnbuck# = nor [#dummy#] | select rownum | calc bucket 'bucket'+str(1+div(rownum,100)) | rename rownum PN | select PN,bucket | top 352 | sort -c bucket,pn;",
                "create #pheno# = nor [#pnbuck#] | select #1 | where random() < 0.5 | calc pheno if(random()<0.8,'A1,A2','C1,C2') | split pheno;",
                "create #gt# = gorrow chr1,1,2 | calc x '1,2,3' | split x | select chrom,x | calc ref 'G' | calc alt 'A'  | select 1-4 | distinct | top 100 | multimap -cartesian -h [#pnbuck#] | calc gt mod(pn,3);",
                "create #allvars# = gor [#gt#] | select 1-4 | distinct;",
                "create #hor# = gor [#gt#] | sort 1 -c #3,#4,bucket,PN",
                "| group 1 -gc #3,#4,bucket -lis -sc gt | rename lis_gt values | deflatecolumn values;",
                "gor [#hor#] | csvcc  -gc #3,#4 [#pnbuck#] [#pheno#] | calc method 'csvcc' ",
                "| merge <(gor [#allvars#] | multimap -cartesian -h [#pnbuck#] | join -snpsnp -l -e 0 -xl #3,#4,pn -xr #3,#4,pn [#gt#] ",
                "| multimap -c PN -h [#pheno#] | group 1 -gc #3,#4,pheno,gt -count ",
                "| rename pheno CC | rename allcount GTcount | calc method 'regular' ) ",
                "| group 1 -gc 3-method[-1] -dis -set -sc method | where gtcount != 0 | where dis_method != 2 "
        };

        var normalLines =  TestUtils.runGorPipe(String.join("\n", normalQueryLines));
        var inflatedLines =  TestUtils.runGorPipe(String.join("\n", inflatedQueryLines));

        Assert.assertEquals(normalLines, inflatedLines);
    }

    @Test
    public void testCSVCCvsGroup() {
        TestUtils.runGorPipe("""
                create #bucket# = norrows 10 | calc bucket 'b1' | calc PN 'PN'+str(#1) | select pn,bucket;
                
                create #gt# = gorrow chr1,1 | calc alt 'A' | calc ref 'C'
                    | merge <(gorrow chr1,2 | calc alt 'G' | calc ref 'T')
                    | merge <(gorrow chr1,3 | calc alt 'A' | calc ref 'T')
                    | multimap -cartesian <(nor [#bucket#] | rownum)
                    | calc gt 1+mod(rownum*4+pos,10)
                    | select 1,2,ref,alt,gt,pn
                    | where PN != 'PN0'
                    | gtgen -gc ref,alt [#bucket#] <(gorrow chr1,0,1 | multimap -cartesian [#bucket#] | select 1-3,pn);
                    
                    gor [#gt#]
                    | csvsel -gc ref,alt,bucket -vs 1 [#bucket#] <(nor [#bucket#] | select pn) -tag PN
                    | group 1 -gc ref,alt,value -count
                    | calc source 'group'
                    | rename value GT | rename allcount GTcount
                    | merge <(gor [#gt#]
                    | csvsel -gc ref,alt,bucket -vs 1 [#bucket#] <(nor [#bucket#] | select pn)
                    | csvcc -gc ref,alt -vs 1 [#bucket#] <(nor [#bucket#] | select pn | calc pheno 'pheno')
                    | calc source 'vs'
                    | hide cc
                    )
                    
                    | merge <(gor [#gt#]
                    | csvsel -gc ref,alt,bucket -vs 1 [#bucket#] <(nor [#bucket#] | select pn)
                    | replace values fsvmap(values,1,'x',',')
                    | csvcc -gc ref,alt -s ',' [#bucket#] <(nor [#bucket#] | select pn | calc pheno 'pheno')
                    | calc source 'sep'
                    | hide cc
                    )
                    | group 1 -gc 2-source[-1] -set -dis -sc source
                    | throwif dis_source != 3
                """ );
    }

    @Test
    public void testGTValuesOfDifferentLengths() {
        TestUtils.runGorPipe("""
                 create #bucket# = norrows 10 | calc bucket 'b1' | calc PN 'PN'+str(#1) | select pn,bucket;
                
                create #gt# = gorrow chr1,1 | calc alt 'A' | calc ref 'C'
                    | merge <(gorrow chr1,2 | calc alt 'G' | calc ref 'T')
                    | merge <(gorrow chr1,3 | calc alt 'A' | calc ref 'T')
                    | multimap -cartesian <(nor [#bucket#] | rownum)
                    | calc gt 1+mod(rownum*4+pos,10)
                    | select 1,2,ref,alt,gt,pn
                    | where PN != 'PN0'
                    | gtgen -gc ref,alt [#bucket#] <(gorrow chr1,0,1 | multimap -cartesian [#bucket#] | select 1-3,pn);
                    
                gor [#gt#]
                | csvsel -gc ref,alt,bucket -vs 1 [#bucket#] <(nor [#bucket#] | select pn) -tag PN
                | replace value value*2
                | group 1 -gc ref,alt,value -count
                | calc source 'group'
                | rename value GT | rename allcount GTcount
                | merge <(gor [#gt#]
                | csvsel -gc ref,alt,bucket -vs 1 [#bucket#] <(nor [#bucket#] | select pn)
                | replace values fsvmap(values,1,'int(x)*2',',')
                | csvcc -gc ref,alt -s ',' [#bucket#] <(nor [#bucket#] | select pn | calc pheno 'pheno')
                | calc source 'sep'
                | hide cc
                )
                | group 1 -gc 2-source[-1] -set -dis -sc source
                | throwif dis_source != 2                    
                    
                """ );
    }

}
