package gorsat.Commands;

import gorsat.TestUtils;
import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UTestCommandParseUtilities {

    @Test
    public void parseNestedCommand() {
        final String result = CommandParseUtilities.parseNestedCommand("<(gor test.gor)");
        assertEquals("gor test.gor", result);
    }

    @Test
    public void parseNestedCommand_whenCommandHasSpace() {
        final String result = CommandParseUtilities.parseNestedCommand("<( gor test.gor )");
        assertEquals("gor test.gor", result);
    }

    @Test
    public void parseNestedCommand_whenCommandHasNewline() {
        final String result = CommandParseUtilities.parseNestedCommand("<(\ngor test.gor\n)");
        assertEquals("gor test.gor", result);
    }

    @Test
    public void replaceSingleQuoutes() {
        assertEquals("bingo", CommandParseUtilities.replaceSingleQuotes("bingo"));
        assertEquals("bingo", CommandParseUtilities.replaceSingleQuotes("\"bingo\""));
        assertEquals("bingo", CommandParseUtilities.replaceSingleQuotes("'bingo'"));
        assertEquals("'bingo'", CommandParseUtilities.replaceSingleQuotes("\"'bingo'\""));
        assertEquals("\"bingo\"", CommandParseUtilities.replaceSingleQuotes("'\"bingo\"'"));
        assertEquals("'bingo", CommandParseUtilities.replaceSingleQuotes("'bingo"));
        assertEquals("bingo'", CommandParseUtilities.replaceSingleQuotes("bingo'"));
        assertEquals("\"bingo", CommandParseUtilities.replaceSingleQuotes("\"bingo"));
        assertEquals("bingo\"", CommandParseUtilities.replaceSingleQuotes("bingo\""));
        assertEquals("'bingo\"", CommandParseUtilities.replaceSingleQuotes("'bingo\""));
    }

    @Test
    public void quoteSafeSplit() {
        assertArrayEquals(new String[]{}, CommandParseUtilities.quoteSafeSplit("", ' '));
        assertArrayEquals(new String[]{"bingo"}, CommandParseUtilities.quoteSafeSplit("bingo", ' '));
        assertArrayEquals(new String[]{"bingo", "bongo"}, CommandParseUtilities.quoteSafeSplit("bingo bongo", ' '));
        assertArrayEquals(new String[]{"'bingo bongo'"}, CommandParseUtilities.quoteSafeSplit("'bingo bongo'", ' '));
        assertArrayEquals(new String[]{"'bingo \"bongo'"}, CommandParseUtilities.quoteSafeSplit("'bingo \"bongo'", ' '));
        assertArrayEquals(new String[]{"'bingo \"'\"bongo'"}, CommandParseUtilities.quoteSafeSplit("'bingo \"'\"bongo'", ' '));
        assertArrayEquals(new String[]{"replace(replace(disease,\"'\",\"\"),' ','_')"}, CommandParseUtilities.quoteSafeSplit("replace(replace(disease,\"'\",\"\"),' ','_')", ' '));
        assertArrayEquals(new String[]{"\"abcd\\\"1234\""}, CommandParseUtilities.quoteSafeSplit("\"abcd\\\"1234\"", ' '));
        assertArrayEquals(new String[]{"\"abcd\\\"1234\"", "bongo"}, CommandParseUtilities.quoteSafeSplit("\"abcd\\\"1234\" bongo", ' '));
    }

    @Test
    public void quoteSafeIndexOf() {
        assertEquals(4, CommandParseUtilities.quoteSafeIndexOf("gor foo", "foo", false, 0));
        assertEquals(-1, CommandParseUtilities.quoteSafeIndexOf("gor 'foo'", "foo", false, 0));
        assertEquals(5, CommandParseUtilities.quoteSafeIndexOf("gor (foo)", "foo", false, 0));
    }

    @Test
    public void testQuoteSafeIndexOf() {
        String inputString = "gor [gorgrid: Query_1]";
        String searchString = "[gorgrid: Query_1]";
        int index = CommandParseUtilities.quoteSafeIndexOf(inputString, searchString, false, 0);

        Assert.assertEquals(4, index);
    }

    @Test
    public void testQuoteSafeIndexOfQuote() {
        String inputString = "gor [gorgrid: 'Query_1']";
        String searchString = "[gorgrid: 'Query_1']";
        int index = CommandParseUtilities.quoteSafeIndexOf(inputString, searchString, false, 0);

        Assert.assertEquals(4, index);
    }

    @Test
    public void testQuoteSafeSplitWithQuery() {
        String query = "norrows 1 | calc disease 'test\\'test' | replace disease replace(replace(disease,\"'\",\"\"),' ','_')";

        String resultSemicolon[] = CommandParseUtilities.quoteSafeSplitAndTrim(query, ';');
        String resultSpace[] = CommandParseUtilities.quoteSafeSplitAndTrim(query, ' ');

        Assert.assertEquals(query, resultSemicolon[0]);
        Assert.assertEquals(10, resultSpace.length);
        Assert.assertEquals("norrows", resultSpace[0]);
    }

    @Test
    public void testQuoteSafeSplitByRunningQuery() {
        String query = "norrows 1 | calc disease 'test\\'test' | replace disease replace(replace(disease,\"'\",\"\"),' ','_')";
        String results = TestUtils.runGorPipe(query);
        Assert.assertEquals("ChromNOR\tPosNOR\tRowNum\tdisease\n" +
                "chrN\t0\t0\ttesttest\n", results);
    }

    @Test(expected = GorParsingException.class)
    public void testQuoteSafeSplitWithQueryExceptionQuote() {
        String query = "norrows 1 | calc disease 'test\\'test' | replace disease replace(replace(disease,\"'\" ',\"\"),' ','_')";
        CommandParseUtilities.quoteSafeSplitAndTrim(query, ';');
    }

    @Test(expected = GorParsingException.class)
    public void testQuoteSafeSplitWithQueryExceptionDoubleQuote() {
        String query = "norrows 1 | calc disease 'test\\'test' | replace disease replace(replace(disease,\"'\" \",\"\"),' ','_')";
        CommandParseUtilities.quoteSafeSplitAndTrim(query, ';');
    }

    @Test(expected = GorParsingException.class)
    public void testQuoteSafeSplitWithQueryExceptionBracket() {
        String query = "norrows 1 | calc disease 'test\\'test' | replace disease replace(replace(disease,\"'\",\"\"),' ','_') )";
        CommandParseUtilities.quoteSafeSplitAndTrim(query, ';');
    }

    @Test
    public void testQuoteSafeSplitWithScript() {
        String script = "create ##first## = pgor ../tests/data/gor/genes.gor | signature -timeres 1;" +
                "create ##second## = pgor [##first##] | signature -timeres 1;" +
                "gor [##second##]";

        String resultSemicolon[] = CommandParseUtilities.quoteSafeSplitAndTrim(script, ';');

        Assert.assertEquals("create ##first## = pgor ../tests/data/gor/genes.gor | signature -timeres 1", resultSemicolon[0]);
        Assert.assertEquals("create ##second## = pgor [##first##] | signature -timeres 1", resultSemicolon[1]);
        Assert.assertEquals("gor [##second##]", resultSemicolon[2]);
    }

    @Test
    public void testQuoteSafeSplitWithWindowsPaths() {
        String input = "abc c:\\test\\this";
        String result[] = CommandParseUtilities.quoteSafeSplit(input, ' ');
        Assert.assertEquals("abc", result[0]);
        Assert.assertEquals("c:\\test\\this", result[1]);
    }

    @Test
    public void testGetExtensionForQueryEmptyString() {
        String result = CommandParseUtilities.getExtensionForQuery("", false);
        Assert.assertEquals(CommandParseUtilities.DEFAULT_EXTENSION(), result);
    }

    @Test
    public void testGetExtensionForQueryGor() {
        String result = CommandParseUtilities.getExtensionForQuery("gor abc.gor", false);
        Assert.assertEquals(CommandParseUtilities.DEFAULT_EXTENSION(), result);
    }

    @Test
    public void testGetExtensionForQueryGorHeader() {
        String result = CommandParseUtilities.getExtensionForQuery("gor abc.gor", true);
        Assert.assertEquals(".header.gor", result);
    }

    @Test
    public void testGetExtensionForQueryNor() {
        String result = CommandParseUtilities.getExtensionForQuery("nor abc.gor", false);
        Assert.assertEquals(CommandParseUtilities.TSV_EXTENSION(), result);
    }

    @Test
    public void testGetExtensionForQueryNorHeader() {
        String result = CommandParseUtilities.getExtensionForQuery("nor abc.gor", true);
        Assert.assertEquals(".header.tsv", result);
    }

    @Test
    public void testGetExtensionForQueryNorWithTogor() {
        String result = CommandParseUtilities.getExtensionForQuery("nor abc.gor | togor", false);
        Assert.assertEquals(CommandParseUtilities.DEFAULT_EXTENSION(), result);
    }

    @Test
    public void testGetExtensionForQueryNorHeaderWithTogor() {
        String result = CommandParseUtilities.getExtensionForQuery("nor abc.gor | togor", true);
        Assert.assertEquals(".header.gor", result);
    }
}