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

    public static final String TABLE_NAME = "variant_annotations";
    public static final String RDA_SCHEMA = "rda";
    public static final String AVAS_SCHEMA = "avas";

    public static String[] createRdaDatabase() throws ClassNotFoundException, SQLException, IOException {
        return createTestDataBase_Derby(RDA_SCHEMA, TABLE_NAME, RDA_SCHEMA, "beta3", false);
    }

    public static String[] createRdaDatabaseWithOrg() throws ClassNotFoundException, SQLException, IOException {
        return createTestDataBase_Derby(RDA_SCHEMA, TABLE_NAME, RDA_SCHEMA, "beta3", true);
    }

    public static String[] createAvasDatabase() throws ClassNotFoundException, SQLException, IOException {
        return createTestDataBase_Derby(AVAS_SCHEMA, TABLE_NAME, AVAS_SCHEMA, "beta3", false);
    }

    public static String[] createAvasDatabaseWithOrg() throws ClassNotFoundException, SQLException, IOException {
        return createTestDataBase_Derby(AVAS_SCHEMA, TABLE_NAME, AVAS_SCHEMA, "beta3", true);
    }

    public static String[] createTestDataBase_Derby(String schema, String tableName, String user, String password, boolean includeOrg) throws ClassNotFoundException, SQLException, IOException {
        System.setProperty("derby.stream.error.field", "MyApp.DEV_NULL");
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        File tmpDirectory = com.google.common.io.Files.createTempDir();
        tmpDirectory.deleteOnExit();
        Path databasePath = Paths.get(tmpDirectory.getAbsolutePath(), "testDB");
        Path credentialsPath = Paths.get(tmpDirectory.getAbsolutePath(), "gor.derby.credentials");
        String connectionString = "jdbc:derby:" + databasePath.toString() + ";create=true";
        DriverManager.setLoginTimeout(0);
        var schemaAndTable = schema + "." + tableName;
        var schemaAndView = schema + ".v_" + tableName;

        // Create test database
        try(Connection connection =  DriverManager.getConnection(connectionString)) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE SCHEMA " + schema.toUpperCase());
            statement.executeUpdate("CREATE TABLE " + schemaAndTable.toUpperCase() + "\n(" +
                    "PROJECT_ID VARCHAR(30),\n" +
                    (includeOrg ? "ORGANIZATION_ID VARCHAR(30),\n" : "") +
                    "CHROMO VARCHAR(10),\n" +
                    "POS INT,\n" +
                    "PN VARCHAR(30),\n" +
                    "FOO VARCHAR(30),\n" +
                    "COMMENT VARCHAR(1000))");

            if(includeOrg) {
                statement.executeUpdate("INSERT INTO " + schemaAndTable.toUpperCase() + " VALUES\n" +
                        "('10004','1','chr1',0,'foo1','" + schema + "1','comment1')," +
                        "('10004','1','chr1',1,'foo2','" + schema + "2','comment2')," +
                        "('10004','1','chr1',2,'foo3','" + schema + "3','comment3')," +
                        "('10004','2','chr1',3,'foo4','" + schema + "4','comment4')," +
                        "('10004','2','chr1',4,'foo5','" + schema + "5','comment5')," +
                        "('10005','1','chr1',0,'bar1','" + schema + "1','comment6')," +
                        "('10005','1','chr1',1,'bar2','" + schema + "2','comment7')," +
                        "('10005','1','chr1',2,'bar3','" + schema + "3','comment8')," +
                        "('10005','3','chr1',3,'bar4','" + schema + "4','comment9')," +
                        "('10005','3','chr1',4,'bar5','" + schema + "5','comment10')"
                );
            } else {
                statement.executeUpdate("INSERT INTO " + schemaAndTable.toUpperCase() + " VALUES\n" +
                        "('10004','chr1',0,'foo1','" + schema + "1','comment1')," +
                        "('10004','chr1',1,'foo2','" + schema + "2','comment2')," +
                        "('10004','chr1',2,'foo3','" + schema + "3','comment3')," +
                        "('10004','chr1',3,'foo4','" + schema + "4','comment4')," +
                        "('10004','chr1',4,'foo5','" + schema + "5','comment5')," +
                        "('10005','chr1',0,'bar1','" + schema + "1','comment6')," +
                        "('10005','chr1',1,'bar2','" + schema + "2','comment7')," +
                        "('10005','chr1',2,'bar3','" + schema + "3','comment8')," +
                        "('10005','chr1',3,'bar4','" + schema + "4','comment9')," +
                        "('10005','chr1',4,'bar5','" + schema + "5','comment10')"
                );
            }

            statement.executeUpdate("CREATE VIEW " + schemaAndView.toUpperCase() + " AS SELECT * FROM " + schemaAndTable.toUpperCase());

            statement.close();

        }

        // Create test db configuration
        String dbConfiguration = "name\tdriver\turl\tuser\tpwd\nrda\torg.apache.derby.jdbc.EmbeddedDriver\tjdbc:derby:" + databasePath + "\t"+user+"\t"+password;
        FileUtils.writeStringToFile(credentialsPath.toFile(), dbConfiguration, Charset.defaultCharset());
        return new String[] {tmpDirectory.getAbsolutePath(), databasePath.toString(), credentialsPath.toString()};
    }
}

