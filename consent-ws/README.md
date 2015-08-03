
Consent Web Services
====================

This is a dropwizard module for the Consent Web Services API.

Some documentation for the initial version of this service is available in 
docs/ConsentServicesAPIv1.pdf

The API defines a few basic methods, including CREATE, READ, and UPDATE methods for consent documents.
```
PUT /consent
GET /consent/{id}
POST /consent/{id}
```

## Configuration

Example configuration:

```
server:
  adminConnectors:
    - type: http
      port: 8181
  applicationConnectors:
    - type: http
      port: 8180
database:
  driverClass: com.mysql.jdbc.Driver
  user: <MYSQL_USERNAME>
  password: <MYSQL_PASSWORD>
  url: jdbc:mysql://<HOST>:<PORT>/<DBNAME>
  validationQuery: SELECT 1
```

Running Consent-WS in development mode
======================================

There are two files to configure to run this app in local environment:

1. pom.xml 
   pom.file should contain local database properties. For instance, to use MySql, 
   db properties may be :

```
    <properties>
        <java.version>1.8</java.version>
        <dropwizard.version>0.7.0</dropwizard.version>
        <driver>com.mysql.jdbc.Driver</driver>
        <url>jdbc:mysql://localhost:3306/consent</url>
        <username>root</username>
        <password>root</password>
    </properties>
```

2. consent-config.yml
   In this file, MySql properties should be like :

```
  driverClass: com.mysql.jdbc.Driver
  user: root
  password: root
  url:  jdbc:mysql://localhost:3306/consent
  validationQuery: SELECT 1
```

Once the 2 files are properly configured, 

 1. Build the application and populate initial database schema. 
    Database schema must exist, even empty, on database server.
```
    mvn clean package
```

 2. After build is complete and successful, start the application:
```
    java -jar target/consent.jar server ./target/test-classes/consent-config.yml
```

