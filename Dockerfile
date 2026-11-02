# Builder
FROM maven:3.9.11-eclipse-temurin-25 AS build

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY .git /usr/src/app/.git
COPY pom.xml /usr/src/app/pom.xml
COPY src /usr/src/app/src

# The Maven workflow validates the OpenAPI spec on each PR, so skip it here.
RUN mvn clean package -Dmaven.test.skip=true -Dcodegen.skip=true --no-transfer-progress

# Published
FROM us.gcr.io/broad-dsp-gcr-public/base/jre:25-jre
# Liquibase 4.30+ sends usage analytics by default. That call can stall where egress is closed.
ENV LIQUIBASE_ANALYTICS_ENABLED=false
COPY --from=build /usr/src/app/target/consent.jar /opt/consent.jar
