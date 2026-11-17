# Builder
FROM maven:3.9.11-eclipse-temurin-25 AS build

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY .git /usr/src/app/.git
COPY pom.xml /usr/src/app/pom.xml
COPY src /usr/src/app/src

# The Maven workflow validates the OpenAPI spec on each PR, so skip it here.
# Retry Maven Central rate limits; the build otherwise fails before Maven can resolve the BOMs.
RUN attempt=1; \
    while [ "$attempt" -le 4 ]; do \
      mvn clean package -Dmaven.test.skip=true -Dcodegen.skip=true --no-transfer-progress > /tmp/maven-build.log 2>&1; \
      status=$?; \
      cat /tmp/maven-build.log; \
      if [ "$status" -eq 0 ]; then exit 0; fi; \
      if ! grep -q 'status code: 429' /tmp/maven-build.log || [ "$attempt" -eq 4 ]; then exit "$status"; fi; \
      delay=$((attempt * 15)); \
      echo "Maven Central returned HTTP 429; retrying in ${delay}s (attempt $((attempt + 1))/4)"; \
      sleep "$delay"; \
      attempt=$((attempt + 1)); \
    done

# Published
FROM us.gcr.io/broad-dsp-gcr-public/base/jre:25-jre
# Liquibase 4.30+ sends usage analytics by default. That call can stall where egress is closed.
ENV LIQUIBASE_ANALYTICS_ENABLED=false
COPY --from=build /usr/src/app/target/consent.jar /opt/consent.jar
