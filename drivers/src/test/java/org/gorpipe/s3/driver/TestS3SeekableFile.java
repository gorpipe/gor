/*
 * Copyright (c) 2012 deCODE Genetics Inc.
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * deCODE Genetics Inc. ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with deCODE.
 */

package org.gorpipe.s3.driver;

import org.gorpipe.test.IntegrationTests;
import org.gorpipe.utils.DriverUtils;
import gorsat.process.GenericSessionFactory;
import gorsat.process.GorSessionFactory;
import gorsat.process.PipeInstance;
import org.gorpipe.gor.session.GorContext;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @version $Id$
 */
@Category(IntegrationTests.class)
public class TestS3SeekableFile {

    private static final Logger log = LoggerFactory.getLogger(TestS3SeekableFile.class);

    private static String S3_KEY_2;
    private static String S3_SECRET_2;

    /**
     * @param args
     */
    public static void main(String... args) {
        try {

            Properties props = DriverUtils.getDriverProperties();
            S3_KEY_2 = props.getProperty("S3_KEY_2");
            S3_SECRET_2 = props.getProperty("S3_SECRET_2");

            System.setProperty("gor.s3.access.key", S3_KEY_2);
            System.setProperty("gor.s3.secret.key", S3_SECRET_2);

            log.info("First time seek...");
            runGorPipe("-p chr10:58929315- s3://com.decode.test/gor/10547.gor | top 10");

            log.info("\nAnd then again...");
            runGorPipe("-p chr10:58929315- s3://com.decode.test/gor/10547.gor | top 10");

            log.info("\nDirect fetch in the end...");
            runGorPipe("s3://com.decode.test/gor/10547.gor");

            log.info("\nAccess vcf file in s3...");
            runGorPipe("s3://com.decode.test/1232400956A.vcf");

            log.info("\nAccess vcf.gz file in s3...");
            runGorPipe("s3://com.decode.test/1232400956A.vcf.gz");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void runGorPipe(String query) {
        GorSessionFactory sessionFactory = new GenericSessionFactory();
        try (PipeInstance pipe = PipeInstance.createGorIterator(new GorContext(sessionFactory.create()))) {
            pipe.init(query, null);
            while (pipe.hasNext()) {
                pipe.next();
            }
        }
    }
}
