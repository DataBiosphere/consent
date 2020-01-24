# Postgres Migration

Helpful resources:
* [Postgres Cheat sheet](https://www.postgresqltutorial.com/postgresql-cheat-sheet/)
* [Postgres Dockerhub](https://hub.docker.com/_/postgres)
* [pgloader](https://pgloader.readthedocs.io/en/latest/)

## Local DB Migration 

Install [pgloader](https://pgloader.readthedocs.io/en/latest/)
```
brew install pgloader
```
Spin up a local copy of the database using this [compose file](postgres-migrate.yaml) 
with appropriate secret values set up correctly.
```
docker-compose -f postgres.yaml up
```

Run the migration on a local database using this [commands file](commands.txt):
```
pgloader commands.txt 
LOG pgloader version "3.6.1"
LOG Data errors in '/private/tmp/pgloader/'
LOG Parsing commands from file #P"commands.txt"
LOG Migrating from #<MYSQL-CONNECTION mysql://consent@localhost:3306/consent {1005EAFDA3}>
LOG Migrating into #<PGSQL-CONNECTION pgsql://consent@localhost:5432/consent {1005EB13E3}>
2020-01-24T11:49:12.872000Z LOG report summary reset
                            table name     errors       rows      bytes      total time
--------------------------------------  ---------  ---------  ---------  --------------
                       fetch meta data          0        109                     0.112s
                        Create Schemas          0          0                     0.004s
                      Create SQL Types          0          0                     0.011s
                         Create tables          0         56                     0.452s
                        Set Table OIDs          0         28                     0.011s
--------------------------------------  ---------  ---------  ---------  --------------
                     consent.access_rp          0        120     1.4 kB          0.070s
consent.accesselection_consentelection          0        168     1.2 kB          0.073s
           consent.consentassociations          0         58     3.0 kB          0.149s
      consent.approval_expiration_time          0          0                     0.144s
                      consent.consents          0         57    66.0 kB          0.313s
                           consent.dac          0          5     0.3 kB          0.264s
             consent.databasechangelog          0        304    53.1 kB          0.337s
                       consent.dacuser          0         77     6.0 kB          0.390s
         consent.databasechangeloglock          0          1     0.0 kB          0.444s
                   consent.datarequest          0          0                     0.362s
                       consent.dataset          0         59     3.9 kB          0.460s
               consent.datasetproperty          0        441    21.9 kB          0.449s
                 consent.dataset_audit          0        229    18.3 kB          0.536s
        consent.dataset_audit_property          0       1899   100.1 kB          0.470s
      consent.dataset_user_association          0          2     0.1 kB          0.526s
                      consent.election          0        361   317.9 kB          0.695s
                    consent.dictionary          0         11     0.2 kB          0.511s
                  consent.email_entity          0        616     2.2 MB          0.945s
                    consent.email_type          0          4     0.1 kB          0.674s
                  consent.match_entity          0        144    12.6 kB          0.750s
               consent.researchpurpose          0          0                     0.738s
                     consent.user_role          0        100     1.9 kB          0.797s
                          consent.vote          0       2327   145.6 kB          0.884s
                   consent.help_report          0         13     0.6 kB          0.513s
           consent.researcher_property          0        217     7.2 kB          0.614s
                         consent.roles          0          6     0.1 kB          0.698s
                 consent.user_role_bak          0         84     1.1 kB          0.788s
               consent.workspace_audit          0        170    13.3 kB          0.777s
--------------------------------------  ---------  ---------  ---------  --------------
               COPY Threads Completion          0          4                     1.139s
                        Create Indexes          0         59                     1.696s
                Index Build Completion          0         59                     0.382s
                       Reset Sequences          0         22                     0.056s
                          Primary Keys          0         27                     0.204s
                   Create Foreign Keys          0         22                     0.164s
                       Create Triggers          0          0                     0.004s
                      Install Comments          0          0                     0.000s
--------------------------------------  ---------  ---------  ---------  --------------
                     Total import time          ✓       7473     3.0 MB          3.645s
```
Dump that to a local file for testing:
```
pg_dump -h localhost -U consent -d consent > consent.sql
```

## Remote DB Migration

### TODO: test process

Update [commands file](commands.txt) such that it connects to remote instances.
* Use a sql proxy file pointing to cloud mysql instance
* Use a sql proxy file pointing to cloud postgresql instance

```yaml
sqlproxy:
  image: broadinstitute/cloudsqlproxy:1.11_20180808
  env_file:
    - ./sqlproxy.env
  volumes:
    - ./sqlproxy-service-account.json:/etc/sqlproxy-service-account.json
  restart: always
```

## Using the db locally:

With this compose, spin up the exported db:
```
version:  "3.7"
services:
  postgres:
    image: postgres:11-alpine
    container_name: postgres
    ports:
      - 5432:5432
    volumes:
      - ./consent.sql:/docker-entrypoint-initdb.d/consent.sql
    environment:
      POSTGRES_DB: consent
      POSTGRES_USER: consent
      POSTGRES_PASSWORD: <db pass>
```

And then connect:
```
$> psql -h localhost -U consent -d consent
Password for user consent: 
psql (12.1, server 11.6)
Type "help" for help.

consent=# \dt
                     List of relations
 Schema  |              Name              | Type  |  Owner  
---------+--------------------------------+-------+---------
 consent | access_rp                      | table | consent
 consent | accesselection_consentelection | table | consent
 consent | approval_expiration_time       | table | consent
 consent | consentassociations            | table | consent
 consent | consents                       | table | consent
 consent | dac                            | table | consent
 consent | dacuser                        | table | consent
 consent | databasechangelog              | table | consent
 consent | databasechangeloglock          | table | consent
 consent | datarequest                    | table | consent
 consent | dataset                        | table | consent
 consent | dataset_audit                  | table | consent
 consent | dataset_audit_property         | table | consent
 consent | dataset_user_association       | table | consent
 consent | datasetproperty                | table | consent
 consent | dictionary                     | table | consent
 consent | election                       | table | consent
 consent | email_entity                   | table | consent
 consent | email_type                     | table | consent
 consent | help_report                    | table | consent
 consent | match_entity                   | table | consent
 consent | researcher_property            | table | consent
 consent | researchpurpose                | table | consent
 consent | roles                          | table | consent
 consent | user_role                      | table | consent
 consent | user_role_bak                  | table | consent
 consent | vote                           | table | consent
 consent | workspace_audit                | table | consent
(28 rows)
```

## Code Changes Required
Running consent against the postgres export requires some configuration and code changes. 

`consent.yaml` - change the driver and connection string:
```
database:
  driverClass: org.postgresql.Driver
  url: jdbc:postgresql://sqlproxy:5432/consent?requireSSL=false&useSSL=false
```

`ConsentModule.java` - change the driver name:
```
this.jdbi = new DBIFactory().build(this.environment, config.getDataSourceFactory(), "postgresql");
```

`pom.xml` changes - add the driver, remove mysql, and update `sql-maven-plugin`:
```xml
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.2.9</version>
    </dependency>
```

`ElectionDAO.findExpiredElections` needs a query update since datediff isn't supported:
```
DATE_PART('day', NOW()::timestamp) - DATE_PART('day', createDate::timestamp)
```
