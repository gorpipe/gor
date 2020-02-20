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

package gorsat.process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Genotype lookup utilities for generating vcf files from horizontal got gt data
 */
public class GenotypeLookupUtilities {
    public final static Map<Character, String> numchar2Genotype = new HashMap<>();

    static {
        String oo = "\t0/0";
        String oi = "\t0/1";
        String ii = "\t1/1";
        String pp = "\t./.";

        numchar2Genotype.put('0', oo);
        numchar2Genotype.put('1', oi);
        numchar2Genotype.put('2', ii);
        numchar2Genotype.put('3', pp);
        numchar2Genotype.put('.', pp);
    }
}
