package sharebite;

import java.util.LinkedHashMap;
import java.util.Map;

/** Creating a new account (the "Sign In" page): send OTP, verify OTP, then submit. */
public class SignIn {

    /** POST /api/auth/otp/send   body: { email, purpose }   purpose is SIGNUP or RESET */
    public static Reply sendOtp(String body) {
        Form form = new Form(body);
        String email = form.email("email");
        String purpose = form.oneOf("purpose", true, "SIGNUP", "RESET");
        form.check();

        String otp = Otp.generate(email, purpose);

        Map<String, String> reply = new LinkedHashMap<>();
        if (Mailer.isConfigured()) {
            // Real mode: email the OTP. For a password reset, only registered emails get one,
            // but the reply is the same either way, so nobody can test which emails are registered.
            boolean registered = Database.findOne(Database.USERS, "email", email) != null;
            if (purpose.equals("SIGNUP") || registered) {
                Mailer.send(email, "Your ShareBite OTP is " + otp,
                        "Your ShareBite OTP is " + otp + "\n\n"
                                + "It is valid for 5 minutes. Do not share it with anyone.\n\n"
                                + "If you did not ask for this, you can ignore this email.");
            }
            reply.put("message", "OTP sent to " + email + ". It is valid for 5 minutes.");
        } else if (ShareBiteServer.OTP_DEMO_MODE) {
            // Demo mode: no email account is set up (no mail.properties), so the OTP
            // is printed in the server console and returned to the page.
            System.out.println("DEMO MODE - OTP for " + email + " (" + purpose + "): " + otp);
            reply.put("message", "OTP sent. It is valid for 5 minutes.");
            reply.put("demoOtp", otp);
        } else {
            throw new ApiException(503, "OTP emails are not set up on this server yet.");
        }
        return new Reply(200, Json.write(reply));
    }

    /** POST /api/auth/otp/verify   body: { email, purpose, otp } */
    public static Reply verifyOtp(String body) {
        Form form = new Form(body);
        String email = form.email("email");
        String purpose = form.oneOf("purpose", true, "SIGNUP", "RESET");
        String otp = form.required("otp", 6);
        form.check();

        Otp.verify(email, purpose, otp);
        return Reply.message(200, "OTP verified.");
    }

    /** POST /api/auth/signup   body: { name, email, phone, password, service } */
    public static Reply submitInfo(String body) {
        Form form = new Form(body);
        String name = form.required("name", 80);
        String email = form.email("email");
        String phone = form.phone("phone");
        String password = form.password("password");
        String service = form.oneOf("service", true, "fooddonor", "foodreceiver", "volunteers");
        form.check();

        if (Database.findOne(Database.USERS, "email", email) != null) {
            throw new ApiException(409, "This email is already registered. Please log in instead.");
        }
        if (!Otp.consumeVerified(email, "SIGNUP")) {
            throw new ApiException(400, "Please verify your email with the OTP first.");
        }

        Map<String, String> user = new LinkedHashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("passwordHash", Passwords.hash(password));
        user.put("service", service);
        user = Database.insert(Database.USERS, user);

        return new Reply(201, Login.loggedInReply(user)); // new users are logged in straight away
    }
}
