package nexus.io.maxkb.service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import nexus.io.tio.utils.crypto.Md5Utils;

public final class UserPassword {
  private UserPassword() { }
  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return "pbkdf2$210000$" + Base64.getEncoder().encodeToString(salt) + "$"
        + Base64.getEncoder().encodeToString(derive(password, salt, 210000));
  }
  public static boolean matches(String password, String stored) {
    if (password == null || stored == null) {
      return false;
    }
    try {
      if (!stored.startsWith("pbkdf2$")) {
        return MessageDigest.isEqual(Md5Utils.md5Hex(password).getBytes(java.nio.charset.StandardCharsets.UTF_8), stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      String[] parts = stored.split("\\$");
      int rounds = Integer.parseInt(parts[1]);
      if (parts.length != 4 || rounds < 10000 || rounds > 1000000) {
        return false;
      }
      return MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[2]), rounds), Base64.getDecoder().decode(parts[3]));
    } catch (RuntimeException e) {
      return false;
    }
  }
  private static byte[] derive(String password, byte[] salt, int rounds) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (java.security.GeneralSecurityException e) {
      throw new IllegalStateException("Password hashing unavailable", e);
    } finally {
      spec.clearPassword();
    }
  }
}
