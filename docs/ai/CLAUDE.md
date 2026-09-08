# Consent Development Notes

## Service constructor parameter order

When writing or updating a service constructor, follow this ordering convention:

1. `Jdbi jdbi` — always first
2. `XxxServiceDAO` composites — immediately after Jdbi, one per service
3. Other injected services — in any order
4. Configuration objects (e.g. `ConsentConfiguration`) — last

Example (DarCollectionService):
```java
@Inject
public DarCollectionService(
    Jdbi jdbi,
    DarCollectionServiceDAO darCollectionServiceDAO,
    EmailService emailService,
    DACAutomationRuleService dacAutomationRuleService) {
  this.dacDAO = jdbi.onDemand(DacDAO.class);
  ...
}
```

## DAO instantiation

All Jdbi SQL object DAOs (interfaces in `org.broadinstitute.consent.http.db`) must be
instantiated inside the service constructor via `jdbi.onDemand(XxxDAO.class)`. Never pass
a raw DAO as a constructor parameter to a service.

The composite orchestration classes in `service/dao` are plain Java objects, NOT Jdbi SQL
object interfaces. They are injected directly as constructor parameters and must NOT be
instantiated via `jdbi.onDemand()`.

## ConsentModule singleton pattern

Every `@Provides` method in `ConsentModule` that creates a new service or DAO instance is
annotated `@Singleton`, and takes each of its dependencies as a method parameter so Guice
resolves them. Guice caches the singleton itself, so no lazy field or `synchronized` guard
is needed:

```java
@Provides
@Singleton
private DatasetService providesDatasetService(
    Jdbi jdbi,
    DatasetServiceDAO datasetServiceDAO,
    ElasticSearchService elasticSearchService,
    EmailService emailService,
    OntologyService ontologyService) {
  return new DatasetService(
      jdbi, datasetServiceDAO, elasticSearchService, emailService, ontologyService);
}
```

Never call one `@Provides` method from another. A direct call bypasses Guice's scoping and
builds a second instance with its own `jdbi.onDemand` DAOs. Declare the dependency as a
parameter instead. Adding a new service means adding a provider here — a service that is
only JIT-bound (constructed by Guice without a declared provider) is unscoped, so a second
injection point silently creates a second instance.
