package io.redhat.na.ssp.tasktally.github.ssh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Collections;

import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.transport.sshd.KnownHostsServerKeyVerifier;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

/**
 * Builds an {@link SshdSessionFactory} from in-memory key material.
 */
public final class TaskTallySshdSessionFactory {
  private TaskTallySshdSessionFactory() {
  }

  public static SshdSessionFactory create(byte[] privateKey, byte[] knownHosts, char[] passphrase) throws IOException {
    KeyPair kp = SecurityUtils.loadKeyPairIdentity("key", new ByteArrayInputStream(privateKey), (session, resourceKey, retry) -> passphrase);
    KeyPairProvider provider = session -> Collections.singletonList(kp);

    SshdSessionFactoryBuilder builder = new SshdSessionFactoryBuilder();
    builder.setHomeDirectory(null);
    builder.setServerKeyVerifier(new KnownHostsServerKeyVerifier(new ByteArrayInputStream(knownHosts), true));
    builder.setKeyPairProvider(provider);
    return builder.build(null);
  }
}
