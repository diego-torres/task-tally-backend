package io.redhat.na.ssp.tasktally.github.ssh;

import io.redhat.na.ssp.tasktally.model.CredentialRef;
import io.redhat.na.ssp.tasktally.secret.SecretResolver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

/**
 * Git operations over SSH using JGit.
 */
@ApplicationScoped
public class SshGitService {
  @Inject SecretResolver resolver;

  private SshdSessionFactory factoryFor(CredentialRef cred) throws IOException {
    byte[] key = resolver.resolveBytes(cred.getSecretRef());
    byte[] known = resolver.resolveBytes(cred.getKnownHostsRef());
    char[] pass = cred.getPassphraseRef() != null ? resolver.resolve(cred.getPassphraseRef()).toCharArray() : null;
    return TaskTallySshdSessionFactory.create(key, known, pass);
  }

  private TransportConfigCallback callback(SshdSessionFactory factory) {
    return (Transport transport) -> {
      SshTransport ssh = (SshTransport) transport;
      ssh.setSshSessionFactory(factory);
    };
  }

  public Path cloneShallow(String uri, String branch, Path dir, CredentialRef cred) throws GitAPIException, IOException {
    SshdSessionFactory fac = factoryFor(cred);
    return Git.cloneRepository()
        .setURI(uri)
        .setBranch(branch)
        .setDepth(1)
        .setDirectory(dir.toFile())
        .setTransportConfigCallback(callback(fac))
        .call()
        .getRepository()
        .getDirectory()
        .toPath();
  }

  public void commitAndPush(Path dir, String authorName, String authorEmail, String message, CredentialRef cred) throws IOException, GitAPIException {
    SshdSessionFactory fac = factoryFor(cred);
    try (Git git = Git.open(dir.toFile())) {
      git.add().addFilepattern(".").call();
      git.commit().setAuthor(authorName, authorEmail).setMessage(message).call();
      git.push().setTransportConfigCallback(callback(fac)).call();
    }
  }
}
