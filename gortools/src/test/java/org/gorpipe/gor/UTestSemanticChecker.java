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

package org.gorpipe.gor;

import org.gorpipe.exceptions.GorParsingException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UTestSemanticChecker {
    private SemanticChecker checker;

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Before
    public void setUp() {
        checker = new SemanticChecker();
    }

    @Test
    public void basic() {
        checker.validate("gor 1.mem");
    }

    @Test
    public void invalidCommand() {
        expected.expect(GorParsingException.class);
        expected.expectMessage("Invalid command BINGO");
        checker.validate("gor 1.mem | bingo");
    }

    @Test
    public void invalidCommandInNestedQuery() {
        expected.expect(GorParsingException.class);
        expected.expectMessage("Invalid command BINGO");
        checker.validate("gor 1.mem | join <(gor 2.mem | bingo)");
    }

    @Test
    public void commandWithInvalidOption() {
        expected.expect(GorParsingException.class);
        expected.expectMessage("Invalid arguments");
        checker.validate("gor 1.mem | top -z -a -b");
    }

    @Test
    public void dualCreateStatementsNoDuplicates() {
        checker.validate("create x = gor 1.mem; create y = gor 2.mem; gor [x] [y]");

    }
    
    @Test
    public void duplicateCreateStatement() {
        expected.expect(GorParsingException.class);
        expected.expectMessage("Duplicate name");
        checker.validate("create x = gor 1.mem; create x = gor 2.mem; gor [x] [y]");
    }

    @Test
    public void createStatementNotReferenced() {
        expected.expect(GorParsingException.class);
        expected.expectMessage("never referenced");
        checker.validate("create x = gor 1.mem; create y = gor 2.mem; gor [x]");
    }
}