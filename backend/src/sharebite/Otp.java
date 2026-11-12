package sharebite;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and checks 6-digit OTPs: the generateOTP() and verifyOTP() methods
 * of the console version, shared by SignIn and ForgetPassword.
 * OTPs live in memory only and expire after 5 minutes.
 */
public class Otp {

    private static final int OTP_VALID_SECONDS = 5 * 60;
    private static final int VERIFIED_VALID_SECONDS = 10 * 60; // time to finish the form after verifying
    private static final int MAX_WRONG_ATTEMPTS = 5;
    private static final int RESEND_WAIT_SECONDS = 30;          // wait this long before asking for another OTP

    private static class Entry {
        String code;
        Instant createdAt = Instant.now();
        Instant expiresAt;
        int wrongAttempts;
        boolean verified;
    }

    private static final Map<String, Entry> store = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();

    /** Creates a new OTP for this email. purpose is SIGNUP or RESET. */
    public static String generate(String email, String purpose) {
        Entry previous = store.get(key(email, purpose));
        if (previous != null && Instant.now().isBefore(previous.createdAt.plusSeconds(RESEND_WAIT_SECONDS))) {
            throw new ApiException(429, "An OTP was just sent. Please wait " + RESEND_WAIT_SECONDS
                    + " seconds before asking for another one.");
        }
        Entry entry = new Entry();
        entry.code = String.valueOf(100000 + random.nextInt(900000));
        entry.expiresAt = Instant.now().plusSeconds(OTP_VALID_SECONDS);
        store.put(key(email, purpose), entry);
        return entry.code;
    }

    /** Checks the OTP typed by the user. Throws an error if it is wrong or expired. */
    public static void verify(String email, String purpose, String code) {
        String key = key(email, purpose);
        Entry entry = store.get(key);
        if (entry == null || Instant.now().isAfter(entry.expiresAt)) {
            store.remove(key);
            throw new ApiException(400, "OTP has expired. Please request a new one.");
        }
        synchronized (entry) {
            if (!entry.code.equals(code)) {
                entry.wrongAttempts++;
                if (entry.wrongAttempts >= MAX_WRONG_ATTEMPTS) {
                    store.remove(key);
                    throw new ApiException(400, "Too many wrong attempts. Please request a new OTP.");
                }
                throw new ApiException(400, "Incorrect OTP. Please try again.");
            }
            entry.verified = true;
            entry.expiresAt = Instant.now().plusSeconds(VERIFIED_VALID_SECONDS);
        }
    }

    /**
     * Returns true if this email has a verified OTP, and uses it up,
     * so one OTP can never be used twice.
     */
    public static boolean consumeVerified(String email, String purpose) {
        Entry entry = store.remove(key(email, purpose));
        return entry != null && entry.verified && Instant.now().isBefore(entry.expiresAt);
    }

    private static String key(String email, String purpose) {
        return purpose + ":" + email;
    }
}
