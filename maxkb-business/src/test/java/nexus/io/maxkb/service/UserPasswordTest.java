package nexus.io.maxkb.service;

import static org.junit.Assert.*;
import org.junit.Test;

public class UserPasswordTest {
  @Test public void saltedPasswordsVerifyWithoutDeterministicHashes() {
    String first = UserPassword.hash("test-only-password");
    assertTrue(UserPassword.matches("test-only-password", first));
    assertFalse(UserPassword.matches("incorrect-password", first));
    assertNotEquals(first, UserPassword.hash("test-only-password"));
  }
  @Test public void existingAccountsRemainCompatibleAndMalformedHashesFailClosed() {
    assertTrue(UserPassword.matches("legacy", nexus.io.tio.utils.crypto.Md5Utils.md5Hex("legacy")));
    assertFalse(UserPassword.matches("legacy", "pbkdf2$bad"));
    assertFalse(UserPassword.matches(null, "anything"));
  }
}
