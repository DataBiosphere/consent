# Local Development

* Maven 3.5
* Java 8
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

Tests spin up embedded mongo and http servers that run against localhost. 
Ensure that your test environment supports that. 


#### Docker

Consent docker images are stored in the cloud in the [Consent Dockerhub repo](https://hub.docker.com/r/broadinstitute/consent).
```
# to build the docker image
./consent-ws/build.sh -d build

# to build the docker image and push it to dockerhub 
./consent-ws/build.sh -d push

# to pull the docker image from dockerhub
docker pull broadinstitute/consent
```

### Render Configs 
Specific to internal Broad systems:
```bash
APP_NAME=consent ENV=local OUTPUT_DIR=config ../firecloud-develop/configure.rb
```
Otherwise, use `consent-ws/src/test/resources/consent-config.yml` as a template to 
create your own environment-specific configuration. 

### Spin up application:
Specific to internal Broad systems:
```bash
docker-compose -p consent -f config/docker-compose.yaml up
```
Or, if not using docker:
```bash
java -jar /path/to/consent.jar server /path/to/config/file
```

Visit local swagger page: https://local.broadinstitute.org:27443/swagger/

### Debugging
Port 5005 is open in the configured docker compose. 
Set up a remote debug configuration pointing to `local.broadinstitute.org`
and the defaults should be correct.

Execute the `fizzed-watcher:run` maven task (under consent-ws plugins)  
to enable hot reloading of class and resource files.


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
```

Consent will now point to a local ES instance. 
I also suggest changing the default bucket location so uploaded
ontology files do not interfere with other dev environments.

