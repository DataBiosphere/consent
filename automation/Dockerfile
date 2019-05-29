FROM openjdk:jre-alpine

ENV SBT_VERSION 1.2.8
ENV PATH /sbt/bin:$PATH

# Add Bash
RUN apk update && apk add bash

# Add/load sbt
RUN wget https://piccolo.link/sbt-$SBT_VERSION.tgz && \
  tar -xzvf sbt-$SBT_VERSION.tgz && \
  sbt sbtVersion

WORKDIR /app

COPY src /app/src
COPY test.sh /app
COPY project /app/project
COPY build.sbt /app

ENTRYPOINT ["bash", "test.sh"]
