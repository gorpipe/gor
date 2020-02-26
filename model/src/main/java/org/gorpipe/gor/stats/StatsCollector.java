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

package org.gorpipe.gor.stats;

import java.util.HashMap;
import java.util.Map;

public class StatsCollector {
    private Map<Integer, Map<String, Double>> statsPerSender = new HashMap<>();
    private Map<Integer, String> senderNames = new HashMap<>();
    private Map<Integer, String> senderAnnotations = new HashMap<>();
    private Map<String, Integer> registeredNames = new HashMap<>();

    public int registerSender(String senderName, String annotation) {
        String registeredName = senderName + ":" + annotation;
        int id = registeredNames.getOrDefault(registeredName, 0);
        if (id == 0) {
            id = senderNames.size() + 1;
            registeredNames.put(registeredName, id);

            senderNames.put(id, senderName);
            statsPerSender.put(id, new HashMap<>());

            senderAnnotations.put(id, annotation);
        }
        return id;
    }

    public void inc(int sender, String stat) {
        Map<String, Double> senderStats = statsPerSender.computeIfAbsent(sender, k ->new HashMap<>());
        senderStats.compute(stat, (k, v) -> v == null ? 1.0 : v + 1.0);
    }

    public void dec(int sender, String stat) {
        Map<String, Double> senderStats = statsPerSender.computeIfAbsent(sender, k ->new HashMap<>());
        senderStats.compute(stat, (k, v) -> v == null ? -1.0 : v - 1.0);
    }

    public void add(int sender, String stat, double delta) {
        Map<String, Double> senderStats = statsPerSender.computeIfAbsent(sender, k ->new HashMap<>());
        senderStats.compute(stat, (k, v) -> v == null ? delta : v + delta);
    }

    public Map<String, Map<String, Double>> getStats() {
        HashMap<String, Map<String, Double>> namedStats = new HashMap<>();
        statsPerSender.forEach((k,v) -> namedStats.put(senderNames.get(k) + ":" + senderAnnotations.get(k), v));
        return namedStats;
    }
}
