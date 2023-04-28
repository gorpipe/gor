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

import org.gorpipe.gor.table.util.PathUtils;

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
    BGI(INDEX, ".bgi"),
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
    GORQ(REPORT, ".gorq"),
    LINK(REFERENCE, ".link"),
    LOCAL_LINK(REFERENCE, ".link.local"),
    CRAM(VARIANTS, ".cram"),
    CRAI(INDEX, ".crai"),
    SPEC(VARIANTS, ".spec"),
    NOR(VARIANTS, ".nor"),
    NORZ(VARIANTS, ".norz"),
    NORD(TABLE, ".nord"),
    CSV(VARIANTS, ".csv"),
    CSVGZ(VARIANTS, ".csv.gz"),
    TSV(VARIANTS, ".tsv"),
    TXT(VARIANTS, ".txt"),
    META(METAINFO, ".meta"),
    MEM(VARIANTS, ".mem"),
    MD5LINK(MD5_LINK, ".md5link"),
    YML(REPORT, ".yml"),
    R(SCRIPT, ".r"),
    SH(SCRIPT, ".sh"),
    PY(SCRIPT, ".py"),
    FASTA(VARIANTS, ".fasta"),
    FA(VARIANTS, ".fa");

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
        file = file.trim().toLowerCase();
        for (DataType type : values()) {
            if (PathUtils.stripTrailingSlash(file).endsWith(type.suffix)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isOfType(String file, DataType type) {
        return PathUtils.stripTrailingSlash(file.trim()).toLowerCase().endsWith(type.suffix);
    }

    private static boolean isLinkToType(String file, DataType type) {
        return PathUtils.stripTrailingSlash(file.trim()).toLowerCase().endsWith(type.suffix + DataType.LINK.suffix);
    }

    public static boolean isOfTypeOrLinksToType(String file, DataType type) {
        return isOfType(file, type) || isLinkToType(file, type);
    }

    public static boolean containsType(String file, DataType type) {
        return file.trim().toLowerCase().contains(type.suffix);
    }

    public static String[] getWritableFormats() {
        return new String[] {DataType.GOR.suffix,
                DataType.GORZ.suffix,
                DataType.NOR.suffix,
                DataType.NORZ.suffix
        };
    }

    @Override
    public String toString() {
        return this.name() + " (" + nature.name() + ")";
    }
}
