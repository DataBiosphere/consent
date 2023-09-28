# Smoke Testing

Provides a mechanism for running simple smoke tests. 
The intention here is to keep this layer as slim as 
possible to provide a minimum sense of confidence in 
application stability. 

## Local development/testing process

```shell
mvn clean test -P integration-tests
```

1. Spin up a local instance in a terminal window using a proper docker compose file
2. Run integration tests against that environment: `mvn gatling:test`
   1. You can also run the above command with any custom destination: `mvn gatling:test -DbaseUrl=https://consent.dsde-dev.broadinstitute.org/`