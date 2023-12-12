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

package org.gorpipe.base.exceptions;

import org.gorpipe.exceptions.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;

public class UTestGorExceptions {



    @Test
    public void testGorResourceException() {
        GorResourceException ex = new GorResourceException("This is an exception", ".../file.txt");
        Assert.assertEquals("org.gorpipe.exceptions.GorResourceException: This is an exception\n" +
                "URI: .../file.txt\n", ex.toString());
    }

    @Test
    public void testGorResourceExceptionFromContext() {
        GorResourceException ex = ExceptionUtilities.mapGorResourceException("file.txt", "file://file.txt", new FileNotFoundException());
        Assert.assertTrue(ex instanceof GorResourceException);
        Assert.assertEquals("org.gorpipe.exceptions.GorResourceException: Input source does not exist: file.txt\n" +
                "URI: file://file.txt\n", ex.toString());
    }

    @Test
    public void testGorMissingRelationException() {
        GorMissingRelationException ex = new GorMissingRelationException("This is an exception", "[somevr]");
        Assert.assertEquals("org.gorpipe.exceptions.GorMissingRelationException: This is an exception\n" +
                "URI: [somevr]\n", ex.toString());
    }

    @Test
    public void testGorMissingRelationExceptionFromContext() {
        GorResourceException ex = ExceptionUtilities.mapGorResourceException("[somevr]", "[somevr]", new FileNotFoundException());
        Assert.assertTrue(ex instanceof GorMissingRelationException);
        Assert.assertEquals("org.gorpipe.exceptions.GorMissingRelationException: Virtual relation '[somevr]' is missing\n" +
                "URI: [somevr]\n", ex.toString());
    }


    @Test
    public void testGorExceptionToJsonCausedByStackTrace() {
        GorException ex = new GorDataException("Some exception", new ArithmeticException("Some arith exception"));
        String json = ExceptionUtilities.gorExceptionToJson(ex);
        Assert.assertTrue(json.contains("Caused by:"));
    }
    
    @Test
    public void testGorExceptionFromJson() {

        String json = "{\"commandName\":\"SELECT\",\"errorType\":\"GorParsingException\",\"requestId\":\"7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\",\"commandIndex\":2,\"stackTrace\":\"org.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted(CommandParseUtilities.scala:374)\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\",\"message\":\"Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\",\"commandSource\":\"queryService\",\"command\":\"SELECT col2\",\"gorMessage\":\"==== Parsing Error ====\\nCommand SELECT in pipe step #2 has some issues in option col2:\\nColumn COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\n\\n .. | SELECT col2 | ..\\n\\nPart of create statement: create queryService = ...\\n\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\n\\nStack Trace:\\norg.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted(CommandParseUtilities.scala:374)\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\\n\\n\",\"option\":\"col2\"}";

        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);

        for (StackTraceElement ste : ex.getStackTrace()) {
            Assert.assertTrue(json.contains(ste.getMethodName()));
            Assert.assertTrue(json.contains(ste.getFileName()));
            Assert.assertTrue(json.contains(Integer.toString(ste.getLineNumber())));
        }
    }

    @Test
    public void testGorExceptionFromJsonWithCause() {
        try {
            throwsArithmeticException();
        } catch (Exception ae) {
            GorDataException ex = new GorDataException("Some exception", 1, "chrom\tpos\tcalc", "chr1\t1\t0", ae);
            ex.setRequestID("1234");
            ex.setContext("abcd");
            String json = ExceptionUtilities.gorExceptionToJson(ex);
            GorDataException exNew = (GorDataException) ExceptionUtilities.gorExceptionFromJson(json);

            Assert.assertEquals(ex.getMessage(), exNew.getMessage());
            Assert.assertEquals(ex.getRequestID(), exNew.getRequestID());
            Assert.assertEquals(ex.getHeader(), exNew.getHeader());
            Assert.assertEquals(ex.getRow(), exNew.getRow());

            Assert.assertEquals("org.gorpipe.base.exceptions.UTestGorExceptions.throwsArithmeticException",
                    exNew.getStackTrace()[0].getMethodName());
        }
    }

    @Test
    public void testGorExceptionFromJsonEmtpy() {
        String json = "";
        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);
        Assert.assertEquals("Got error with null or empty json", ex.getMessage());
    }

    @Test
    public void testGorExceptionFromJsonNull() {
        String json = null;
        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);
        Assert.assertEquals("Got error with null or empty json", ex.getMessage());
    }

    @Test
    public void testGorExceptionFromJsonBad() {
        String json = "bad json";
        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);
        Assert.assertTrue(ex.getMessage().startsWith("Got error: '" + json + "'\n" +
            "Trying to parse this error as json error resulted in an exception."));
        Assert.assertTrue(ex.getCause() instanceof org.json.simple.parser.ParseException);
        Assert.assertNull(ex.getCause().getMessage());
    }

    @Test
    public void testGorExceptionFromJsonMissingLineNum() {

        String json = "{\"commandName\":\"SELECT\",\"errorType\":\"GorParsingException\",\"requestId\":\"7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\",\"commandIndex\":2,\"stackTrace\":\"org.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted(CommandParseUtilities.scala)\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\",\"message\":\"Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\",\"commandSource\":\"queryService\",\"command\":\"SELECT col2\",\"gorMessage\":\"==== Parsing Error ====\\nCommand SELECT in pipe step #2 has some issues in option col2:\\nColumn COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\n\\n .. | SELECT col2 | ..\\n\\nPart of create statement: create queryService = ...\\n\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\n\\nStack Trace:\\norg.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted(CommandParseUtilities.scala:374)\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\\n\\n\",\"option\":\"col2\"}";

        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);

        for (StackTraceElement ste : ex.getStackTrace()) {
            Assert.assertTrue(json.contains(ste.getMethodName()));
            Assert.assertTrue(json.contains(ste.getFileName()));
            // Skip checking the line pos (as some do not have it)
        }
    }

    @Test
    public void testGorExceptionFromJsonMissingFile() {

        String json = "{\"commandName\":\"SELECT\",\"errorType\":\"GorParsingException\",\"requestId\":\"7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\",\"commandIndex\":2,\"stackTrace\":\"org.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\",\"message\":\"Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\",\"commandSource\":\"queryService\",\"command\":\"SELECT col2\",\"gorMessage\":\"==== Parsing Error ====\\nCommand SELECT in pipe step #2 has some issues in option col2:\\nColumn COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\n\\n .. | SELECT col2 | ..\\n\\nPart of create statement: create queryService = ...\\n\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\n\\nStack Trace:\\norg.gorpipe.exceptions.GorParsingException: Column COL2 is not in the following header:CHROMNOR\\tPOSNOR\\tFILE\\tSET\\n\\n1: CHROMNOR\\n2: POSNOR\\n3: FILE\\n4: SET\\nRequest ID: 7dc7ffe2-de2c-4acf-a81f-b8f9ab51af97\\nCommand Step: SELECT col2\\nCommand Name: SELECT\\nCommand Source: queryService\\nCommand Index: 2\\nOption: col2\\n\\n\\tat gorsat.Commands.CommandParseUtilities$.columnNumber(CommandParseUtilities.scala:351)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2(CommandParseUtilities.scala:395)\\n\\tat gorsat.Commands.CommandParseUtilities$.$anonfun$columnsFromHeader$2$adapted(CommandParseUtilities.scala:374)\\n\\tat scala.collection.IndexedSeqOptimized.foreach(IndexedSeqOptimized.scala:36)\\n\\tat scala.collection.IndexedSeqOptimized.foreach$(IndexedSeqOptimized.scala:33)\\n\\tat scala.collection.mutable.ArrayOps$ofRef.foreach(ArrayOps.scala:198)\\n\\tat gorsat.Commands.CommandParseUtilities$.columnsFromHeader(CommandParseUtilities.scala:374)\\n\\tat gorsat.Commands.Select$.parseArguments(Select.scala:21)\\n\\tat gorsat.Commands.Select$Select.processArguments(Select.scala:71)\\n\\tat gorsat.Commands.CommandInfo.processArguments(CommandInfo.scala:46)\\n\\tat gorsat.Commands.CommandInfo.init(CommandInfo.scala:37)\\n\\tat gorsat.process.PipeInstance.parseCommand(PipeInstance.scala:526)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2(PipeInstance.scala:266)\\n\\tat gorsat.process.PipeInstance.$anonfun$subProcessArguments$2$adapted(PipeInstance.scala:261)\\n\\tat scala.collection.immutable.Range.foreach(Range.scala:158)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:261)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.DynIterator$DynamicRowSource.hasNext(gorsatDynIterator.scala:110)\\n\\tat gorsat.AnalysisUtilities$.getFilterTags(AnalysisUtilities.scala:212)\\n\\tat gorsat.InputSources.Nor$.createNordIterator(Nor.scala:108)\\n\\tat gorsat.InputSources.Nor$.processNorArguments(Nor.scala:77)\\n\\tat gorsat.InputSources.Nor$Nor.processArguments(Nor.scala:95)\\n\\tat gorsat.Commands.InputSourceInfo.init(InputSourceInfo.scala:32)\\n\\tat gorsat.process.PipeInstance.subProcessArguments(PipeInstance.scala:190)\\n\\tat gorsat.process.PipeInstance.processArguments(PipeInstance.scala:95)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.scalaInit(gorsatGorIterator.scala:47)\\n\\tat gorsat.gorsatGorIterator.gorsatGorIterator.init(gorsatGorIterator.scala:51)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.runQuery(GorQueryTask.java:299)\\n\\tat com.decode.resque.services.tasks.GorQueryTask.perform(GorQueryTask.java:205)\\n\\tat org.gorpipe.gor.servers.GorTaskBase.run(GorTaskBase.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.execute(WorkerPoolImpl.java:698)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.process(WorkerPoolImpl.java:650)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.poll(PartitioningWorkerImpl.java:155)\\n\\tat net.greghaines.jesque.worker.WorkerPoolImpl.run(WorkerPoolImpl.java:218)\\n\\tat com.decode.resque.services.PartitioningWorkerImpl.run(PartitioningWorkerImpl.java:83)\\n\\tat java.lang.Thread.run(Thread.java:748)\\n\\n\\n\",\"option\":\"col2\"}";

        GorException ex = ExceptionUtilities.gorExceptionFromJson(json);

        for (StackTraceElement ste : ex.getStackTrace()) {
            Assert.assertTrue(json.contains(ste.getMethodName()));
            // Skip checking the file name and line pos (as some do not have it)
        }
    }


    private void throwsArithmeticException() throws ArithmeticException {
        throw new ArithmeticException("Some arith exception");
    }

}