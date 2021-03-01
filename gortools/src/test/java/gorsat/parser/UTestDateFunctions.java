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
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.time.temporal.ChronoUnit.SECONDS;

public class UTestDateFunctions {
    @Test
    public void testDateNoArgs() {
        LocalDateTime before = LocalDateTime.now().truncatedTo(SECONDS);
        String dateString = TestUtils.getCalculated("date()");
        LocalDateTime then = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime now = LocalDateTime.now();

        Assert.assertTrue(then.compareTo(before) >= 0);
        Assert.assertTrue(then.compareTo(LocalDateTime.now()) <= 0);
    }

    @Test
    public void testDate() {
        String before = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String dateString = TestUtils.getCalculated("date('dd/MM/yyyy')");
        String after = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Guard against the test failing if it is run just around midnight
        Assert.assertTrue(after.equals(dateString) || before.equals(dateString));
    }

    @Test
    public void testEdateDefaultFormat() {
        String dateString = TestUtils.getCalculated("edate(1497571200000)");
        Assert.assertEquals("2017-06-16 00:00:00", dateString);
    }

    @Test
    public void testEdateGivenFormat() {
        String dateString = TestUtils.getCalculated("edate(1497571200000, 'dd/MM/yyyy')");
        Assert.assertEquals("16/06/2017", dateString);
    }

    @Test
    public void testEpoch() {
        String epochAsString = TestUtils.getCalculated("epoch()");
        long epoch = Long.parseLong(epochAsString);
        long now = new Date().getTime();
        long d = now - epoch;
        Assert.assertTrue(d < 1000);
    }

    @Test
    public void testEpochGivenTimeAndFormat() {
        String epochAsString = TestUtils.getCalculated("epoch('16/06/2017','dd/MM/yyyy')");
        long epoch = Long.parseLong(epochAsString);
        Assert.assertEquals(1497571200000L, epoch);
    }

    @Test
    public void testDaydiffSameDate() {
        String result = TestUtils.getCalculated("daydiff('dd/MM/yyyy', '16/06/2017', '16/06/2017')");
        Assert.assertEquals("0", result);
    }

    @Test
    public void testDaydiffFirstDateEarlier() {
        String result = TestUtils.getCalculated("daydiff('dd/MM/yyyy', '16/06/2017', '18/06/2017')");
        Assert.assertEquals("2", result);
    }

    @Test
    public void testDaydiffFirstDateLater() {
        String result = TestUtils.getCalculated("daydiff('dd/MM/yyyy', '23/06/2017', '18/06/2017')");
        Assert.assertEquals("-5", result);
    }

    @Test
    public void testMonthdiffSameDate() {
        String result = TestUtils.getCalculated("monthdiff('dd/MM/yyyy', '16/06/2017', '16/06/2017')");
        Assert.assertEquals("0", result);
    }

    @Test
    public void testMonthdiffFirstDateEarlier() {
        String result = TestUtils.getCalculated("monthdiff('dd/MM/yyyy', '16/04/2017', '18/06/2017')");
        Assert.assertEquals("2", result);
    }

    @Test
    public void testMonthdiffFirstDateLater() {
        String result = TestUtils.getCalculated("monthdiff('dd/MM/yyyy', '23/11/2017', '18/06/2017')");
        Assert.assertEquals("-5", result);
    }

    @Test
    public void testYeardiffSameDate() {
        String result = TestUtils.getCalculated("yeardiff('dd/MM/yyyy', '16/06/2017', '16/06/2017')");
        Assert.assertEquals("0", result);
    }

    @Test
    public void testYeardiffFirstDateEarlier() {
        String result = TestUtils.getCalculated("yeardiff('dd/MM/yyyy', '16/06/2015', '18/06/2017')");
        Assert.assertEquals("2", result);
    }

    @Test
    public void testYeardiffFirstDateLater() {
        String result = TestUtils.getCalculated("yeardiff('dd/MM/yyyy', '23/06/2022', '18/06/2017')");
        Assert.assertEquals("-5", result);
    }

    @Test
    public void testAddYears() {
        String result = TestUtils.getCalculated("addyears('dd/MM/yyyy', '18/06/2017', 5)");
        Assert.assertEquals("18/06/2022", result);
    }

    @Test
    public void testAddMonths() {
        String result = TestUtils.getCalculated("addmonths('dd/MM/yyyy', '18/06/2017', 5)");
        Assert.assertEquals("18/11/2017", result);
    }

    @Test
    public void testAddDays() {
        String result = TestUtils.getCalculated("adddays('dd/MM/yyyy', '18/06/2017', 5)");
        Assert.assertEquals("23/06/2017", result);
    }

    @Test
    public void testYear() {
        String result = TestUtils.getCalculated("year('dd/MM/yyyy', '18/06/2017')");
        Assert.assertEquals("2017", result);
    }

    @Test
    public void testMonth() {
        String result = TestUtils.getCalculated("month('dd/MM/yyyy', '18/06/2017')");
        Assert.assertEquals("6", result);
    }

    @Test
    public void testDayOfWeek() {
        String result = TestUtils.getCalculated("dayofweek('dd/MM/yyyy', '14/01/2021')");
        Assert.assertEquals("4", result);
    }

    @Test
    public void testDayOfMonth() {
        String result = TestUtils.getCalculated("dayofmonth('dd/MM/yyyy', '14/01/2021')");
        Assert.assertEquals("14", result);
    }

    @Test
    public void testDayOfYear() {
        String result = TestUtils.getCalculated("dayofyear('dd/MM/yyyy', '14/01/2021')");
        Assert.assertEquals("14", result);
    }
}
