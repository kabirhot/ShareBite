package sharebite;

import java.util.LinkedHashMap;
import java.util.Map;

/** The "Send Us a Message" form on the contact page. No login needed. */
public class Contact {

    /** POST /api/contact   body: { name, email, phone, subject, message } */
    public static Reply submitInfo(String body) {
        Form form = new Form(body);
        Map<String, String> message = new LinkedHashMap<>();
        message.put("name", form.required("name", 80));
        message.put("email", form.email("email"));
        String phone = form.optional("phone", 15);
        if (!phone.isEmpty() && !phone.matches("[0-9+ -]{7,15}")) {
            form.reject("phone", "Enter a valid contact number.");
        }
        message.put("phone", phone);
        message.put("subject", form.oneOf("subject", true, "General Inquiry", "Food Donation",
                "Food Request", "Volunteer", "Partnership", "Technical Issue", "Other"));
        message.put("message", form.required("message", 2000));
        form.check();

        Database.insert(Database.MESSAGES, message);
        return Reply.message(201, "Thank you! Your message has been sent.");
    }
}
