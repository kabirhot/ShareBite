package sharebite;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of who is logged in.
 * On login the server creates a long random token and remembers which user it
 * belongs to. The browser sends the token back with every later request
 * in the header:   Authorization: Bearer <token>
 */
public class Sessions {

    private static final int SESSION_SECONDS = 2 * 60 * 60; // stay logged in for 2 hours

    private static class Session {
        String userId;
        Instant expiresAt;
    }

    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final SecureRandom random = new SecureRandom();

    public static String create(String userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Session session = new Session();
        session.userId = userId;
        session.expiresAt = Instant.now().plusSeconds(SESSION_SECONDS);
        sessions.put(token, session);
        return token;
    }

    /** Returns the user id for this token, or null if the token is unknown or expired. */
    public static String getUserId(String token) {
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (Instant.now().isAfter(session.expiresAt)) {
            sessions.remove(token);
            return null;
        }
        return session.userId;
    }

    public static void end(String token) {
        if (token != null) sessions.remove(token);
    }

    /** After a password reset: log that user out everywhere. */
    public static void endAllOf(String userId) {
        sessions.values().removeIf(s -> s.userId.equals(userId));
    }
}
