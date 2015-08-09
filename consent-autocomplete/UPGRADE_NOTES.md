# Upgrade Notes

## [Migrating from dropwizard 7->8](https://github.com/dropwizard/dropwizard/wiki/Upgrade-guide-0.7.x-to-0.8.x)

## Add new property to module pom file

```
    <properties>
        <dropwizard.version>0.8.1</dropwizard.version>
    </properties>
```

And add that to each of the DW dependencies: `<version>${dropwizard.version}</version>`

Dropwizard Guice needs an update to: `<version>0.8.1.0</version>`

Remove `jetty-servlet` and `jersey-client` dependencies

## Refactor Jersey Client Tests

`
    ClientResponse response =
        gRule.client().resource("/translate")
            .queryParam("for",qType)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.TEXT_PLAIN_TYPE)
            .post(ClientResponse.class,json);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getEntity(String.class)).isEqualTo(expect);
`

becomes: 

`
    Response response =
        gRule.client().target("/translate")
            .queryParam("for", qType)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.TEXT_PLAIN_TYPE)
            .post(Entity.json(json));
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.readEntity(String.class)).isEqualTo(expect);
`
