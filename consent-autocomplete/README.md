# Consent Autocomplete Service

## Configuration

```
server:
  adminConnectors:
    - type: http
      port: 8181
  applicationConnectors:
    - type: http
      port: 8180
elasticSearch:
  servers: [127.0.0.1:9300]
  indexName: ontology
```
