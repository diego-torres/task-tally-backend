package io.redhat.na.ssp.tasktally.secrets;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class KubernetesSecretResolverTest {

    @Test
    void resolvesFile() throws Exception {
        Path base = Files.createTempDirectory("k8ssec");
        Path secret = base.resolve("mysecret");
        Files.createDirectories(secret);
        Files.writeString(secret.resolve("token"), "abc");
        System.setProperty("k8s.secret.base.path", base.toString());
        KubernetesSecretResolver r = new KubernetesSecretResolver();
        assertEquals("abc", r.resolve("k8s:secret/mysecret#token"));
    }

    @Test
    void resolvesEnv() {
        System.setProperty("k8s.secret.base.path", "/nope");
        System.setProperty("env.MYSECRET_TOKEN", "xyz");
        KubernetesSecretResolver r = new KubernetesSecretResolver();
        assertEquals("xyz", r.resolve("k8s:secret/mysecret#token"));
    }
}
