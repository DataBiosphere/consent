FROM us.gcr.io/broad-dsp-gcr-public/base/jre:21-alpine

# Install necessary tools
RUN apk add --no-cache \
    bash \
    curl \
    jq && \
# Create directory for credentials
    mkdir -p /secrets

# Copy the script to the container
COPY run-endpoint.sh /usr/local/bin/run-endpoint.sh

# Set the entry point to our script
ENTRYPOINT ["/usr/local/bin/run-endpoint.sh"]