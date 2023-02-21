# Pact Testing

Pact tests can be run with maven using the `pact-tests` profile:
```shell
mvn clean test -Ppact-tests
```

More details on Contract Testing can be found in our handbook
 * https://broadworkbench.atlassian.net/wiki/spaces/IRT/pages/2660368406/Getting+Started+with+Pact+Contract+Testing

Each party owns a set of tests (aka contract tests). The consumer contract tests (aka consumer tests) 
are completely independent of the provider contract tests (aka provider tests), and vice versa.

Specifically:
 * Consent runs consumer tests against mock Sam service. 
 * Upon success, publish a consumer-provider pact to DSP's [Pact Broker](https://pact-broker.dsp-eng-tools.broadinstitute.org/).
 * Pact Broker is the  source of truth to forge contractual obligations between consumer and provider.
 * Sam obtains contract from Pact Broker and runs provider tests to validate its obligations to consumers.

The Pact Broker is a 2-way street. The consumer can see in the dashboard if the service is able to
honor their consumer expectations (which is also true when those expectations are updated). The
provider can validate that a desired deployment would not break any of the consumers' published
expectations.  Both the consumer and the provider can gate deployment on availability/honoring of
the published pact (contract expectations) between the two.

### Sequence Diagram

```mermaid
sequenceDiagram
  participant Consent
  participant Broker
  participant Sam
  Sam->>Broker: Publishes intended behaviors
  Broker->>Broker: Validate contracts
  Consent->>Consent: Generate expected Sam behaviors
  Consent->>Consent: Unit test expected behaviors
  Consent->>Broker: Exports contracts to broker
  Broker->>Broker: Validate contracts
```