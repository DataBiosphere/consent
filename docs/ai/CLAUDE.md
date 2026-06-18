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

Every `@Provides` method in `ConsentModule` that creates a new service or DAO instance uses
`@Singleton` + `synchronized` + a lazy null-guard field to guarantee a single instance
on both the Guice injection path and the direct inter-provider call path:

```java
@Provides
@Singleton
synchronized EmailService providesEmailService() {
  if (emailService == null) {
    emailService = new EmailService(...);
  }
  return emailService;
}
```
