# Builder
FROM maven:3.9.7-eclipse-temurin-21 AS build

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY .git /usr/src/app/.git
COPY pom.xml /usr/src/app/pom.xml
COPY src /usr/src/app/src

RUN mvn clean package -Dmaven.test.skip=true --no-transfer-progress

# Published
FROM us.gcr.io/broad-dsp-gcr-public/base/jre:21-debian
COPY --from=build /usr/src/app/target/consent.jar /opt/consent.jar
