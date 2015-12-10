
Consent Web Services
====================

This is a dropwizard module for the Consent Web Services API.

Some documentation for the initial version of this service is available in 
docs/ConsentServicesAPIv1.pdf

# Endpoints:

https://consent-ui.dsde-dev.broadinstitute.org/swagger/ [Update with Prod URL]

# Configuration

### Example configuration:

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
mongo:
  uri: mongodb://localhost:27017
  dbName: consent
  username: <MONGO_USERNAME>
  password: <MONGO_PASSWORD>
  testMode: true
mailConfiguration:
  activateEmailNotifications: false
  smtpPort: 587
  smtpAuth: true
  host: smtp.gmail.com
  smtpStartTlsEnable: true
  googleAccount: <GOOGLE_USER>
  accountPassword: <GOOGLE_PASSWORD>
freeMarkerConfiguration:
  templateDirectory: /freemarker
  defaultEncoding: UTF-8
```

### Running Consent-WS in development mode

There are two files to configure to run this app in local environment:

##### 1. pom.xml 
Pom file should contain local database properties. 

For instance, to use MySql, db properties should be :

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

##### 2. consent-config.yml
   In this file, properties should be like :

```
database:
  driverClass: com.mysql.jdbc.Driver
  user: root
  password: root
  url:  jdbc:mysql://localhost:3306/consent
  validationQuery: SELECT 1
mongo:
  uri: mongodb://localhost:27017
  dbName: consent
  username:
  password:
  testMode: true
googleStore:
  username: <GOOGLE_CLOUD_STORAGE_USERNAME>
  password: <URL TO .p12 PASSWORD FILE>
  endpoint: https://storage.googleapis.com/
  bucket: consent
  type: GCS
mailConfiguration:
  activateEmailNotifications: false
  smtpPort: 587
  smtpAuth: true
  host: smtp.gmail.com
  smtpStartTlsEnable: true
  googleAccount: <GOOGLE_USER>
  accountPassword: <GOOGLE_PASSWORD>
freeMarkerConfiguration:
  templateDirectory: /freemarker
  defaultEncoding: UTF-8
```

##### Once the 2 files are properly configured: 

 1. Build the application and populate initial database schema. 
    Database schema must exist, even empty, on database server.
```
    mvn clean package
```
 2. Change Mongo test Mode and Mail Notifications in consent-config.yml.
```
testMode: false
activateEmailNotifications: true
```

 3. After build is complete and successful, start the application:
```
    java -jar target/consent.jar server ./target/test-classes/consent-config.yml
```