## Database Access In GOR
This document describes how GOR interacts with databases.

### Overview

GOR supports reading from and writing to SQL databases using JDBC. The supported databases include all databases with a JDBC driver, such as PostgreSQL, Snowflake, MySQL, Oracle, SQL Server, and others.

GOR has several ways to interact with databases:

1. **Direct SQL Queries**: You can execute full SQL queries (select only) directly within GOR scripts.
2. **Limited SQL Commands**: GOR also provides limited SQL commands that allow you to perform limited SELECT.

### **Direct SQL Queries**

This allows you to fetch data from a database and use it in your GOR pipe scripts.


These come in two flavors:
1. **sql commands**: By using the `sql`, `norsql`, `gorsql` source commands you can execute arbitrary SQL select queries and use the results in your GOR pipe scripts. 
2. **sql:// URIs**: By `sql://` URIs in GOR commands you can execute arbitrary SQL select queries and use the results in your GOR pipe scripts.

#### **sql commands**

Notes:
1. Uses DBNorIterator.

#### **sql:// URIs**

Notes:
1. Uses DBNorIterator (and SQLSource).

### **Limited SQL Commands**
These come in  flavors:
1. **db:// URIs**: You can specify database connections using `db://` URIs in GOR commands. This method allows you to read from or write to a database table as if it were a file.
2. **//db/ Paths**: You can use `//db/` paths to reference database tables in GOR commands. This method is similar to using `db://` URIs but provides a more file-like interface.

#### **db:// URIs**

The `db://` URIs are used to read from or write to a database table in GOR queries.

These URIs have the following format:
```db://<db-name>:<table-name>
```

Behind the scenes the data is filterd by organization and project.
Example:
```
gor db://rda:variant_annotations | top 10
```

Notes:
1. Uses DBSource and DbGenomicIterator.

#### **//db/ Paths**
The //db/ paths are arbitrary SELECT statements that can be used to selected from a given database.  
For example, if you have a link file `some/path/mydb.link` with the following content:
```
//db:select * from rda.v_variant_annotations variant_annotations
where variant_annotations.project_id = #{project-id}
order by chromo, pos desc
```

The link file can then be used in a GOR command like this:
```gor some/path/mydb | top 10
```

Their limit is the can ONLY be executed from a link file  but not from a GOR query, as it is not really a data source. As link files are not editable by standard users so this has to be set up by an administrator.  

Notes:  
1. Uses DBNorIterator.
2. This is the old style of doing SQL access in GOR.  The new preferred way is to use the `sql`, `norsql`, `gorsql` commands.  This is likely to be deprecated.

### Sepcial Variables

TBD

#{project-id}


### Configuration

TBD

We have two different configuration files:
*
* <li> gor.db.credentials - contains the system databases, which are used by the system internally (e.g. session management),
*                         and by operation where we have strict access controls (db://, //db:).  These credentials
*                         typically would grant full access to the database.
* <li> gor.sql.credentials - contains the user databases, which are used by the user available commands/sources (SQL,
*                          GORSQL, NORSQL, sql://).  These credentials typically would grant limited/read-only access to
*                          the database.
* <p><br>
* The format for these files is:
* <pre>
*    name\tdriver\turl\tuser\tpwd
*    rda\torg.postgresql.Driver\tjdbc:postgresql://myurl.com:5432/csa\trda\tmypass
*    ...
* </pre>
*<p>
* The location of the files defaults to the config directory but can be specified by the system properties
* gor.db.credentials and gor.sql.credentials.