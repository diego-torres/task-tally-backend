package io.redhat.na.ssp.tasktally.secrets.k8s;

import io.redhat.na.ssp.tasktally.secrets.SecretWriter;
import io.redhat.na.ssp.tasktally.secrets.SshSecretRefs;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.Arrays;

/**
 * Writes SSH key material to a Kubernetes-style secret directory.
 */
@ApplicationScoped
public class KubernetesSecretWriter implements SecretWriter {
  private final Path basePath;

  public KubernetesSecretWriter(@ConfigProperty(name = "secrets.base-path") String basePathString) {
    this.basePath = Paths.get(basePathString);
  }

  @Override
  public SshSecretRefs writeSshKey(String userId, String name, byte[] privateKeyPem, byte[] publicKeyOpenSsh,
      char[] passphrase, byte[] knownHosts) {
    String slug = slug(name);
    String secretName = "tasktally-ssh-" + userId + "-" + slug;
    Path dir = basePath.resolve(secretName);
    try {
      Files.createDirectories(dir);
      Files.write(dir.resolve("id_ed25519"), privateKeyPem);
      if (publicKeyOpenSsh != null) {
        Files.write(dir.resolve("id_ed25519.pub"), ensureNewline(publicKeyOpenSsh));
      }
      if (passphrase != null) {
        Files.writeString(dir.resolve("passphrase"), new String(passphrase), StandardCharsets.UTF_8);
      }
      if (knownHosts != null) {
        Files.write(dir.resolve("known_hosts"), ensureNewline(knownHosts));
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to write secret", e);
    }
    String privateRef = "k8s:secret/" + secretName + "#id_ed25519";
    String knownHostsRef = knownHosts != null ? "k8s:secret/" + secretName + "#known_hosts" : null;
    String passphraseRef = passphrase != null ? "k8s:secret/" + secretName + "#passphrase" : null;
    return new SshSecretRefs(privateRef, knownHostsRef, passphraseRef);
  }

  @Override
  public void deleteByRef(String secretRef) {
    if (secretRef == null || !secretRef.startsWith("k8s:secret/")) {
      return;
    }
    String body = secretRef.substring("k8s:secret/".length());
    String[] parts = body.split("#", 2);
    if (parts.length != 2) {
      return;
    }
    Path file = basePath.resolve(parts[0]).resolve(parts[1]);
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      // ignore
    }
  }

  private String slug(String in) {
    String norm = Normalizer.normalize(in, Normalizer.Form.NFD).replaceAll("[^A-Za-z0-9]", "-").toLowerCase();
    return norm.replaceAll("-+", "-");
  }

  private byte[] ensureNewline(byte[] data) {
    if (data.length == 0 || data[data.length - 1] == '\n') {
      return data;
    }
    byte[] out = Arrays.copyOf(data, data.length + 1);
    out[data.length] = (byte) '\n';
    return out;
  }
}
