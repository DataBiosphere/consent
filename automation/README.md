# Automation Testing

Tests are designed to be run in two ways. Manually from the command line, or through automation scripts.

References:
* [Gatling Documentation](https://gatling.io/docs/current/)
* [Gatling SBT Plugin](https://github.com/gatling/gatling-sbt-plugin-demo)
* [Consul Template](https://github.com/hashicorp/consul-template)

## Automated Testing (Local Development)

Render configs for all cases below:
```bash
./render-local-env.sh [env] [vault token]
```
**Arguments:** (arguments are positional)
* env
  * Environment to run tests against; defaults to `dev`. Other valid values are `local`, `staging`
* Vault Auth Token
  * Defaults to reading it from the .vault-token via `$(cat ~/.vault-token)`.
  
Example for running tests locally against the dev environment:
```bash
./render-local-env.sh dev ~/.vault-token 
```  

Example for running tests locally against a local environment:
```bash
./render-local-env.sh local 
```  

### Run all tests:
```
sbt clean gatling:test 
```

### Run specific tests:
```
sbt clean gatling:testOnly *.StatusScenarios 
```

### Run all tests under docker:
```
docker build -t automation-consent:latest .
docker run automation-consent
```

## Development
To create new tests, use `StatusScenarios` as a model. All tests extending `Simulation` will be run 
by default. `Requests` contains a summary of existing supported APIs. 
