#!/bin/bash

# Wait for Keycloak to be ready (using Bash TCP port check)
until echo > /dev/tcp/keycloak/8080 2>/dev/null; do
  echo "Waiting for Keycloak to be ready..."
  sleep 3
done

# Login to Keycloak
/opt/keycloak/bin/kcadm.sh config credentials --server http://keycloak:8080 --realm master --user "$KEYCLOAK_ADMIN" --password "$KEYCLOAK_ADMIN_PASSWORD"

REALM=tasktally
KC=http://keycloak:8080

# Create realm
/opt/keycloak/bin/kcadm.sh create realms -s realm=$REALM -s enabled=true || true

# Backend service client
auth_backend="clients -r $REALM -s clientId=task-tally-backend -s enabled=true -s protocol=openid-connect -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false -s directAccessGrantsEnabled=false"
/opt/keycloak/bin/kcadm.sh create $auth_backend || true

# Swagger public client with PKCE
/opt/keycloak/bin/kcadm.sh create clients -r $REALM -s clientId=task-tally-swagger -s enabled=true -s protocol=openid-connect -s publicClient=true -s standardFlowEnabled=true -s 'attributes."pkce.code.challenge.method"=S256' -s 'attributes."pkce.code.challenge.required"=true' -s 'redirectUris=["http://localhost:8080/q/swagger-ui/oauth2-redirect.html"]' -s 'webOrigins=["http://localhost:8080"]' || true

SWAGGER_ID=$(/opt/keycloak/bin/kcadm.sh get clients -r $REALM -q clientId=task-tally-swagger --fields id --format csv --noquotes)
/opt/keycloak/bin/kcadm.sh create clients/$SWAGGER_ID/protocol-mappers/models -r $REALM -s name=aud-task-tally-backend -s protocol=openid-connect -s protocolMapper=oidc-audience-mapper -s 'config."included.client.audience"=task-tally-backend' -s 'config."id.token.claim"=false' -s 'config."access.token.claim"=true' || true

# Roles
/opt/keycloak/bin/kcadm.sh create roles -r $REALM -s name=user || true
/opt/keycloak/bin/kcadm.sh create roles -r $REALM -s name=admin || true

# Users
for u in alice admin; do
  /opt/keycloak/bin/kcadm.sh create users -r $REALM -s username=$u -s enabled=true || true
  /opt/keycloak/bin/kcadm.sh set-password -r $REALM --username $u --new-password $u
 done

/opt/keycloak/bin/kcadm.sh add-roles -r $REALM --uusername alice --rolename user
/opt/keycloak/bin/kcadm.sh add-roles -r $REALM --uusername admin --rolename user
/opt/keycloak/bin/kcadm.sh add-roles -r $REALM --uusername admin --rolename admin

echo "Keycloak realm initialized."
