# Endpoint Execution CronJob

This directory contains the configuration and scripts for a Kubernetes CronJob that authenticates with a Google service account and executes a specific API endpoint.

## Files

- `Dockerfile`: Builds a container with necessary tools for making authenticated API calls
- `run-endpoint.sh`: Script that handles authentication and endpoint execution

## Dockerfile Details

The Dockerfile builds a minimal container based on a distroless image that:
1. Installs required tools (curl, jq, ca-certificates)
2. Creates a directory for credential mounting
3. Copies and makes the execution script executable
4. Sets the script as the container entrypoint

## Script Details

The `run-endpoint.sh` script:
1. Accepts configurable environment variables for service URL, endpoint path, and credentials location
2. Authenticates with Google using a service account JSON file
3. Makes an API call to the specified endpoint with the obtained token
4. Reports success or failure with response details

## Environment Variables

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `SERVICE_URL` | `https://local.broadinstitute.org:27443` | Base URL of the service |
| `ENDPOINT_PATH` | `/api/endpoint` | API endpoint path to call |
| `CREDENTIALS_PATH` | `/secrets/service-account.json` | Path to mounted service account JSON file |

## Local Testing

To test this container locally:

1. Build the Docker image:
   ```bash
   cd cronjob
   docker build -t consent-cronjob .
   ```

2. Create or copy a service account file for testing:
   ```bash
   cp ../config/consent-service-account.json test-credentials.json
   ```

4. Run the container with your credentials mounted:

   ```bash
   docker run --rm \
    -v "$(pwd)/test-credentials.json:/secrets/service-account.json" \
    consent-cronjob
   ```
