#!/bin/bash

# Wait for Keycloak to be ready
until curl -s http://localhost:8080/health/ready | grep '"status":"UP"' > /dev/null; do
  echo "Waiting for Keycloak to be ready..."
  sleep 5
done

# Login to Keycloak
/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# Create the client
/opt/keycloak/bin/kcadm.sh create clients -r master -s clientId=task-tally-client \
  -s enabled=true \
  -s publicClient=true \
  -s protocol=openid-connect \
  -s 'redirectUris=["http://localhost:9000/*"]' \
  -s 'webOrigins=["http://localhost:9000"]' \
  -s standardFlowEnabled=true \
  -s attributes.'pkce.code.challenge.method'="S256" \
  -s attributes.'pkce.code.challenge.required'="true"

echo "Keycloak client initialized."
