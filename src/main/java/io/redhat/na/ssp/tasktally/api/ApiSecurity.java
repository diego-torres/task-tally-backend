package io.redhat.na.ssp.tasktally.api;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(
  name = "keycloak",
  type = SecuritySchemeType.OAUTH2,
  flows = @OAuthFlows(
    authorizationCode = @OAuthFlow(
      authorizationUrl = "http://localhost:8080/realms/tasktally/protocol/openid-connect/auth",
      tokenUrl = "http://localhost:8080/realms/tasktally/protocol/openid-connect/token",
      scopes = {
        @OAuthScope(name = "openid", description = "OpenID Connect")
      }
    )
  )
)
public class ApiSecurity {}
