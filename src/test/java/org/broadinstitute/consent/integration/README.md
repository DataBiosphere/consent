# Smoke Testing

Provides a mechanism for running simple smoke tests. The intention here is to keep this
layer as slim as possible to provide a minimum sense of confidence in application stability. 

## Local development/testing process

To run against the default environment (dev), run with no additional arguments:

```shell
mvn clean test -P integration-tests
```

To run against a custom environment, pass a `-DbaseUrl=` with a valid base url.
