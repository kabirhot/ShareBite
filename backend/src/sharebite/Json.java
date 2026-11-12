package sharebite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A very small JSON reader and writer, so the project needs no external library.
 * It handles what this project sends and receives: flat objects such as
 * {"email":"a@b.com","plates":20} and lists of such objects.
 * Every value is kept as a String.
 */
public class Json {

    // ---------------------------------------------------------------- writing

    public static String write(Map<String, String> object) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : object.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(quote(e.getKey())).append(":").append(quote(e.getValue()));
        }
        return sb.append("}").toString();
    }

    public static String write(List<Map<String, String>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(write(list.get(i)));
        }
        return sb.append("]").toString();
    }

    /** Puts a text inside "..." and escapes the characters JSON does not allow as-is. */
    public static String quote(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    // '<' is escaped too, so data can never close a <script> tag if it is ever put in HTML
                    if (c < 0x20 || c == '<') sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    // ---------------------------------------------------------------- reading

    public static Map<String, String> parseObject(String text) {
        Json parser = new Json(text);
        parser.skipSpaces();
        Map<String, String> result = parser.readObject();
        parser.skipSpaces();
        if (parser.pos != parser.text.length()) throw parser.error();
        return result;
    }

    public static List<Map<String, String>> parseArray(String text) {
        Json parser = new Json(text);
        List<Map<String, String>> result = new ArrayList<>();
        parser.skipSpaces();
        parser.expect('[');
        parser.skipSpaces();
        if (parser.peek() == ']') {
            parser.pos++;
            return result;
        }
        while (true) {
            parser.skipSpaces();
            result.add(parser.readObject());
            parser.skipSpaces();
            if (parser.peek() == ',') { parser.pos++; continue; }
            parser.expect(']');
            return result;
        }
    }

    private final String text;
    private int pos = 0;

    private Json(String text) {
        this.text = text == null ? "" : text;
    }

    private Map<String, String> readObject() {
        Map<String, String> map = new LinkedHashMap<>();
        expect('{');
        skipSpaces();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipSpaces();
            String key = readString();
            skipSpaces();
            expect(':');
            skipSpaces();
            map.put(key, readValue());
            skipSpaces();
            if (peek() == ',') { pos++; continue; }
            expect('}');
            return map;
        }
    }

    private String readValue() {
        char c = peek();
        if (c == '"') return readString();
        if (c == '{' || c == '[') throw error(); // nested data is not used in this project
        int start = pos;
        while (pos < text.length() && ",}] \n\r\t".indexOf(text.charAt(pos)) < 0) pos++;
        String word = text.substring(start, pos);
        if (word.equals("null")) return "";
        if (word.equals("true") || word.equals("false") || word.matches("-?[0-9]+(\\.[0-9]+)?")) return word;
        throw error();
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) throw error();
            char c = text.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c != '\\') { sb.append(c); continue; }
            if (pos >= text.length()) throw error();
            char e = text.charAt(pos++);
            switch (e) {
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    if (pos + 4 > text.length()) throw error();
                    try {
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw error();
                    }
                    pos += 4;
                    break;
                case '"': case '\\': case '/': sb.append(e); break;
                default: throw error();
            }
        }
    }

    private char peek() {
        if (pos >= text.length()) throw error();
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) throw error();
        pos++;
    }

    private void skipSpaces() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
    }

    private ApiException error() {
        return new ApiException(400, "The request could not be read. Please check the form values.");
    }
}
