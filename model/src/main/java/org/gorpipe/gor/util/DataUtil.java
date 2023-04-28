package org.gorpipe.gor.util;

import org.gorpipe.exceptions.GorDataException;
import org.gorpipe.gor.driver.meta.DataType;

public class DataUtil {

    public static boolean isMem(String file) {
        return DataType.isOfType(file, DataType.MEM);
    }

    public static boolean isGord(String file) {
        return DataType.isOfType(file, DataType.GORD) || isGordLink(file);
    }

    public static boolean isGordLink(String file) {
        return DataType.isOfType(file, DataType.LINK)
                && DataType.isOfType(file.substring(0, file.length() - DataType.LINK.suffix.length()), DataType.GORD);
    }

    public static boolean isGor(String file) {
        return DataType.isOfType(file, DataType.GOR);
    }

    public static boolean isGorz(String file) {
        return DataType.isOfType(file, DataType.GORZ);
    }

    public static boolean isNord(String file) {
        return DataType.isOfType(file, DataType.NORD);
    }

    public static boolean isNor(String file) {
        return DataType.isOfType(file, DataType.NOR);
    }

    public static boolean isNorz(String file) {
        return DataType.isOfType(file, DataType.NORZ);
    }

    public static boolean isNorSource(String file) {
        return isNorz(file)
                || isTsv(file)
                || isNor(file);
    }

    public static boolean isTxt(String file) {
        return DataType.isOfType(file, DataType.TXT);
    }

    public static boolean isTsv(String file) {
        return DataType.isOfType(file, DataType.TSV);
    }

    public static boolean isAnyCsv(String file) {
        return DataType.isOfType(file, DataType.CSV)
                || DataType.isOfType(file, DataType.CSVGZ);
    }

    public static boolean isLink(String file) {
        return DataType.isOfType(file, DataType.LINK);
    }

    public static boolean isMeta(String file) {
        return DataType.isOfType(file, DataType.META);
    }

    public static boolean isRScript(String file) {
        return DataType.isOfType(file, DataType.R);
    }

    public static boolean isShellScript(String file) {
        return DataType.isOfType(file, DataType.SH);
    }

    public static boolean isPythonScript(String file) {
        return DataType.isOfType(file, DataType.PY);
    }

    public static boolean isYml(String file) {
        return DataType.containsType(file, DataType.YML);
    }

    public static boolean isGorq(String file) {
        return DataType.containsType(file, DataType.GORQ);
    }

    public static boolean isParquet(String file) {
        return DataType.isOfType(file, DataType.PARQUET);
    }

    public static boolean isBam(String file) {
        return DataType.isOfType(file, DataType.BAM);
    }

    public static boolean isCram(String file) {
        return DataType.isOfType(file, DataType.CRAM);
    }

    public static boolean isBgen(String file) { return DataType.isOfType(file, DataType.BGEN); }

    public static boolean isAnyVcf(String file) {
        return DataType.isOfType(file, DataType.VCF)
                || DataType.isOfType(file, DataType.VCFGZ)
                || DataType.isOfType(file, DataType.VCFBGZ);
    }

    public static boolean isGZip(String file) {
        return file.trim().toLowerCase().endsWith(".gz");
    }

    public static String toFile(String name, DataType type) {
        return name + type.suffix;
    }

    public static String toLinkFile(String name, DataType type) {
        return name + type.suffix + DataType.LINK.suffix;
    }

    public static String toTempTempFile(String file) {
        var type = DataType.fromFileName(file);

        if (type != null) {
            return file.replace(type.suffix, "temptempfile" + type.suffix);
        }

        throw new GorDataException(String.format("Data type does not exist for file: %s", file));

    }
}
