package io.redhat.na.ssp.tasktally.github.ssh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Collections;

import org.apache.sshd.client.config.keys.KeyIdentityProvider;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

/**
 * Builds an {@link SshdSessionFactory} from in-memory key material.
 */
public final class TaskTallySshdSessionFactory {
  private TaskTallySshdSessionFactory() {
  }

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase)
      throws IOException {
    FilePasswordProvider pwd =
        passphrase == null
            ? FilePasswordProvider.EMPTY
            : (session, resourceKey, retryIndex) -> new String(passphrase);
    KeyPair kp =
        SecurityUtils.loadKeyPairIdentity(
            null, "key", new ByteArrayInputStream(privateKey), pwd);
    KeyIdentityProvider provider = session -> Collections.singletonList(kp);

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(null);
    builder.setServerKeyVerifier(new KnownHostsServerKeyVerifier(new ByteArrayInputStream(knownHosts), true));
    builder.setClientKeyIdentityProvider(provider);
    return builder.build(null);
  }
}
