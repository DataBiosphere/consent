# Local Development

* Maven 3.9
* Java 21
* Dropwizard Docs: http://www.dropwizard.io/

### Check out repository:

```bash
git clone git@github.com:broadinstitute/consent.git
```

### Build, test

```bash
cd consent
mvn clean test package 
```

Tests require docker or another OCI runtime to spin up postgres and http servers that run against
localhost. Ensure that your test environment supports that.  Command line docker, podman, and 
Docker Desktop have all worked to run these tests.  Podman was observed to perform better when running tests,
however docker is the tool of choice for other developers and the CI/CD environment.

#### Docker

Consent docker images are stored in GCR: `gcr.io/broad-dsp-gcr-public/consent`

```
# build the docker image
docker build . -t consent
```

This image can then be run with the proper configuration files provided.

### Render Configs

Specific to internal Broad systems:

```bash
APP_NAME=consent ENV=local OUTPUT_DIR=config ../firecloud-develop/configure.rb
```

Otherwise, use `src/test/resources/consent-config.yml` as a template to
create your own environment-specific configuration.

### Spin up application:

Specific to internal Broad systems:

```bash
mvn clean compile
docker-compose -p consent -f config/docker-compose.yaml up
```

Or, if not using docker:

```bash
java -jar /path/to/consent.jar server /path/to/config/file
```

Visit local swagger page: https://local.dsde-dev.broadinstitute.org:27443/swagger/

### Debugging

Port 7777 is open in the configured docker compose.
Set up a remote debug configuration pointing to `local.dsde-dev.broadinstitute.org`
and the defaults should be correct.

### Developing with a local Elastic Search instance:

Update the compose file to include a new section for an ES instance:

```
es:
  image: docker.elastic.co/elasticsearch/elasticsearch:5.5.0
  ports:
    - "9200:9200"
  volumes:
    - ../data:/usr/share/elasticsearch/data
  environment:
    transport.host: 127.0.0.1
    xpack.security.enabled: "false"
    http.host: 0.0.0.0
```

Add a line to the `app` section to link to that:

```
  links:
    - es:es
```

Finally, update the servers in consent.conf to point to this instance:

```
elasticSearch:
  servers:
    - es
  indexName: local-ontology    
  datasetIndexName: datasetIName
```

Consent will now point to a local ES instance.
I also suggest changing the default bucket location so uploaded
ontology files do not interfere with other dev environments.

## How To...

### How to add a new email

DUOS uses SendGrid for sending emails and FreeMarker for expanding email templates before 
passing them to SendGrid. To add a new email, follow these steps:
1. 
2. Create the HTML template and add it to [src/main/resources/freemarker](src/main/resources/freemarker).
2. Add a new entry to [EmailType](src/main/java/org/broadinstitute/consent/http/enumeration/EmailType.java) that references that file.
3. Add a new [MailMessage](src/main/java/org/broadinstitute/consent/http/mail/message/MailMessage.java) subclass that references this EmailType and implements methods to provide data for the freemarker template and database operation
4. Add a method to [EmailService](src/main/java/org/broadinstitute/consent/http/service/EmailService.java) that creates this message and passes it to sendMessage().
