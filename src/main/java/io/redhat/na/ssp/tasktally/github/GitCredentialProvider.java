package io.redhat.na.ssp.tasktally.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;

@ApplicationScoped
public class GitCredentialProvider {

  @Inject
  SecretResolver resolver;

  @Inject
  GitHubAppJwtBuilder jwtBuilder;

  private final ObjectMapper mapper = new ObjectMapper();

  public String provideToken(CredentialRef ref) {
    String payload = resolver.resolve(ref.secretRef);
    try {
      JsonNode node = mapper.readTree(payload);
      if (node.has("token")) {
        return node.get("token").asText();
      }
      if (node.has("appId")) {
        return jwtBuilder.buildJwt(
            node.get("appId").asText(),
            node.get("privateKeyPem").asText());
      }
      throw new IllegalStateException("Unsupported credential payload");
    } catch (IOException e) {
      throw new IllegalStateException("Invalid secret payload", e);
    }
  }
}
