FROM phusion:baseimage

#base setup
RUN apt-get update \
    apt-get install opendjdk-7-jre supervisor \
    apt-get clean \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* 

#Configs
COPY consent-ws/target/*.jar /opt
COPY consent-autocomplete/target/*.jar /opt/

#Supervisord config
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

CMD ["/usr/bin/supervisord"]
