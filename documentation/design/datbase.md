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

There are two connection caches, and in a server they are fed from **separate** sources:

| Cache | Used by | Fed from |
|---|---|---|
| system connections | internal use (e.g. session management) and the access-controlled operations `db://`, `//db:` | credentials the host passes in |
| user connections | the user-available commands and sources `SQL`, `GORSQL`, `NORSQL`, `sql://` | the `gor.db.credentials` file |

They are kept apart deliberately. System credentials typically rotate, and a file cannot hold rotating
credentials without going stale; equally, the rotating system credentials are not the ones user queries
should reach.

In a console app there is no host to supply credentials, so both caches load from the file.

#### The credentials file

`gor.db.credentials` is tab-separated, with a header line:

```
name	driver	url	user	pwd
rda	org.postgresql.Driver	jdbc:postgresql://myurl.com:5432/csa	rda	mypass
```

The password column is optional. Lines starting with `#` are ignored.

Its location defaults to the config directory and can be set with the `gor.db.credentials` system
property.

A missing credentials file is an error.

#### Credentials passed in by the host

The host application supplies the system credentials as `DbCredentials`:

```java
var systemCredentials = List.of(new DbCredentials("rda", url, user, password));

DbConnection.initInServer(systemCredentials);
// which is, per cache:
DbConnection.systemConnections.initializeDbSources(systemCredentials);
DbConnection.userConnections.initializeDbSources(credpath);
```

Gor never reads credentials from the environment itself, and does not care where the host got them —
its own configuration, a secret manager, or environment variables the host owns the naming of. This
keeps deployment-specific naming out of the library.

`DbCredentials` takes `name`, `url`, `user`, `pwd`, and an optional `driver`. When `driver` is null it
is derived from the url prefix (`jdbc:postgresql:`, `jdbc:oracle:`). Blank values count as unset,
including the password, so a secret manager rendering an empty string does not become a real empty
password. Incomplete credentials are logged — naming the missing field, never a value — and skipped,
rather than failing startup.

The two `initializeDbSources` overloads are alternatives, not additive: each clears the cache first.
That is what keeps a cache fed from exactly one source. Passing no credentials therefore leaves the
system cache empty, and `db://` sources will not resolve.