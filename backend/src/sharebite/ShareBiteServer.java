package sharebite;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Primary class. Starts a web server that
 *   1. answers the REST API under /api/...   (JSON in, JSON out)
 *   2. serves the frontend folder, so http://localhost:8080 opens the website.
 * Data is stored in MySQL through JDBC (see Database.java).
 *
 * The web server uses only classes that come with Java (com.sun.net.httpserver).
 * The one extra file needed is the MySQL JDBC driver jar in the lib folder.
 */
public class ShareBiteServer {

    // ---- settings: each can be changed with an environment variable ----
    static final int PORT = Integer.parseInt(env("PORT", "8080"));
    static final Path FRONTEND_DIR = Paths.get(env("FRONTEND_DIR", "../frontend")).toAbsolutePath().normalize();
    /** Other addresses that may call the API. 5500 is the VS Code Live Server port. */
    static final List<String> ALLOWED_ORIGINS = Arrays.asList(
            env("CORS_ALLOWED_ORIGINS", "http://localhost:5500,http://127.0.0.1:5500").split("\\s*,\\s*"));
    /** true = no real email/SMS service, so the OTP is shown on the page and in the console. */
    static final boolean OTP_DEMO_MODE = Boolean.parseBoolean(env("OTP_DEMO_MODE", "true"));

    private static final int MAX_BODY_BYTES = 20_000;

    public static void main(String[] args) throws IOException {
        try {
            Database.connect();
        } catch (Exception e) {
            System.out.println("Could not connect to the MySQL database: " + e.getMessage());
            System.out.println("Check that MySQL is running, that lib/ contains the MySQL connector jar,");
            System.out.println("and that the user and password in backend/db.properties are correct.");
            return;
        }
        System.out.println("Connected to the MySQL database.");
        Mailer.load();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", ShareBiteServer::handle);
        server.setExecutor(Executors.newFixedThreadPool(16)); // serve up to 16 requests at the same time
        server.start();

        System.out.println("ShareBite is running.");
        System.out.println("  Website : http://localhost:" + PORT + "/");
        System.out.println("  API     : http://localhost:" + PORT + "/api/health");
        if (Mailer.isConfigured()) {
            System.out.println("  OTP emails are ON (sent through the account in mail.properties).");
        } else if (OTP_DEMO_MODE) {
            System.out.println("  OTP demo mode is ON (OTPs are printed here and shown on the page).");
        }
        System.out.println("Press Ctrl+C to stop.");
    }

    /** Every request comes here first. */
    private static void handle(HttpExchange exchange) throws IOException {
        try {
            addSecurityHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api") || path.startsWith("/api/")) {
                handleApi(exchange, path);
            } else {
                serveFrontendFile(exchange, path);
            }
        } catch (Exception e) {
            // Unexpected problem: details go to the server console only, never to the browser.
            e.printStackTrace();
            send(exchange, 500, "application/json; charset=utf-8",
                    Reply.message(500, "Something went wrong on the server. Please try again.").json
                            .getBytes(StandardCharsets.UTF_8));
        } finally {
            exchange.close();
        }
    }

    // ================================================================ REST API

    private static void handleApi(HttpExchange exchange, String path) throws IOException {
        Reply reply;
        try {
            if (addCorsHeaders(exchange)) {
                exchange.sendResponseHeaders(204, -1); // browser's CORS "pre-flight" check: nothing more to do
                return;
            }
            reply = route(exchange, exchange.getRequestMethod(), path);
        } catch (ApiException e) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("message", e.getMessage());
            String json = Json.write(body);
            if (e.errors != null) {
                // add  "errors": { "fieldName": "what is wrong" }
                json = json.substring(0, json.length() - 1) + ",\"errors\":" + Json.write(e.errors) + "}";
            }
            reply = new Reply(e.status, json);
        }
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        send(exchange, reply.status, "application/json; charset=utf-8", reply.json.getBytes(StandardCharsets.UTF_8));
    }

    /** The list of API endpoints: which class handles which URL. */
    private static Reply route(HttpExchange exchange, String method, String path) throws IOException {
        String body = "";
        if (method.equals("POST")) {
            checkRateLimit(exchange);
            body = readBody(exchange);
        }

        switch (method + " " + path) {
            // ---- open to everyone ----
            case "GET /api/health":               return Reply.message(200, "ok");
            case "POST /api/auth/otp/send":       return SignIn.sendOtp(body);
            case "POST /api/auth/otp/verify":     return SignIn.verifyOtp(body);
            case "POST /api/auth/signup":         return SignIn.submitInfo(body);
            case "POST /api/auth/login":          return Login.verify(body);
            case "POST /api/auth/reset-password": return ForgetPassword.resetPassword(body);
            case "POST /api/contact":             return Contact.submitInfo(body);

            // ---- login needed: requireLogin() stops the request if there is no valid token ----
            case "POST /api/auth/logout":         requireLogin(exchange); return Login.logout(readToken(exchange));
            case "GET /api/auth/me":              return Login.me(requireLogin(exchange));
            case "POST /api/donations":           return Donor.submitInfo(body, requireLogin(exchange));
            case "GET /api/donations":            requireLogin(exchange); return Donor.list();
            case "POST /api/requests":            return Receiver.submitInfo(body, requireLogin(exchange));
            case "GET /api/requests":             requireLogin(exchange); return Receiver.list();
            case "POST /api/volunteers":          return Volunteer.submitInfo(body, requireLogin(exchange));
            case "GET /api/volunteers/me":        return Volunteer.mine(requireLogin(exchange));

            default: throw new ApiException(404, "This API address does not exist.");
        }
    }

    /** Returns the logged-in user's id, or stops the request with 401. */
    private static String requireLogin(HttpExchange exchange) {
        String userId = Sessions.getUserId(readToken(exchange));
        if (userId == null) throw new ApiException(401, "Please log in to continue.");
        return userId;
    }

    private static String readToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7).trim();
        return null;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, "Send the data as JSON (Content-Type: application/json).");
        }
        try (InputStream in = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > MAX_BODY_BYTES) throw new ApiException(413, "The request is too large.");
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    // ---------------------------------------------------------------- CORS
    /**
     * A browser blocks a page on one address (for example Live Server on port 5500)
     * from calling an API on another address (port 8080) unless the API says that
     * address is allowed. Only the origins in ALLOWED_ORIGINS are allowed.
     * Returns true if this was only the browser's pre-flight (OPTIONS) check.
     */
    private static boolean addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        Headers headers = exchange.getResponseHeaders();
        headers.set("Vary", "Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            headers.set("Access-Control-Allow-Origin", origin);
            headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            headers.set("Access-Control-Max-Age", "3600");
        }
        return exchange.getRequestMethod().equals("OPTIONS");
    }

    // ---------------------------------------------------------------- rate limit
    /**
     * Stops one computer from hammering the API (password guessing, OTP guessing,
     * form spam): at most 30 POST requests per minute from one IP address.
     */
    private static final int MAX_POSTS_PER_MINUTE = 30;
    private static final Map<String, long[]> postCounters = new ConcurrentHashMap<>(); // ip -> {windowStart, count}

    private static void checkRateLimit(HttpExchange exchange) {
        long now = System.currentTimeMillis();
        if (postCounters.size() > 10_000) {
            postCounters.values().removeIf(c -> now - c[0] > 60_000); // forget finished minutes
        }
        String ip = clientIp(exchange);
        long[] counter = postCounters.computeIfAbsent(ip, k -> new long[]{now, 0});
        synchronized (counter) {
            if (now - counter[0] > 60_000) {
                counter[0] = now;
                counter[1] = 0;
            }
            counter[1]++;
            if (counter[1] > MAX_POSTS_PER_MINUTE) {
                throw new ApiException(429, "Too many requests. Please wait a minute and try again.");
            }
        }
    }

    /**
     * The visitor's IP address. When the site is shared through a Cloudflare tunnel,
     * every request arrives from this same computer (127.0.0.1), so the real visitor's
     * address is read from the CF-Connecting-IP header that Cloudflare adds.
     * That header is trusted only for requests coming from this computer.
     */
    private static String clientIp(HttpExchange exchange) {
        java.net.InetAddress remote = exchange.getRemoteAddress().getAddress();
        String forwarded = exchange.getRequestHeaders().getFirst("CF-Connecting-IP");
        if (remote.isLoopbackAddress() && forwarded != null && !forwarded.isBlank()) {
            return forwarded.trim();
        }
        return remote.getHostAddress();
    }

    // ================================================================ frontend files

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "webp", "image/webp",
            "ico", "image/x-icon");

    private static void serveFrontendFile(HttpExchange exchange, String path) throws IOException {
        if (!exchange.getRequestMethod().equals("GET") && !exchange.getRequestMethod().equals("HEAD")) {
            send(exchange, 405, "text/plain; charset=utf-8", "Method not allowed".getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (path.equals("/")) path = "/index.html";

        // Only files inside the frontend folder may be read. normalize() removes "../"
        // tricks, and startsWith() then makes sure we are still inside that folder.
        Path file = FRONTEND_DIR.resolve(path.substring(1)).normalize();
        String name = file.getFileName() == null ? "" : file.getFileName().toString();
        String extension = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        boolean allowed = file.startsWith(FRONTEND_DIR) && !name.startsWith(".")
                && CONTENT_TYPES.containsKey(extension) && Files.isRegularFile(file);

        if (!allowed) {
            Path notFound = FRONTEND_DIR.resolve("404.html");
            byte[] page = Files.isRegularFile(notFound)
                    ? Files.readAllBytes(notFound)
                    : "Page not found".getBytes(StandardCharsets.UTF_8);
            send(exchange, 404, "text/html; charset=utf-8", page);
            return;
        }
        send(exchange, 200, CONTENT_TYPES.get(extension), Files.readAllBytes(file));
    }

    // ================================================================ helpers

    /** Standard headers that tell the browser to be strict with our pages. */
    private static void addSecurityHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        // Only scripts from our own .js files may run. Styles are also allowed inline
        // because the HTML pages keep their CSS in <style> blocks.
        headers.set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    }

    private static void send(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (exchange.getRequestMethod().equals("HEAD")) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
