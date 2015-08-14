FROM phusion/baseimage:latest
FROM google/nodejs

MAINTAINER Belatrix Team <belatrix@broadinstitute.org>

USER root

#base setup
RUN apt-get update \
    && apt-get install -y \
    && apt-get clean \
    && apt-get -yq install libfontconfig imagemagick \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Copy source files to the app directory
COPY gulp /app/gulp
COPY e2e /app/e2e
COPY src /app/src
COPY .bowerrc /app/
COPY .jshintrc /app/
COPY .yo-rc.json /app/
COPY bower.json /app/
COPY gulpfile.js /app/
COPY karma.conf.js /app/
COPY package.json /app/
COPY protractor.conf.js /app/

#install bower and gulp, and local gulp
WORKDIR /app
RUN npm install -g wrench \
    && npm install -g bower \
    && npm install -g gulp \
    && npm install --save-dev gulp \
    && npm install \
    && bower install --allow-root

# Default ports for gulp
EXPOSE 8000
EXPOSE 3001

WORKDIR /app
CMD ["gulp", "serve"]
