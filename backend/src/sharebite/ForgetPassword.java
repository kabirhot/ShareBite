package sharebite;

import java.util.Map;

/**
 * Forgot password. The page first uses SignIn.sendOtp and SignIn.verifyOtp
 * with purpose RESET, then calls this to set the new password.
 */
public class ForgetPassword {

    /** POST /api/auth/reset-password   body: { email, newPassword } */
    public static Reply resetPassword(String body) {
        Form form = new Form(body);
        String email = form.email("email");
        String newPassword = form.password("newPassword");
        form.check();

        if (!Otp.consumeVerified(email, "RESET")) {
            throw new ApiException(400, "Please verify the OTP first.");
        }

        Map<String, String> user = Database.findOne(Database.USERS, "email", email);
        if (user != null) {
            Database.update(Database.USERS, user.get("id"), "passwordHash", Passwords.hash(newPassword));
            Sessions.endAllOf(user.get("id"));
        }
        // Same reply even if the email is not registered.
        return Reply.message(200, "Password updated. Please log in with your new password.");
    }
}
