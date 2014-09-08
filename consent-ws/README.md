
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