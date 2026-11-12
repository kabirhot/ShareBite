package sharebite;

import java.util.Map;

/** Thrown anywhere in the backend to send an error reply with a chosen HTTP status code. */
public class ApiException extends RuntimeException {

    final int status;
    final Map<String, String> errors; // field name -> what is wrong (may be null)

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(int status, String message, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}
