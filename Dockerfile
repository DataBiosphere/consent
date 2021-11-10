# Builder
FROM adoptopenjdk/maven-openjdk11:latest AS build

RUN mkdir /usr/src/app
WORKDIR /usr/src/app

COPY .git /usr/src/app/.git
COPY pom.xml /usr/src/app/pom.xml
COPY src /usr/src/app/src

RUN mvn clean package -Dmaven.test.skip=true --no-transfer-progress

# Published
FROM us.gcr.io/broad-dsp-gcr-public/base/jre:11-debian
COPY --from=build /usr/src/app/target/consent.jar /opt/consent.jar
