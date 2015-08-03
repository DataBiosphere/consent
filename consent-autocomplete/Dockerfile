FROM phusion/baseimage:latest

#base setup
RUN apt-get update \
    && apt-get install -y openjdk-8-jre supervisor \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* 

#Configs
COPY target/autocomplete.jar /opt/consent-autocomplete.jar
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD ["/usr/bin/supervisord"]
