package sharebite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Food requests from NGOs and receivers (the "Food Receiver" page). */
public class Receiver {

    /** POST /api/requests  -  field names are the same as the input names in food-receiver.html */
    public static Reply submitInfo(String body, String userId) {
        Form form = new Form(body);
        Map<String, String> request = new LinkedHashMap<>();
        request.put("userId", userId);
        request.put("receiverName", form.required("receiverName", 80));
        request.put("organization", form.required("organization", 120));
        request.put("email", form.email("email"));
        request.put("contact", form.phone("contact"));
        request.put("foodType", form.oneOf("foodType", true,
                "vegetarian", "non-vegetarian", "packaged_foods", "fruits_vegetables"));
        request.put("quantity", form.required("quantity", 60));
        request.put("amount", form.oneOf("amount", true,
                "0-500", "501-1000", "1001-5000", "5001-10000", "10001+"));
        request.put("deliveryLocation", form.required("deliveryLocation", 500));
        request.put("requiredDate", form.dateNotInPast("requiredDate"));
        request.put("deliveryTime", form.time("deliveryTime"));
        request.put("additionalRequirements", form.optional("additionalRequirements", 1000));
        form.check();

        Map<String, String> saved = Database.insert(Database.REQUESTS, request);
        saved.remove("userId");
        return new Reply(201, Json.write(saved));
    }

    /** GET /api/requests  -  the latest 50 food requests, for the dashboard. */
    public static Reply list() {
        List<Map<String, String>> requests = Database.latest(Database.REQUESTS, 50);
        requests.forEach(r -> r.remove("userId"));
        return new Reply(200, Json.write(requests));
    }
}
