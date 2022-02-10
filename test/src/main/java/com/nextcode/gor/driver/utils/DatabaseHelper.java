package com.nextcode.gor.driver.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {

    public static String[] createTestDataBase_Derby() throws ClassNotFoundException, SQLException, IOException {
        System.setProperty("derby.stream.error.field", "MyApp.DEV_NULL");
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        File tmpDirectory = com.google.common.io.Files.createTempDir();
        tmpDirectory.deleteOnExit();
        Path databasePath = Paths.get(tmpDirectory.getAbsolutePath(), "testDB");
        Path credentialsPath = Paths.get(tmpDirectory.getAbsolutePath(), "gor.derby.credentials");
        String connectionString = "jdbc:derby:" + databasePath.toString() + ";create=true";
        DriverManager.setLoginTimeout(0);

        // Create test database
        try(Connection connection =  DriverManager.getConnection(connectionString)) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE SCHEMA RDA");
            statement.executeUpdate("CREATE TABLE rda.variant_annotations\n(" +
                    "PROJECT_ID VARCHAR(30),\n" +
                    "CHROMO VARCHAR(10),\n" +
                    "POS INT,\n" +
                    "PN VARCHAR(30),\n" +
                    "FOO VARCHAR(30),\n" +
                    "COMMENT VARCHAR(1000))");


            statement.executeUpdate("INSERT INTO RDA.VARIANT_ANNOTATIONS VALUES\n" +
                    "('10004','chr1',0,'foo1','bar1','comment1')," +
                    "('10004','chr1',1,'foo2','bar2','comment2')," +
                    "('10004','chr1',2,'foo3','bar3','comment3')," +
                    "('10004','chr1',3,'foo4','bar4','comment4')," +
                    "('10004','chr1',4,'foo5','bar5','comment5')"
            );

            statement.executeUpdate("CREATE VIEW rda.v_variant_annotations as select * from rda.variant_annotations");

            statement.close();

        }

        // Create test db configuration
        String dbConfiguration = "name\tdriver\turl\tuser\tpwd\nrda\torg.apache.derby.jdbc.EmbeddedDriver\tjdbc:derby:" + databasePath + "\trda\tbeta3";
        FileUtils.writeStringToFile(credentialsPath.toFile(), dbConfiguration, Charset.defaultCharset());
        return new String[] {tmpDirectory.getAbsolutePath(), databasePath.toString(), credentialsPath.toString()};
    }
}

