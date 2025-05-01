#!/bin/bash
set -eo pipefail

# Default values that can be overridden via environment variables
SERVICE_URL=${SERVICE_URL:-"https://local.broadinstitute.org:27443"}
ENDPOINT_PATH=${ENDPOINT_PATH:-"/api/endpoint"}
CREDENTIALS_PATH=${CREDENTIALS_PATH:-"/secrets/service-account.json"}

echo "Starting endpoint execution job..."

# Check if credentials file exists
if [ ! -f "$CREDENTIALS_PATH" ]; then
  echo "Error: Service account credentials file not found at $CREDENTIALS_PATH"
  exit 1
fi

# Get access token using the service account
echo "Authenticating with service account..."
ACCESS_TOKEN=$(curl -s -X POST https://www.googleapis.com/oauth2/v4/token \
  -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" \
  -d "assertion=$(jq -r '.private_key' < "$CREDENTIALS_PATH" | awk '/-----BEGIN PRIVATE KEY-----/ {found=1; next} /-----END PRIVATE KEY-----/ {found=0; next} found {printf "%s", $0} END {print ""}')" \
  |  jq -r '.access_token')

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: Failed to obtain access token"
  exit 1
fi

# Call the service endpoint with the token
echo "Calling service endpoint: $SERVICE_URL$ENDPOINT_PATH"
set +e
RESPONSE=$(curl --no-progress-meter -X POST "$SERVICE_URL$ENDPOINT_PATH" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" 2>&1)
CURL_EXIT_CODE=$?
set -e

if [ $CURL_EXIT_CODE -ne 0 ]; then
  echo "Error: Failed to call service endpoint"
  echo "Response: $RESPONSE"
  exit 1
fi

echo "Endpoint execution completed successfully"
echo "Response: $RESPONSE"