# PostgreSQL Migration

## Local DB Migration 

Install [pgloader](https://pgloader.readthedocs.io/en/latest/)
```shell script
brew install pgloader
```
Spin up a local copy of the database using this [compose file](postgresql.yaml) 
with appropriate secret values set up correctly.
```shell script
docker-compose -f postgres.yaml up
```

Run the migration on a local database using this [commands file](commands.txt):
```shell script
pgloader commands.txt 
LOG pgloader version "3.6.1"
LOG Data errors in '/private/tmp/pgloader/'
LOG Parsing commands from file #P"commands.txt"
LOG Migrating from #<MYSQL-CONNECTION mysql://<user>@localhost:3306/consent {1005EAFDA3}>
LOG Migrating into #<PGSQL-CONNECTION pgsql://root@localhost:5432/consent {1005EB13E3}>
LOG report summary reset
                            table name     errors       rows      bytes      total time
--------------------------------------  ---------  ---------  ---------  --------------
                       fetch meta data          0        109                     0.124s
                        Create Schemas          0          0                     0.016s
                      Create SQL Types          0          0                     0.014s
                         Create tables          0         56                     0.548s
                        Set Table OIDs          0         28                     0.012s
--------------------------------------  ---------  ---------  ---------  --------------
                     consent.access_rp          0        120     1.4 kB          0.090s
consent.accesselection_consentelection          0        168     1.2 kB          0.085s
           consent.consentassociations          0         58     3.0 kB          0.200s
      consent.approval_expiration_time          0          0                     0.137s
                           consent.dac          0          5     0.3 kB          0.371s
                      consent.consents          0         57    66.0 kB          0.305s
             consent.databasechangelog          0        304    53.1 kB          0.451s
                       consent.dacuser          0         77     6.0 kB          0.455s
                   consent.datarequest          0          0                     0.472s
         consent.databasechangeloglock          0          1     0.0 kB          0.478s
               consent.datasetproperty          0        441    21.9 kB          0.575s
                       consent.dataset          0         59     3.9 kB          0.546s
                 consent.dataset_audit          0        229    18.3 kB          0.657s
        consent.dataset_audit_property          0       1899   100.1 kB          0.624s
      consent.dataset_user_association          0          2     0.1 kB          0.719s
                      consent.election          0        361   317.9 kB          0.945s
                    consent.dictionary          0         11     0.2 kB          0.690s
                  consent.email_entity          0        616     2.2 MB          1.041s
                    consent.email_type          0          4     0.1 kB          0.907s
                  consent.match_entity          0        144    12.6 kB          1.011s
               consent.researchpurpose          0          0                     0.972s
                     consent.user_role          0        100     1.9 kB          1.066s
                          consent.vote          0       2327   145.6 kB          1.120s
                   consent.help_report          0         13     0.6 kB          0.758s
           consent.researcher_property          0        217     7.2 kB          0.863s
                         consent.roles          0          6     0.1 kB          1.019s
                 consent.user_role_bak          0         84     1.1 kB          0.987s
               consent.workspace_audit          0        170    13.3 kB          1.047s
--------------------------------------  ---------  ---------  ---------  --------------
               COPY Threads Completion          0          4                     1.544s
                        Create Indexes          0         59                     2.266s
                Index Build Completion          0         59                     0.507s
                       Reset Sequences          0         22                     0.067s
                          Primary Keys          0         27                     0.291s
                   Create Foreign Keys          0         22                     0.282s
                       Create Triggers          0          0                     0.009s
                       Set Search Path          0          1                     0.019s
                      Install Comments          0          0                     0.000s
--------------------------------------  ---------  ---------  ---------  --------------
                     Total import time          ✓       7473     3.0 MB          4.985s
```
Dump that to a local file for testing:
```shell script
pg_dump -h localhost -U root -d consent > pg_consent.sql
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