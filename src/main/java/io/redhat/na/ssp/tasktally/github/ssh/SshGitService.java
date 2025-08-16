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
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.jboss.logging.Logger;

/**
 * Git operations over SSH using JGit.
 */
@ApplicationScoped
public class SshGitService {
  private static final Logger LOG = Logger.getLogger(SshGitService.class);
  @Inject SecretResolver resolver;

  private SshdSessionFactory factoryFor(CredentialRef cred) throws IOException {
    if (cred == null) {
      LOG.debug("Using default SSH configuration");
      java.io.File home = new java.io.File(System.getProperty("user.home"));
      return new SshdSessionFactoryBuilder()
          .setHomeDirectory(home)
          .setSshDirectory(new java.io.File(home, ".ssh"))
          .build(null);
    }
    LOG.debugf("Using credential %s", cred.getName());
    byte[] key = resolver.resolveBytes(cred.getSecretRef());
    byte[] known = cred.getKnownHostsRef() != null ? resolver.resolveBytes(cred.getKnownHostsRef()) : new byte[0];
    char[] pass = cred.getPassphraseRef() != null ? resolver.resolve(cred.getPassphraseRef()).toCharArray() : null;
    return TaskTallySshdSessionFactory.create(key, known, pass, null);
  }

  private TransportConfigCallback callback(SshdSessionFactory factory) {
    return (Transport transport) -> {
      SshTransport ssh = (SshTransport) transport;
      ssh.setSshSessionFactory(factory);
    };
  }

  public Path cloneShallow(String uri, String branch, Path dir, CredentialRef cred) throws GitAPIException, IOException {
    LOG.debugf("Cloning %s (branch %s)", uri, branch);
    SshdSessionFactory fac = factoryFor(cred);
    try {
      Git git = Git.cloneRepository()
          .setURI(uri)
          .setBranch(branch)
          .setDepth(1)
          .setDirectory(dir.toFile())
          .setTransportConfigCallback(callback(fac))
          .call();
      git.getRepository().close();
      git.close();
      LOG.infof("Cloned repository %s", uri);
      return dir;
    } catch (GitAPIException e) {
      LOG.errorf("Failed to clone %s", uri, e);
      throw e;
    }
  }

  public void commitAndPush(Path dir, String authorName, String authorEmail, String message, CredentialRef cred) throws IOException, GitAPIException {
    LOG.debugf("Committing and pushing in %s", dir);
    SshdSessionFactory fac = factoryFor(cred);
    try (Git git = Git.open(dir.toFile())) {
      git.add().addFilepattern(".").call();
      git.commit().setAuthor(authorName, authorEmail).setMessage(message).call();
      git.push().setTransportConfigCallback(callback(fac)).call();
      LOG.info("Push successful");
    } catch (GitAPIException | IOException e) {
      LOG.error("Push failed", e);
      throw e;
    }
  }
}
