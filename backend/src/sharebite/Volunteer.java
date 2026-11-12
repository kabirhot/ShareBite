package sharebite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Volunteer registration (the "Volunteers" page). */
public class Volunteer {

    /** POST /api/volunteers  -  field names are the same as the input names in volunteers.html */
    public static Reply submitInfo(String body, String userId) {
        Form form = new Form(body);
        Map<String, String> volunteer = new LinkedHashMap<>();
        volunteer.put("userId", userId);
        volunteer.put("volunteerName", form.required("volunteerName", 80));
        volunteer.put("email", form.email("email"));
        volunteer.put("contact", form.phone("contact"));
        volunteer.put("location", form.required("location", 120));
        volunteer.put("availability", form.oneOf("availability", true,
                "morning", "afternoon", "evening", "full-day"));
        volunteer.put("work", form.oneOf("work", true, "foodpickup", "fooddelivery", "both"));
        volunteer.put("vehicle", form.oneOf("vehicle", false, "yes", "no"));
        volunteer.put("type", form.oneOf("type", false, "bike", "scooter", "car", "other"));
        volunteer.put("area", form.required("area", 120));
        volunteer.put("message", form.optional("message", 1000));
        form.check();

        Map<String, String> saved = Database.insert(Database.VOLUNTEERS, volunteer);
        saved.remove("userId");
        return new Reply(201, Json.write(saved));
    }

    /**
     * GET /api/volunteers/me  -  only the logged-in user's own registrations.
     * Volunteers' phone numbers and emails are not shown to other users.
     */
    public static Reply mine(String userId) {
        List<Map<String, String>> mine = Database.findAll(Database.VOLUNTEERS, "userId", userId);
        mine.forEach(v -> v.remove("userId"));
        return new Reply(200, Json.write(mine));
    }
}
