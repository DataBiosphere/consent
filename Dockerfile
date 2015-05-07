FROM:ubuntu:14.04

RUN apt-get update \
    apt-get install opendjdk-7-jre supervisor \
    apt-get clean \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* 


