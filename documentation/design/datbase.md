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

Database sources come from two places: a credentials file, and credentials the host application passes
in programmatically.

#### The credentials file

`gor.db.credentials` contains the databases gor can reach. It feeds both connection caches:

- **system connections** — used internally (e.g. session management) and by the access-controlled
  operations (`db://`, `//db:`)
- **user connections** — behind the user-available commands and sources (`SQL`, `GORSQL`, `NORSQL`,
  `sql://`)

The file is tab-separated, with a header line:

```
name	driver	url	user	pwd
rda	org.postgresql.Driver	jdbc:postgresql://myurl.com:5432/csa	rda	mypass
```

The password column is optional. Lines starting with `#` are ignored.

Its location defaults to the config directory and can be set with the `gor.db.credentials` system
property.

#### Credentials passed in by the host

Credentials that rotate cannot be baked into a file — a rotation would leave it stale. The host
application passes those in as `DbCredentials`:

```java
var credentials = List.of(new DbCredentials("rda", url, user, password));

DbConnection.initInServer(credentials);
// or, per cache:
DbConnection.systemConnections.initializeDbSources(credpath, credentials);
```

Gor never reads credentials from the environment itself, and does not care where the host got them —
its own configuration, a secret manager, or environment variables the host owns the naming of. This
keeps deployment-specific naming out of the library.

`DbCredentials` takes `name`, `url`, `user`, `pwd`, and an optional `driver`. When `driver` is null it
is derived from the url prefix (`jdbc:postgresql:`, `jdbc:oracle:`). Blank values count as unset,
including the password, so a secret manager rendering an empty string does not become a real empty
password. Incomplete credentials are logged — naming the missing field, never a value — and skipped,
rather than failing startup.

#### Precedence

Supplied credentials are installed first, then the file is read on top, so **a file row overrides a
supplied source of the same name**. A host that wants its supplied credentials to be authoritative for
a source must keep a row of that name out of the file.

If the configured credentials file is missing, that is an error — unless credentials were supplied, in
which case gor logs a warning and continues with those.