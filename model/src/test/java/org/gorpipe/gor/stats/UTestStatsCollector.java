package org.gorpipe.gor.stats;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class UTestStatsCollector {

    private StatsCollector statsCollector;

    @Before
    public void setUp() throws Exception {
        statsCollector = new StatsCollector();
    }

    @Test
    public void getStatsReturnsEmptyMapAtStart() {
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        assertTrue(stats.isEmpty());
    }

    @Test
    public void registerSingleSender() {
        statsCollector.registerSender("test", "bingo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        Map<String, Double> testStats = stats.get("test:bingo");
        assertTrue(testStats.isEmpty());
    }

    @Test
    public void registerSendersWithIdenticalNames() {
        statsCollector.registerSender("test", "bingo");
        statsCollector.registerSender("test", "bongo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        assertEquals(2, stats.size());
    }

    @Test
    public void incWhenStatDidNotExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.inc(test, "bingo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(1.0, value, 1e-8);
    }

    @Test
    public void incWhenStatDoesExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.inc(test, "bingo");
        statsCollector.inc(test, "bingo");
        statsCollector.inc(test, "bingo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(3.0, value, 1e-8);
    }

    @Test
    public void decWhenStatDidNotExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.dec(test, "bingo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(-1.0, value, 1e-8);
    }

    @Test
    public void decWhenStatDoesExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.dec(test, "bingo");
        statsCollector.dec(test, "bingo");
        statsCollector.dec(test, "bingo");
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(-3.0, value, 1e-8);
    }

    @Test
    public void addWhenStatDidNotExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.add(test, "bingo", 3.14);
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(3.14, value, 1e-8);
    }

    @Test
    public void addWhenStatDoesExist() {
        int test = statsCollector.registerSender("test", "bingo");
        statsCollector.inc(test, "bingo");
        statsCollector.add(test, "bingo", 3.14);
        Map<String, Map<String, Double>> stats = statsCollector.getStats();
        double value = stats.get("test:bingo").get("bingo");
        assertEquals(4.14, value, 1e-8);
    }
}