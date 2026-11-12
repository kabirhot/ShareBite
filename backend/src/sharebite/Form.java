package sharebite;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checks the values sent by a form: the validateInfo() methods of the console
 * version. The browser also checks the form, but the server must check again
 * because anyone can send a request without using our pages.
 *
 * Use: read each field with one of the methods below, then call check().
 * Every problem is collected, so the user sees all mistakes at once.
 */
public class Form {

    private final Map<String, String> data;
    private final Map<String, String> errors = new LinkedHashMap<>();

    public Form(String jsonBody) {
        this.data = Json.parseObject(jsonBody);
    }

    /** A field that must be filled in, with a maximum length. */
    public String required(String name, int maxLength) {
        String value = optional(name, maxLength);
        if (value.isEmpty()) errors.putIfAbsent(name, "This field is required.");
        return value;
    }

    /** A field that may be left empty. */
    public String optional(String name, int maxLength) {
        String value = data.getOrDefault(name, "").trim();
        if (value.length() > maxLength) errors.put(name, "Must be at most " + maxLength + " characters.");
        return value;
    }

    public String email(String name) {
        String value = required(name, 100).toLowerCase();
        if (!value.isEmpty() && !value.matches("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}")) {
            errors.putIfAbsent(name, "Enter a valid email address.");
        }
        return value;
    }

    public String phone(String name) {
        String value = required(name, 10);
        if (!value.isEmpty() && !value.matches("[0-9]{10}")) {
            errors.putIfAbsent(name, "Enter a 10-digit phone number.");
        }
        return value;
    }

    /** Password is NOT trimmed: spaces are allowed in passwords. */
    public String password(String name) {
        String value = data.getOrDefault(name, "");
        if (value.length() < 8 || value.length() > 72) {
            errors.put(name, "Password must be 8 to 72 characters.");
        }
        return value;
    }

    /** A drop-down: the value must be one of the allowed options. */
    public String oneOf(String name, boolean isRequired, String... allowed) {
        String value = isRequired ? required(name, 40) : optional(name, 40);
        if (!value.isEmpty() && !Arrays.asList(allowed).contains(value)) {
            errors.putIfAbsent(name, "Choose one of the listed options.");
        }
        return value;
    }

    public String wholeNumber(String name, int min, int max) {
        String value = required(name, 9);
        if (value.isEmpty()) return value;
        if (!value.matches("[0-9]+") || Integer.parseInt(value) < min || Integer.parseInt(value) > max) {
            errors.putIfAbsent(name, "Enter a number from " + min + " to " + max + ".");
        }
        return value;
    }

    /** A date like 2026-10-05 that is today or later. */
    public String dateNotInPast(String name) {
        String value = required(name, 10);
        if (value.isEmpty()) return value;
        try {
            if (LocalDate.parse(value).isBefore(LocalDate.now())) {
                errors.putIfAbsent(name, "The date cannot be in the past.");
            }
        } catch (DateTimeParseException e) {
            errors.putIfAbsent(name, "Enter a valid date.");
        }
        return value;
    }

    /** A time like 14:30. */
    public String time(String name) {
        String value = required(name, 8);
        if (value.isEmpty()) return value;
        try {
            LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            errors.putIfAbsent(name, "Enter a valid time.");
        }
        return value;
    }

    /** Lets a handler report its own problem with a field. */
    public void reject(String name, String message) {
        errors.putIfAbsent(name, message);
    }

    /** Call after reading all fields. Stops the request if anything was wrong. */
    public void check() {
        if (!errors.isEmpty()) {
            throw new ApiException(400, "Please correct the highlighted details and try again.", errors);
        }
    }
}
