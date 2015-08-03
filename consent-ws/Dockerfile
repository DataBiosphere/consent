FROM phusion/baseimage:latest
FROM java:openjdk-8-jre

#base setup
RUN apt-get update \
    && apt-get install -y supervisor \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* 

#Configs
COPY target/consent.jar /opt/consent.jar
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
#CMD ["/usr/bin/supervisord"]
CMD /usr/bin/java -jar /opt/consent.jar server /opt/consent.yml 
