package sharebite;

import java.util.LinkedHashMap;
import java.util.Map;

/** What a handler sends back: an HTTP status code and a JSON text. */
public class Reply {

    final int status;
    final String json;

    public Reply(int status, String json) {
        this.status = status;
        this.json = json;
    }

    /** Shortcut for a reply like {"message":"..."} */
    public static Reply message(int status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        return new Reply(status, Json.write(body));
    }
}
