package io.redhat.na.ssp.tasktally.secrets;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SshKeyValidatorTest {
  @Test
  void rejectsMissingPemMarkers() {
    byte[] pem = "no markers".getBytes();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      SshKeyValidator.validatePrivateKey(pem);
    });
    assertTrue(ex.getMessage().contains("Invalid PEM"));
  }

  @Test
  void rejectsOversizedKey() {
    byte[] pem = new byte[11 * 1024];
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
      SshKeyValidator.validatePrivateKey(pem);
    });
    assertTrue(ex.getMessage().contains("size"));
  }
}
