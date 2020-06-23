package gorsat;

import org.junit.Ignore;
import org.junit.Test;

public class UTestGorKing {

    @Test
    @Ignore("fix top 0")
    public void test_equivalent_queries() {
        final String query = "def #r# = rename #1 PN | replace #1 'PN'+#1;\n" +
                "def #n# = 100;\n" +
                "def #bucksize# = 17;\n" +
                "\n" +
                "create #bucket# = norrows #n# | #r# | rownum | calc bucket 'b'+str(1+div(rownum, #bucksize# )) | hide rownum;\n" +
                "create #dummybuck# = norrows #n# | #r# | calc bucket 'b1';\n" +
                "create #pns# = norrows #n# | #r#;\n" +
                "create #pns1# = nor [#pns#];\n" +
                "create #pns2# = nor [#pns#];\n" +
                "\n" +
                "create #rel# = nor [#pns1#] | multimap -cartesian [#pns2#] | rename PN id1 | rename PNx id2 | select id1,id2;\n" +
                "\n" +
                "create #values# = norrows 100 | rename #1 i | multimap -cartesian <(norrows #n# | rename #1 j) | calc gt mod(i+j,4) | group -gc i -lis -sc gt -s '' | rename #2 values;\n" +
                "create #vars# = gorrows -p chr1:1-100 | select 1-2 | calc ref 'C' | calc alt 'A' | calc af random() | map -c pos [#values#] | calc bucket 'b1'\n" +
                "| csvsel -tag PN -gc ref,alt,af -vs 1 [#dummybuck#] [#pns#] | map -c pn [#bucket#] | rename value gt | gtgen -gc ref,alt,af [#bucket#] <(gorrows -p chr1:1-2 | group chrom | calc pn '' | top 0) | signature -timeres 100;\n" +
                "\n" +
                "create #king# = nor <(gor [#vars#] | king -gc af [#bucket#] [#pns1#] [#pns2#] -vs 1 /* -phithr -1.0 */) | select pn1- | signature -timeres 100;\n" +
                "\n" +
                "create #king2# = nor <(gor [#vars#]\n" +
                "| calc tpq 2*af*af*(1.0-af)*(1.0-af)\n" +
                "| calc kpq 2.0*af*(1-af)\n" +
                "| csvsel -gc ref,alt,tpq,kpq -vs 1 -u 3 -tag PN [#bucket#] [#pns1#]\n" +
                "| where value != '3'\n" +
                "| multimap -c pn <(nor [#rel#] | select id1,id2)\n" +
                "| varjoin -norm -r -xl id2 -xr pn <(gor [#vars#]\n" +
                "  | csvsel -gc ref,alt -vs 1 -u 3 -tag PN [#bucket#] [#pns2#]\n" +
                "  | where value != '3')\n" +
                "| calc values = str(value)+str(valuex)\n" +
                "| calc IBS0 if(values='02' or values = '20',1,0)\n" +
                "| calc XX if(values='01' or values = '10' or values = '21' or values = '12',1,if(values='02' or values = '20',4,0))\n" +
                "| calc Nhet if(values = '11',1,0)\n" +
                "| calc Nhom if(values = '02' or values = '20',1,0)\n" +
                "| calc NAai if(left(values,1)='1',1,0)\n" +
                "| calc NAaj if(right(values,1)='1',1,0)\n" +
                "| group genome -gc pn,pnx -sum -fc IBS0,XX,tpq,kpq,Nhet,Nhom,NAai,NAaj\n" +
                "| rename pn pn1 | rename pnx pn2\n" +
                "| rename sum_(.*pq) #{1}\n" +
                "| replace sum_* int(float(#rc))\n" +
                "| rename sum_(.*) #{1})\n" +
                "| select pn1-\n" +
                ";\n" +
                "\n" +
                "nor [#king#]\n" +
                "| select 1-count[-1]\n" +
                "| merge [#king2#] -s\n" +
                "| replace tpq form(tpq,4,4)\n" +
                "| replace kpq form(kpq,4,4)\n" +
                "| group -gc 1-source[-1] -count -sc source -lis\n" +
                "| sort -c pn1,pn2\n" +
                "| throwif allcount != 2";
        TestUtils.runGorPipe(query);
    }
}
