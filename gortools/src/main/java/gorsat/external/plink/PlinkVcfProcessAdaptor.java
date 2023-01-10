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

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.session.GorSession;
import org.gorpipe.gor.model.Row;
import org.gorpipe.gor.util.DataUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

/**
 * This analysis class collects gor or vcf lines and writes them in batches to a temporary vcf files read by plink2 process
 */
public class PlinkVcfProcessAdaptor extends PlinkProcessAdaptor {
    private FileWriter vcf;
    private String vcfHeader;

    public PlinkVcfProcessAdaptor(GorSession session, PlinkArguments plinkArguments, int refIdx, int altIdx, int rsIdx, int valueIdx, boolean hc, float th, boolean vcf, String header, String expectedHeader) throws IOException {
        super(session, plinkArguments, refIdx, altIdx, rsIdx, valueIdx, hc, th, vcf, expectedHeader);
        if( vcf ) vcfHeader = "#"+header+"\n";
    }

    private void dumpToVcf(Writer fw, String vcfHeader) throws IOException {
        fw.write("##fileformat=VCFv4.2\n");
        fw.write("##FORMAT=<ID=GT,Type=String,Number=1,Description=\"Threshholded genotype call\">\n");
        fw.write("##FORMAT=<ID=GP,Type=Float,Number=G,Description=\"Genotype call probabilities\">\n");
        fw.write("##FORMAT=<ID=HP,Type=Float,Number=.,Description=\"Haplotype call probabilities\">\n");
        fw.write(vcfHeader);
    }

    boolean isWriterInitialized() {
        return vcf!=null;
    }

    void prepareAndRunPlink(String vcfFilePath) throws ExecutionException, InterruptedException {
        try {
            if(isWriterInitialized()) vcf.close();
        } catch (Exception e) {
            throw new GorSystemException(e);
        }
        Path vcfPath = Paths.get(DataUtil.toFile( vcfFilePath, DataType.VCF));
        Path rootPath = session.getProjectContext().getRealProjectRootPath();
        if((vcfPath.isAbsolute() && Files.exists(vcfPath)) || Files.exists(rootPath.resolve(vcfPath))) {
            if (plinkFuture != null) first = plinkFuture.get();
            PlinkThread plinkThread = new PlinkThread(session.getProjectContext().getRealProjectRootPath().toFile(), this.writeDir,
                    this.plinkExecutable, vcfFilePath, this.psamFile, this.first, this, this.args, true);
            plinkFuture = es.submit(plinkThread);
        } else plinkFuture = null;
    }

    @Override
    public void setup() {
        try {
            setNewVcfStream();
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
    }

    private void setNewVcfStream() throws IOException {
        this.pfnIdx = (this.pfnIdx + 1) & 1;
        this.vcf = new FileWriter(DataUtil.toFile(getCurrentInputFile(), DataType.VCF));
        dumpToVcf(vcf, vcfHeader);
    }

    @Override
    void processRow(Row row) throws IOException, ExecutionException, InterruptedException {
        if (linesWrittenToCurrentFile > MAXIMUM_NUMBER_OF_LINES) {
            prepareAndRunPlink(getCurrentInputFile());
            setNewVcfStream();
            linesWrittenToCurrentFile = 0;
        }
        row.writeRow(vcf);
        vcf.write('\n');
        linesWrittenToCurrentFile++;
    }
}