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

package gorsat.external.plink;

/**
 * This class stores the phenotype file for plink vcf input header generation
 * Also the argument to the plink external process
 */
public class PlinkArguments {
    String pheno;
    String covar;
    boolean firth;
    boolean hideCovar;
    boolean dom;
    boolean rec;
    boolean vs;
    boolean qn;
    boolean cvs;
    float hweThreshold;
    float genoThreshold;
    float mafThreshold;

    public PlinkArguments(String pheno, String covar, boolean firth, boolean hideCovar, boolean dom, boolean rec, boolean vs, boolean qn, boolean cvs,
                          float hweThreshold, float genoThreshold, float mafThreshold) {
        this.pheno = pheno;
        this.covar = covar;
        this.firth = firth;
        this.hideCovar = hideCovar;
        this.dom = dom;
        this.rec = rec;
        this.vs = vs;
        this.qn = qn;
        this.cvs = cvs;
        this.hweThreshold = hweThreshold;
        this.genoThreshold = genoThreshold;
        this.mafThreshold = mafThreshold;
    }
}
