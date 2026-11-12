package sharebite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Food donations (the "Food Donor" page). */
public class Donor {

    /** POST /api/donations  -  field names are the same as the input names in food-donor.html */
    public static Reply submitInfo(String body, String userId) {
        Form form = new Form(body);
        Map<String, String> donation = new LinkedHashMap<>();
        donation.put("userId", userId); // from the login token, never from the request body
        donation.put("donorName", form.required("donorName", 80));
        donation.put("contact", form.phone("contact"));
        donation.put("foodName", form.required("foodName", 120));
        donation.put("plates", form.wholeNumber("plates", 1, 10000));
        donation.put("foodType", form.oneOf("foodType", true,
                "vegetarian", "non-vegetarian", "packaged_foods", "fruits_vegetables"));
        donation.put("pickupAddress", form.required("pickupAddress", 500));
        donation.put("confirmAddress", form.required("confirmAddress", 500));
        donation.put("pickupDate", form.dateNotInPast("pickupDate"));
        donation.put("pickupTime", form.time("pickupTime"));
        donation.put("additionalInfo", form.optional("additionalInfo", 1000));

        // Same check as the console version: both addresses must match.
        if (!donation.get("pickupAddress").equalsIgnoreCase(donation.get("confirmAddress"))) {
            form.reject("confirmAddress", "The two pickup addresses do not match.");
        }
        form.check();

        return new Reply(201, Json.write(hideOwner(Database.insert(Database.DONATIONS, donation))));
    }

    /** GET /api/donations  -  the latest 50 donations, for the dashboard. */
    public static Reply list() {
        List<Map<String, String>> donations = Database.latest(Database.DONATIONS, 50);
        donations.forEach(Donor::hideOwner);
        return new Reply(200, Json.write(donations));
    }

    private static Map<String, String> hideOwner(Map<String, String> record) {
        record.remove("userId");
        return record;
    }
}
