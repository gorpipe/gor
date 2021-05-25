package gorsat;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Assert;
import org.junit.Test;

public class UTestRecursiveDef {
    @Test
    public void testRecursiveDef() {
        var query = "def ##sample## = gor ##sample##; ##sample##";
        try {
            TestUtils.runGorPipe(query);
            Assert.fail("The recusive def query should fail");
        } catch(GorParsingException e) {
            Assert.assertEquals("Replace string contains the definition itself", e.getMessage());
        }
    }
}
