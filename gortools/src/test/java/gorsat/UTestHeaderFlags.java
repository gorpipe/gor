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

import gorsat.Utilities.IteratorUtilities;
import org.gorpipe.exceptions.GorDataException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

public class UTestHeaderFlags {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(UTestHeaderFlags.class);

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    /**
     * If an excluded char becomes included after the initial state in gorsat.Utilities.IteratorUtilities#validHeader
     * this test will throw an exception.
     * Creates a temporary nor file with excluded escape characters in the first data row and no header flag '#'.
     * The test then runs nor -h on this file and tests if the metacharacters are correctly
     * replaced and are not malformed when passed forth to subsequent commands.
     *
     * @throws Exception
     * @implSpec The default implementation will cover only explicitly excluded escape characters: .\/*+-'$;,
     * @implNote This implementation does not test every combination of subsequent commands.
     */
    @Test
    public void testNorHeaderWithFirstRowEscapeCharacters() throws Exception {
        File norFile = File.createTempFile("norFile", "WithExcludedCharsInFirstRow");
        String excludedChars = "testColName.\\/*+-'$;,\ntestColValue";
        Writer fileWriter = new FileWriter(norFile, true);
        fileWriter.write(excludedChars);
        try {
            String norQueryResult = gorsat.TestUtils.runGorPipe( String.format(" nor -h %s | top 1 | unpivot 1- ", norFile));
            Assert.assertEquals("ChromNOR\tPosNOR\tCol_Name\tCol_Value\n" +
                    "chrN\t0\ttestColNamexxxxxxxxxx\ttestColValue\n", norQueryResult);
        } catch (Exception e) {
            log.debug("Excluded metacharacters are included in nor file header : " + excludedChars);
            e.printStackTrace();
        }
        norFile.deleteOnExit();
    }

    @Test
    public void testValidHeaderUsedKeywordsWithDupAllowingDup() {
        String testHeader = "#abc\tstart\tfrom\tselect\tmax\tmin\tfrom\tgroup\trange\torder\trank\torder";
        String resultingHeader = IteratorUtilities.validHeader(testHeader, true);
        Assert.assertEquals("#abc\tstart\tfrom\tselect\tmax\tmin\tfromx\tgroup\trange\torder\trank\torderx", resultingHeader);
        Assert.assertFalse(systemErrRule.getLog().contains("Duplicate column name 'from'"));
        Assert.assertFalse(systemErrRule.getLog().contains("Duplicate column name 'order'"));
    }

    @Test
    public void testValidHeaderUsedKeywordsWithDupNotAllowingDup() {
        String testHeader = "#abc\tstart\tfrom\tselect\tmax\tmin\tfrom\tgroup\trange\torder\trank\torder";
        Assert.assertThrows(GorDataException.class, () -> IteratorUtilities.validHeader(testHeader, false));
    }

    @Ignore("Only works if run alone, as the property is read only on class load")
    @Test
    public void testValidHeaderUsedKeywordsWithDupAllowingDupOnlyGlobal() {
        System.setProperty("gor.iterators.allowDuplicateColumns", "true");
        String testHeader = "#abc\tstart\tfrom\tselect\tmax\tmin\tfrom\tgroup\trange\torder\trank\torder";
        String resultingHeader = IteratorUtilities.validHeader(testHeader, false);
        Assert.assertEquals("#abc\tstart\tfrom\tselect\tmax\tmin\tfromx\tgroup\trange\torder\trank\torderx", resultingHeader);
        Assert.assertTrue(systemErrRule.getLog().contains("Duplicate column name 'from'"));
        Assert.assertTrue(systemErrRule.getLog().contains("Duplicate column name 'order'"));
    }

    @Test
    public void testHeaderParsing() {
        String testStr = "test$He*aDer";
        String resultingHeader = IteratorUtilities.validHeader(testStr, false);
        Assert.assertEquals("testxHexaDer", resultingHeader);
    }
}