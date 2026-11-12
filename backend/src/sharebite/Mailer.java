package sharebite;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Properties;

/**
 * Sends emails (used for the OTP) through an SMTP mail server such as Gmail.
 *
 * SMTP is the standard protocol for sending email. The program opens an
 * encrypted (SSL) connection to the mail server and has a short conversation:
 *   EHLO -> AUTH LOGIN (user, password) -> MAIL FROM -> RCPT TO -> DATA (the message) -> QUIT
 * After each line the server answers with a number; 2xx / 3xx means OK.
 *
 * Settings are read from backend/mail.properties. If that file is missing,
 * the project stays in OTP demo mode (OTP shown on the page).
 */
public class Mailer {

    private static String host;
    private static int port;
    private static String user;
    private static String password;

    /** Reads mail.properties. Called once when the server starts. */
    public static void load() throws IOException {
        Properties settings = new Properties();
        Path file = Paths.get("mail.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                settings.load(in);
            }
        }
        host = settings.getProperty("mail.host", "smtp.gmail.com").trim();
        port = Integer.parseInt(settings.getProperty("mail.port", "587").trim());
        user = settings.getProperty("mail.user", "").trim();
        // Gmail shows app passwords as "abcd efgh ijkl mnop"; the spaces are not part of it.
        password = settings.getProperty("mail.password", "").replace(" ", "");
        if (user.startsWith("YOUR_") || password.startsWith("YOUR_")) {
            user = "";
            password = "";
        }
    }

    /** true if an email account has been set up in mail.properties. */
    public static boolean isConfigured() {
        return user != null && !user.isEmpty() && !password.isEmpty();
    }

    /** Sends a plain-text email. Throws an ApiException if the mail server refuses it. */
    public static void send(String to, String subject, String body) {
        try (Socket socket = connect()) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            command(out, in, "EHLO localhost", "250");
            command(out, in, "AUTH LOGIN", "334");
            command(out, in, base64(user), "334");
            command(out, in, base64(password), "235");           // 235 = login accepted
            command(out, in, "MAIL FROM:<" + user + ">", "250");
            command(out, in, "RCPT TO:<" + to + ">", "250");
            command(out, in, "DATA", "354");

            String date = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH));
            String message = "From: ShareBite <" + user + ">\r\n"
                    + "To: <" + to + ">\r\n"
                    + "Subject: " + subject + "\r\n"
                    + "Date: " + date + "\r\n"
                    + "MIME-Version: 1.0\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\n"
                    + "\r\n"
                    + body.replace("\r\n", "\n").replace("\n", "\r\n").replace("\r\n.", "\r\n..") + "\r\n"
                    + ".";                                        // a line with only "." ends the message
            command(out, in, message, "250");
            command(out, in, "QUIT", "221");
        } catch (IOException e) {
            // Details go to the server console only (never the password).
            System.err.println("Could not send email: " + e.getMessage());
            throw new ApiException(502, "Could not send the OTP email right now. Please try again in a minute.");
        }
    }

    /**
     * Opens an encrypted connection to the mail server and reads its greeting.
     *  - Port 465: encrypted from the very first byte (SSL).
     *  - Port 587: starts as plain text, then the STARTTLS command switches it to encrypted.
     *    (Useful where antivirus software intercepts port 465.)
     */
    private static Socket connect() throws IOException {
        SSLSocketFactory ssl = (SSLSocketFactory) SSLSocketFactory.getDefault();
        if (port == 465) {
            SSLSocket socket = (SSLSocket) ssl.createSocket(host, port);
            socket.setSoTimeout(15000); // give up if the mail server is silent for 15 seconds
            expect(reader(socket), "220");
            return socket;
        }
        Socket plain = new Socket(host, port);
        plain.setSoTimeout(15000);
        BufferedReader in = reader(plain);
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(plain.getOutputStream(), StandardCharsets.UTF_8));
        expect(in, "220");                                       // server greeting
        command(out, in, "EHLO localhost", "250");
        command(out, in, "STARTTLS", "220");                     // switch to encryption
        SSLSocket socket = (SSLSocket) ssl.createSocket(plain, host, port, true);
        socket.setSoTimeout(15000);
        socket.startHandshake();                                 // checks the server's certificate
        return socket;
    }

    // Reads the socket byte by byte, so nothing is buffered past a reply (needed before STARTTLS).
    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8), 1);
    }

    private static void command(BufferedWriter out, BufferedReader in, String line, String expectedCode)
            throws IOException {
        out.write(line + "\r\n");
        out.flush();
        expect(in, expectedCode);
    }

    /** Reads the server's answer (it may be several lines) and checks the number it starts with. */
    private static void expect(BufferedReader in, String expectedCode) throws IOException {
        String line;
        do {
            line = in.readLine();
            if (line == null) throw new IOException("mail server closed the connection");
        } while (line.length() > 3 && line.charAt(3) == '-');   // "250-..." means more lines follow
        if (!line.startsWith(expectedCode)) {
            throw new IOException("mail server answered: " + line);
        }
    }

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}
