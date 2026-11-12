package sharebite;

import java.util.LinkedHashMap;
import java.util.Map;

/** Logging in and out. */
public class Login {

    /** POST /api/auth/login   body: { email, password } */
    public static Reply verify(String body) {
        Form form = new Form(body);
        String email = form.email("email");
        String password = form.required("password", 72);
        form.check();

        Map<String, String> user = Database.findOne(Database.USERS, "email", email);
        // One message for "email not found" and "wrong password", so the login
        // page cannot be used to find out which emails are registered.
        if (user == null || !Passwords.matches(password, user.get("passwordHash"))) {
            throw new ApiException(401, "Incorrect email or password.");
        }
        return new Reply(200, loggedInReply(user));
    }

    /** POST /api/auth/logout */
    public static Reply logout(String token) {
        Sessions.end(token);
        return Reply.message(200, "Logged out.");
    }

    /** GET /api/auth/me  -  details of the logged-in user (never the password hash). */
    public static Reply me(String userId) {
        Map<String, String> user = Database.findOne(Database.USERS, "id", userId);
        if (user == null) throw new ApiException(401, "Please log in again.");
        user.remove("passwordHash");
        return new Reply(200, Json.write(user));
    }

    /** Starts a session and builds the JSON sent to the browser after login / signup. */
    static String loggedInReply(Map<String, String> user) {
        Map<String, String> reply = new LinkedHashMap<>();
        reply.put("token", Sessions.create(user.get("id")));
        reply.put("name", user.get("name"));
        reply.put("email", user.get("email"));
        reply.put("service", user.get("service"));
        return Json.write(reply);
    }
}
