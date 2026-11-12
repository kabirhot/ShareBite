package sharebite;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Passwords are never stored as plain text. We store a salted PBKDF2 hash
 * (a standard algorithm that ships with Java), so even someone who reads
 * users.json cannot see the passwords.
 */
public class Passwords {

    private static final int ITERATIONS = 310_000;
    private static final SecureRandom random = new SecureRandom();

    /** Returns "iterations:salt:hash" for saving in the database. */
    public static String hash(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    /** Checks a typed password against a saved hash. */
    public static boolean matches(String password, String saved) {
        try {
            String[] parts = saved.split(":");
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(password, salt, Integer.parseInt(parts[0]));
            return MessageDigest.isEqual(expected, actual); // comparison that takes the same time for right and wrong
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
