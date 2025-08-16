package io.redhat.na.ssp.tasktally.github.ssh;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.eclipse.jgit.junit.ssh.SshServerRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@QuarkusTest
class SshGitServiceTest {

  @RegisterExtension
  static SshServerRule server = new SshServerRule();

  @InjectMock
  SecretResolver resolver;

  @jakarta.inject.Inject
  SshGitService service;

  @Test
  void clonesViaSsh() throws Exception {
    CredentialRef cred = new CredentialRef();
    cred.name = "test";
    cred.provider = "git";
    cred.scope = "read";
    cred.secretRef = "key";
    cred.knownHostsRef = "known";
    when(resolver.resolveBytes("key")).thenReturn(server.getPrivateKey());
    when(resolver.resolveBytes("known"))
        .thenReturn(server.getKnownHosts().getBytes(StandardCharsets.UTF_8));
    java.nio.file.Path dir = Files.createTempDirectory("clone");
    service.cloneShallow(server.getUri(), "master", dir, cred);
    assertTrue(Files.exists(dir.resolve(".git")));
  }
}
