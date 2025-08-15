package io.redhat.na.ssp.tasktally.github;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

public class GitHubAppJwtBuilderTest {
    @Test
    void buildsJwt() throws Exception {
        KeyPair kp = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()) +
                "\n-----END PRIVATE KEY-----";
        GitHubAppJwtBuilder builder = new GitHubAppJwtBuilder();
        String jwt = builder.buildJwt("123", pem);
        SignedJWT parsed = SignedJWT.parse(jwt);
        assertEquals("123", parsed.getJWTClaimsSet().getIssuer());
    }
}
