
Consent Web Services
====================

This is a dropwizard module for the Consent Web Services API.

## Local Development

Check out repository:
```bash
git clone git@github.com:broadinstitute/consent.git
```

Build and render Configs:
```bash
cd consent
mvn clean package
APP_NAME=consent ENV=local OUTPUT_DIR=config ./configure.rb
```

Spin up application:
```bash
docker-compose -p consent -f config/docker-compose.yaml up
```

Visit local swagger page: https://local.broadinstitute.org/swagger/

### Debugging
Port 5005 is open in the configured docker compose. 
Set up a remote debug configuration pointing to `local.broadinstitute.org`
and the defaults should be correct.

Execute the `fizzed-watcher:run` maven task (under consent-ws plugins)  
to enable hot reloading of class and resource files.