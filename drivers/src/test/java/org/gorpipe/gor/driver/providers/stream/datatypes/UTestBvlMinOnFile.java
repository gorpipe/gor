package org.gorpipe.gor.driver.providers.stream.datatypes;

import org.gorpipe.gor.driver.utils.TestUtils;

public class UTestBvlMinOnFile extends BvlTestSuite {

    public UTestBvlMinOnFile() {
        // TODO Auto-generated constructor stub
    }

    @Override
    protected String getSourcePath(String name) {
        return TestUtils.getTestFile("bvl_min/" + name);
    }

}
