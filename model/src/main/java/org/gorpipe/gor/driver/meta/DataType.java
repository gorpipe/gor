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

package org.gorpipe.gor.driver.meta;

import static org.gorpipe.gor.driver.meta.FileNature.*;

/**
 * Enumerates possible data types (e.g. file types)
 * Created by villi on 23/08/15.
 */
public enum DataType {
    GOR(VARIANTS, ".gor"),
    GORZ(VARIANTS, ".gorz"),
    GORGZ(VARIANTS, ".gor.gz"),
    PARQUET(VARIANTS, ".parquet"),
    GORI(INDEX, ".gori"),
    SAM(VARIANTS, ".sam"),
    BAM(VARIANTS, ".bam"),
    BGEN(VARIANTS, ".bgen"),
    VCF(VARIANTS, ".vcf"),
    GVCF(VARIANTS, ".gvcf"),
    BCF(VARIANTS, ".bcf"),
    VCFGZ(VARIANTS, ".vcf.gz"),
    VCFBGZ(VARIANTS, ".vcf.bgz"),
    GVCFGZ(VARIANTS, ".gvcf.gz"),
    BAI(INDEX, ".bai"),
    TBI(INDEX, ".tbi"),
    CSI(INDEX, ".csi"),
    GORD(TABLE, ".gord"),
    GORT(TABLE, ".gort"),
    GORP(TABLE, ".gorp"),
    LINK(REFERENCE, ".link"),
    LOCAL_LINK(REFERENCE, ".link.local"),
    CRAM(VARIANTS, ".cram"),
    CRAI(INDEX, ".crai"),
    SPEC(VARIANTS, ".spec"),
    NOR(VARIANTS, ".nor"),
    NORZ(VARIANTS, ".norz"),
    CSV(VARIANTS, ".csv"),
    TSV(VARIANTS, ".tsv"),
    TXT(VARIANTS, ".txt"),
    MEM(VARIANTS, ".mem"),
    MD5LINK(MD5_LINK, ".md5link");

    public final String suffix;
    public final FileNature nature;

    DataType(FileNature nature, String suffix) {
        this.nature = nature;
        this.suffix = suffix;
    }

    /**
     * Use file/source name to determine DataType
     *
     * @return DataType or null if not found
     */
    public static DataType fromFileName(String file) {
        file = file.toLowerCase();
        for (DataType type : values()) {
            if (file.endsWith(type.suffix)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name() + " (" + nature.name() + ")";
    }
}
