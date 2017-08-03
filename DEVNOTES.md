# Local Development

Check out repository:
```bash
git clone git@github.com:broadinstitute/consent.git
```

Build and render Configs:
```bash
cd consent
mvn clean compile
APP_NAME=consent ENV=local OUTPUT_DIR=config ../firecloud-develop/configure.rb
```

Spin up application:
```bash
docker-compose -p consent -f config/docker-compose.yaml up
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