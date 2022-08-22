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
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UTestTypeAndStringConversion {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testStr() {
        TestUtils.assertCalculated("str(5)", "5");
        TestUtils.assertCalculated("str(-1000)", "-1000");
        TestUtils.assertCalculated("str(123456789)", "123456789");
        TestUtils.assertCalculated("str(3.1415926535)", "3.1415926535");
        TestUtils.assertCalculated("str(1+2+3)", "6");
    }

    @Test
    public void testString() {
        TestUtils.assertCalculated("string(5)", "5");
        TestUtils.assertCalculated("string(-1000)", "-1000");
        TestUtils.assertCalculated("string(123456789)", "123456789");
        TestUtils.assertCalculated("string(3.1415926535)", "3.1415926535");
    }

    @Test
    public void testInt() {
        TestUtils.assertCalculated("int(3.1415926535)", "3");
        TestUtils.assertCalculated("int('3')", "3");
        TestUtils.assertCalculated("int(1+3.14)", "4");
        TestUtils.assertCalculated("int(1+2+3)", "6");
    }

    @Test
    public void testLong() {
        TestUtils.assertCalculated("long(3.1415926535)", "3");
        TestUtils.assertCalculated("long(3)", "3");
        TestUtils.assertCalculated("long(1234567890123456789)", "1234567890123456789");
        TestUtils.assertCalculated("long(-1234567890123456789)", "-1234567890123456789");
        TestUtils.assertCalculated("long('1234567890123456789')", "1234567890123456789");
    }

    @Test
    public void castToLongWhenFloatTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("long(3e300)", "");
    }

    @Test
    public void castToLongWhenNegativeFloatTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("long(-3e300)", "");
    }

    @Test
    public void castToIntWhenLongTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("int(1234567890123456789)", "");
    }

    @Test
    public void castToIntWhenNegativeLongTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("int(-1234567890123456789)", "");
    }

    @Test
    public void castToIntWhenFloatTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("int(3e300)", "");
    }

    @Test
    public void castToIntWhenNegativeFloatTooLarge() {
        exception.expect(GorDataException.class);
        TestUtils.assertCalculated("int(-3e300)", "");
    }

    @Test
    public void testFloat() {
        TestUtils.assertCalculated("float('3')", 3.0);
        TestUtils.assertCalculated("float('-99.9')", -99.9);
        TestUtils.assertCalculated("float('1.23456789')", 1.23456789);
        TestUtils.assertCalculated("float('bull', -1.0)", -1.0);
        TestUtils.assertCalculated("float('5.4321', -1.0)", 5.4321);
        TestUtils.assertCalculated("float(1.23456789)", 1.23456789);
        TestUtils.assertCalculated("float(1+2+3)", 6.0);
        TestUtils.assertCalculated("float(1+2+3.14)", 6.14);
    }

    @Test
    public void testFloatNaN() {
        Assert.assertTrue( Float.isNaN(Float.parseFloat(TestUtils.getCalculated("float('bull', NaN)"))) );
    }

    @Test
    public void testNumber() {
        TestUtils.assertCalculated("number('3')", 3.0);
        TestUtils.assertCalculated("number('-99.9')", -99.9);
        TestUtils.assertCalculated("number('1.23456789')", 1.23456789);
        TestUtils.assertCalculated("number(1.23456789)", 1.23456789);
        TestUtils.assertCalculated("number(1+2+3)", 6.0);
        TestUtils.assertCalculated("number(1+2+3.14)", 6.14);
    }

    @Test
    public void testBase26() {
        TestUtils.assertCalculated("base26(0)", 0);
        TestUtils.assertCalculated("base26(57)", 25);
        TestUtils.assertCalculated("base26(-676)", -100);
        TestUtils.assertCalculated("base26(19010)", 1234);
    }

    @Test
    public void testBasePN() {
        TestUtils.assertCalculated("basepn(1)", "AAAAAAA");
        TestUtils.assertCalculated("basepn(2)", "AAAAAAB");
        TestUtils.assertCalculated("basepn(24)", "AAAAAAX");
        TestUtils.assertCalculated("basepn(26)", "AAAAAAZ");
        TestUtils.assertCalculated("basepn(74)", "AAAAACV");
        TestUtils.assertCalculated("basepn(12345)", "AAAASGU");
        TestUtils.assertCalculated("basepn(321272408)", "BBBBBBB");
    }

    @Test
    public void testLen() {
        TestUtils.assertCalculated("len('')", 0);
        TestUtils.assertCalculated("len('test123')", 7);
        TestUtils.assertCalculated("len('Hello World!')", 12);
    }

    @Test
    public void testReverse() {
        TestUtils.assertCalculated("reverse('')", "");
        TestUtils.assertCalculated("reverse('test123')", "321tset");
        TestUtils.assertCalculated("reverse('Hello World!')", "!dlroW olleH");
    }

    @Test
    public void testTrim() {
        TestUtils.assertCalculated("trim('   ')", "");
        TestUtils.assertCalculated("trim('  test123 ')", "test123");
        TestUtils.assertCalculated("trim(' Hello World! ')", "Hello World!");
    }

    @Test
    public void testMD5() {
        TestUtils.assertCalculated("md5('')", "9c9c5amjfc5a991bkm8dhc8mdjg");
        TestUtils.assertCalculated("md5('test123')", "b5lkbijci42g01daa1eab3akod9");
        TestUtils.assertCalculated("md5('Hello World!')", "42d1ggmpmmfk75c6igcpd70nfpm");
    }

    @Test
    public void testIOOA() {
        TestUtils.assertCalculated("iooa('test')", "1");
    }

    @Test
    public void testUpper() {
        TestUtils.assertCalculated("upper('')", "");
        TestUtils.assertCalculated("upper('test123')", "TEST123");
        TestUtils.assertCalculated("upper('Hello World!')", "HELLO WORLD!");
    }

    @Test
    public void testUpperWithColumnReference() {
        TestUtils.assertCalculated("upper(chromo)", "CHR1");
        TestUtils.assertCalculated("upper(pos)", "0");
    }


    @Test
    public void testLower() {
        TestUtils.assertCalculated("lower('')", "");
        TestUtils.assertCalculated("lower('TEST123')", "test123");
        TestUtils.assertCalculated("lower('Hello World!')", "hello world!");
    }

    @Test
    public void testLeft() {
        TestUtils.assertCalculated("left('CALC',0)", "");
        TestUtils.assertCalculated("left('test123',4)", "test");
        TestUtils.assertCalculated("left('Hello World!',100)", "Hello World!");
    }

    @Test
    public void testRight() {
        TestUtils.assertCalculated("right('CALC',0)", "");
        TestUtils.assertCalculated("right('test123',3)", "123");
        TestUtils.assertCalculated("right('Hello World!',100)", "Hello World!");
    }

    @Test
    public void testSubstr() {
        TestUtils.assertCalculated("substr('CALC',3,3)", "");
        TestUtils.assertCalculated("substr('test123',2,6)", "st12");
        TestUtils.assertCalculated("substr('newtest',3,4)", "t");
        TestUtils.assertCalculated("substr('Hello World!',0,100)", "Hello World!");
        TestUtils.assertCalculated("substr('New Hello!',0,9)", "New Hello");
    }

    @Test
    public void testMid() {
        TestUtils.assertCalculated("mid('CALC',3,0)", "");
        TestUtils.assertCalculated("mid('test123',2,5)", "st123");
        TestUtils.assertCalculated("mid('newtest',3,4)", "test");
        TestUtils.assertCalculated("mid('Hello World!',0,100)", "Hello World!");
        TestUtils.assertCalculated("mid('New Hello!',0,9)", "New Hello");
    }

    @Test
    public void testReplace() {
        TestUtils.assertCalculated("replace('CALC','C','')", "AL");
        TestUtils.assertCalculated("replace('test123','e','oa')", "toast123");
        TestUtils.assertCalculated("replace('newtest','x','y')", "newtest");
        TestUtils.assertCalculated("replace('Hello World!','World','Test')", "Hello Test!");
        TestUtils.assertCalculated("replace('New Hello!','','.')", ".N.e.w. .H.e.l.l.o.!.");
    }

    @Test
    public void testPosOf() {
        TestUtils.assertCalculated("posof('CALC','C')", 0);
        TestUtils.assertCalculated("posof('test123','1')", 4);
        TestUtils.assertCalculated("posof('newtest','123')", -1);
        TestUtils.assertCalculated("posof('Hello World!','World')", 6);
        TestUtils.assertCalculated("posof('New Hello!','New Hello!')", 0);
    }

    @Test
    public void testChar() {
        TestUtils.assertCalculated("char('abc', 1)", 98);
        TestUtils.assertCalculated("char('ABC', 2)", 67);
    }

    @Test
    public void testBrackets() {
        TestUtils.assertCalculated("brackets('')", "()");
        TestUtils.assertCalculated("brackets('test123')", "(test123)");
        TestUtils.assertCalculated("brackets('Hello World!')", "(Hello World!)");
    }

    @Test
    public void testUnbracket() {
        TestUtils.assertCalculated("unbracket('()')", "");
        TestUtils.assertCalculated("unbracket('test123')", "test123");
        TestUtils.assertCalculated("unbracket('(Hello World!)')", "Hello World!");
    }

    @Test
    public void testSquote() {
        TestUtils.assertCalculated("squote('')", "''");
        TestUtils.assertCalculated("squote(\"'\")", "'\\''");
        TestUtils.assertCalculated("squote('\"')", "'\"'");
        TestUtils.assertCalculated("squote('bingo')", "'bingo'");
        TestUtils.assertCalculated("squote(\"this 'is' bingo\")", "'this \\'is\\' bingo'");
        TestUtils.assertCalculated("squote('\\'bingo\\'')", "'\\'bingo\\''");
    }

    @Test
    public void testSunquote() {
        TestUtils.assertCalculated("sunquote('')", "");
        TestUtils.assertCalculated("sunquote(\"'\")", "'");
        TestUtils.assertCalculated("sunquote('\\'')", "'");
        TestUtils.assertCalculated("sunquote(\"'\\''\")", "'");
        TestUtils.assertCalculated("sunquote(\"'\\\\'\")", "'\\'");
        TestUtils.assertCalculated("sunquote('bingo\\'')", "bingo'");
        TestUtils.assertCalculated("sunquote('\\'bingo\\'')", "bingo");
    }

    @Test
    public void testDquote() {
        TestUtils.assertCalculated("dquote('')", "\"\"");
        TestUtils.assertCalculated("dquote('\"')", "\"\\\"\"");
        TestUtils.assertCalculated("dquote('bingo')", "\"bingo\"");
        TestUtils.assertCalculated("dquote('this \"is\" bingo')", "\"this \\\"is\\\" bingo\"");
    }

    @Test
    public void testDunquote() {
        TestUtils.assertCalculated("dunquote('')", "");
        TestUtils.assertCalculated("dunquote('\"')", "\"");
        TestUtils.assertCalculated("dunquote('\\'')", "'");
        TestUtils.assertCalculated("dunquote('bingo\"')", "bingo\"");
        TestUtils.assertCalculated("dunquote('\"bingo\"')", "bingo");
    }

    @Test
    public void testForm() {
        TestUtils.assertCalculated("form(5,5,3)", "5.000");
        TestUtils.assertCalculated("form(123,5,5)", "123.00000");
        TestUtils.assertCalculated("form(1000,10,1)", "    1000.0");
        TestUtils.assertCalculated("form(0.999,7,5)", "0.99900");
        TestUtils.assertCalculated("form(3.1415926535,4,2)", "3.14");
    }

    @Test
    public void testEForm() {
        TestUtils.assertCalculated("eform(5,5,3)", "5.000e+00");
        TestUtils.assertCalculated("eform(123,5,5)", "1.23000e+02");
        TestUtils.assertCalculated("eform(1000,10,1)", "   1.0e+03");
        TestUtils.assertCalculated("eform(0.999,7,5)", "9.99000e-01");
        TestUtils.assertCalculated("eform(3.1415926535,4,2)", "3.14e+00");
        TestUtils.assertCalculated("eform(1+3+5,4,2)", "9.00e+00");
        TestUtils.assertCalculated("gform(3.1415926535,1,4)", "3.142");
        TestUtils.assertCalculated("gform(1234567890,1,4)", "1.235e+09");
    }

    @Test
    public void testGForm() {
        TestUtils.assertCalculated("gform(5,5,3)", " 5.00");
        TestUtils.assertCalculated("gform(123,5,5)", "123.00");
        TestUtils.assertCalculated("gform(1000,10,1)", "     1e+03");
        TestUtils.assertCalculated("gform(0.999,7,5)", "0.99900");
        TestUtils.assertCalculated("gform(3.1415926535,4,2)", " 3.1");
        TestUtils.assertCalculated("gform(1+3+5,4,2)", " 9.0");
        TestUtils.assertCalculated("gform(3.1415926535,1,4)", "3.142");
        TestUtils.assertCalculated("gform(1234567890,1,4)", "1.235e+09");
    }

    @Test
    public void testNanFormattingFloat() {
        String gorcmd = "gorrow chr1,1,2 | calc a NaN | calc c form(a, 10,4)";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(gorcmd);
        Assert.assertEquals(1, lines.length);
        Assert.assertEquals("chr1\t1\t2\tNaN\tNaN\n", lines[0]);
    }

    @Test
    public void testNanFormattingScientific() {
        String gorcmd = "gorrow chr1,1,2 | calc a NaN | calc c eform(a, 10,4)";
        String[] lines = TestUtils.runGorPipeLinesNoHeader(gorcmd);
        Assert.assertEquals(1, lines.length);
        Assert.assertEquals("chr1\t1\t2\tNaN\tNaN\n", lines[0]);
    }

    @Test
    public void testRegSel() {
        TestUtils.assertCalculated("regsel('', '')", "");
        TestUtils.assertCalculated("regsel('34xx27xx876', '\\\\d+xx(\\\\d+)xx\\\\d+')", "27");
    }

    @Test
    public void testStr2List() {
        TestUtils.assertCalculated("str2list('', 3)", "");
        TestUtils.assertCalculated("str2list('this is a test', 3)", "thi,s i,s a, te,st");
        TestUtils.assertCalculated("str2list('this is a test', 3, ' ')", "thi s i s a  te st");
    }
}
