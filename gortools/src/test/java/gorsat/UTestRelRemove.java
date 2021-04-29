package gorsat;

import org.gorpipe.test.SlowTests;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTests.class)
public class UTestRelRemove {
    @Test
    public void testRelRemove() {
        String query = "create #pns# = norrows 10001 | calc pn #1 | select pn;\n" +
                "create #r# = nor [#pns#] | multimap -cartesian <(norrows 100 | group -lis -sc #1) | replace #2 listfilter(listmap(#2,'round(10000*random())'),'random()<0.05') | rename #1 pn1 | rename #2 pn2 | split pn2 | where pn2 != '' and pn1 != pn2;\n" +
                "create #pheno# = nor [#pns#] | calc pheno1 if(random()<0.01,'NA',str(random())) | calc pheno2 mod(pn,3) | calc pheno3 pheno1 | calc pheno4 decode(pheno2,'0,NA,1,0,2,1') | calc pheno5 decode(pheno2,'0,-9,1,0,2,1');\n" +
                "/* Test if identical columns are treated in same way */\n" +
                "create #t1# = nor [#pheno#] | relremove [#r#] -rsymb hakon | throwif pheno3 != pheno1  | top 1;\n" +
                "/* Test if identical columns are treated in same way with -sepcc option */\n" +
                "create #t2# = nor [#pheno#] | relremove [#r#] -rsymb hakon -sepcc  | throwif pheno3 != pheno1  | top 1;\n" +
                "/* Test if identical columns are treated in same way with no option */\n" +
                "create #t3# = nor [#pheno#] | relremove [#r#] | throwif pheno3 != pheno1  | top 1;\n" +
                "/* Test if elim counts of identical columns are the same and that -sepcc option reduces the number of eliminated rows for case-control */\n" +
                "create #t4# = nor [#pheno#] | relremove [#r#] -rsymb elim | unpivot 2- | where col_value = 'elim' | group -gc col_name -count\n" +
                "| pivot col_name -v pheno1,pheno2,pheno3,pheno4,pheno5 | rename (.*)_allcount #{1}\n" +
                "| multimap -cartesian <(nor [#pheno#] | relremove [#r#] -sepcc -rsymb elim | unpivot 2- | where col_value = 'elim' | group -gc col_name -count\n" +
                "| pivot col_name -v pheno1,pheno2,pheno3,pheno4,pheno5 | rename (.*)_allcount #{1})\n" +
                "| throwif pheno1 != pheno3 or pheno2!=pheno4 or pheno2 != pheno5 or pheno2 != pheno5 or pheno2 < pheno2x or pheno4 < pheno4x or pheno5 < pheno5x;\n" +
                "/* Test if there are relatives after elimination */\n" +
                "create #t5# = nor [#pheno#] | select pn,pheno1 | relremove [#r#] -rsymb elim | where pheno1 != 'elim' and pheno1 != 'NA' | multimap -c pn [#r#] | multimap -c pn2 <(nor [#pheno#] | select pn,pheno1 | relremove [#r#] -rsymb elim | where pheno1 != 'elim' and pheno1 != 'NA') | throwif 2=2;\n" +
                "/* Test if there are relatives after elimination within either case or ctrl groups */\n" +
                "create #t6# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim | where pheno4 != 'elim' and pheno4 != 'NA' | multimap -c pn [#r#] | multimap -c pn2 <(nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim  | where pheno4 != 'elim' and pheno4 != 'NA') | throwif 2=2;\n" +
                "/* Test if there are relatives after elimination within same case-ctrl group */\n" +
                "create #t7# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim -sepcc | where pheno4 != 'elim' and pheno4 != 'NA' | multimap -c pn [#r#] | multimap -c pn2 <(nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim -sepcc  | where pheno4 != 'elim' and pheno4 != 'NA') | where pheno4 = pheno4x | throwif 2=2;\n" +
                "/* Test if there are fewer eliminations than with a simple method */\n" +
                "create #t8# = nor [#pheno#] | select pn,pheno4 | relremove [#r#] -rsymb elim | where pheno4 = 'elim' | group -count | calc method 'relremove'\n" +
                "| merge <(nor [#pheno#] | select pn,pheno4 | inset -c pn -b <(nor [#r#] | calc pn pn1+','+pn2 | select pn | split pn ) | where inset = 1 | group -count | calc method 'simple')\n" +
                "| pivot method -v relremove,simple | throwif relremove_allcount > simple_allcount;\n" +
                "/* Check that samples with the max number of relatives are eliminated and that samples with no relatives are kept */\n" +
                "create #t9# = nor [#pheno#] | select pn | calc pheno random() | relremove [#r#] -rsymb elim | map -c pn -m 1000 <(nor [#r#] | calc pn pn1+','+pn2 | select pn | split pn | group -gc pn -count | rank allcount -o desc) | throwif rank_allcount > 0 and rank_allcount < 5 and pheno != 'elim' or allcount = 0 and pheno = 'elim' | top 10;\n" +
                "nor [#t9#] | top 0";

        String result = TestUtils.runGorPipe(query);
        Assert.assertEquals("Wrong results from relremove",result,"ChromNOR\tPosNOR\tpn\tpheno\tallCount\trank_allCount\n");
    }
}
