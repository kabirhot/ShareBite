package sharebite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The data store, connected to MySQL through JDBC.
 *
 * In the console version this class held one hard-coded user. Now every
 * method runs an SQL query on the "sharebite" database.
 *
 * JDBC in four steps (see insert() for a full example):
 *   1. Connection         - DriverManager.getConnection(url, user, password)
 *   2. PreparedStatement  - the SQL text with ? in place of every value
 *   3. setString(...)     - fill in the ? values, then executeUpdate() / executeQuery()
 *   4. ResultSet          - read the rows that come back
 *
 * Values are ALWAYS passed with ? and setString(), never joined into the SQL
 * text. That is what prevents SQL injection.
 */
public class Database {

    public static final String USERS = "users";
    public static final String DONATIONS = "donations";
    public static final String REQUESTS = "requests";
    public static final String VOLUNTEERS = "volunteers";
    public static final String MESSAGES = "messages";

    /**
     * Describes one MySQL table: its real name, its id column, and which column
     * each form field is saved in.   form field name  ->  column name
     * Table and column names in the SQL below only ever come from these lists,
     * never from anything a user typed.
     */
    private static class Table {
        final String name;
        final String idColumn;
        final Map<String, String> columns = new LinkedHashMap<>();

        Table(String name, String idColumn, String... fieldThenColumn) {
            this.name = name;
            this.idColumn = idColumn;
            for (int i = 0; i < fieldThenColumn.length; i += 2) {
                columns.put(fieldThenColumn[i], fieldThenColumn[i + 1]);
            }
        }

        /** The column for a form field ("id" means the id column). */
        String column(String field) {
            if (field.equals("id")) return idColumn;
            String column = columns.get(field);
            if (column == null) throw new IllegalArgumentException("Unknown field: " + field);
            return column;
        }

        /** "user_id AS id, full_name AS name, ..." so results use the form field names. */
        String selectList() {
            StringBuilder sb = new StringBuilder(idColumn + " AS id");
            columns.forEach((field, column) -> sb.append(", ").append(column).append(" AS ").append(field));
            return sb.toString();
        }
    }

    private static final Map<String, Table> TABLES = new LinkedHashMap<>();
    static {
        TABLES.put(USERS, new Table("users", "user_id",
                "name", "full_name",
                "email", "email",
                "phone", "phone",
                "service", "service",
                "passwordHash", "password"));
        TABLES.put(DONATIONS, new Table("food_donor", "donor_id",
                "userId", "user_id",
                "donorName", "donor_name",
                "contact", "contact",
                "foodName", "food_name",
                "plates", "food_plates",
                "foodType", "food_type",
                "pickupAddress", "pickup_address",
                "confirmAddress", "confirm_address",
                "pickupDate", "pickup_date",
                "pickupTime", "pickup_time",
                "additionalInfo", "additional_info"));
        TABLES.put(REQUESTS, new Table("food_receiver", "receiver_id",
                "userId", "user_id",
                "receiverName", "receiver_name",
                "organization", "organization",
                "email", "email",
                "contact", "contact",
                "foodType", "food_type",
                "quantity", "quantity",
                "amount", "amount",
                "deliveryLocation", "delivery_location",
                "requiredDate", "required_date",
                "deliveryTime", "delivery_time",
                "additionalRequirements", "additional_requirements"));
        TABLES.put(VOLUNTEERS, new Table("volunteers", "volunteer_id",
                "userId", "user_id",
                "volunteerName", "volunteer_name",
                "email", "email",
                "contact", "contact",
                "location", "location",
                "availability", "availability",
                "work", "volunteer_work",
                "vehicle", "vehicle",
                "type", "vehicle_type",
                "area", "service_area",
                "message", "additional_information"));
        TABLES.put(MESSAGES, new Table("contact_messages", "message_id",
                "name", "full_name",
                "email", "email",
                "phone", "phone",
                "subject", "subject",
                "message", "message"));
    }

    private static String url;
    private static String user;
    private static String password;

    // ------------------------------------------------------------ connecting

    /**
     * Called once when the server starts: reads the connection settings,
     * checks that MySQL can be reached and creates the tables if they are missing.
     */
    public static void connect() throws IOException, SQLException {
        Properties settings = new Properties();
        Path file = Paths.get("db.properties");
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                settings.load(in);
            }
        }
        // An environment variable, if set, wins over the file.
        url = setting(settings, "DB_URL", "db.url",
                "jdbc:mysql://localhost:3306/sharebite?createDatabaseIfNotExist=true");
        user = setting(settings, "DB_USER", "db.user", "root");
        password = setting(settings, "DB_PASSWORD", "db.password", "");

        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String sql : readSchema()) {
                statement.execute(sql);
            }
        }
    }

    /** Step 1 of JDBC: open a connection to the database. */
    private static Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static String setting(Properties file, String envName, String key, String defaultValue) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env;
        return file.getProperty(key, defaultValue).trim();
    }

    /** Reads the CREATE TABLE statements from database/schema.sql. */
    private static List<String> readSchema() throws IOException {
        String text = Files.readString(Paths.get("database", "schema.sql"), StandardCharsets.UTF_8);
        List<String> statements = new ArrayList<>();
        for (String part : text.replaceAll("(?m)^\\s*--.*$", "").split(";")) {
            String sql = part.trim();
            // The database itself is chosen by the connection URL, so skip those two lines.
            if (sql.toUpperCase().startsWith("CREATE TABLE")) statements.add(sql);
        }
        return statements;
    }

    // ------------------------------------------------------------ queries

    /**
     * INSERT: adds a record and returns it as saved (with its new id).
     * Example of the SQL that is built:
     *   INSERT INTO contact_messages (full_name, email, phone, subject, message) VALUES (?, ?, ?, ?, ?)
     */
    public static Map<String, String> insert(String tableKey, Map<String, String> record) {
        Table table = table(tableKey);
        List<String> fields = new ArrayList<>(table.columns.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(fields.size(), "?"));
        String sql = "INSERT INTO " + table.name + " (" + String.join(", ", table.columns.values())
                + ") VALUES (" + placeholders + ")";

        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < fields.size(); i++) {
                statement.setString(i + 1, record.get(fields.get(i)));    // fill in the i-th ?
            }
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {          // the id MySQL gave the new row
                keys.next();
                return findOne(tableKey, "id", keys.getString(1));
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // For example: two people signing up with the same email at the same moment.
            throw new ApiException(409, "This record already exists.");
        } catch (SQLException e) {
            throw failed(e);
        }
    }

    /** SELECT ... WHERE column = ? LIMIT 1  -  the first matching record, or null. */
    public static Map<String, String> findOne(String tableKey, String field, String value) {
        Table table = table(tableKey);
        List<Map<String, String>> rows = select("SELECT " + table.selectList() + " FROM " + table.name
                + " WHERE " + table.column(field) + " = ? LIMIT 1", value);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** SELECT ... WHERE column = ? ORDER BY id DESC  -  all matching records, newest first. */
    public static List<Map<String, String>> findAll(String tableKey, String field, String value) {
        Table table = table(tableKey);
        return select("SELECT " + table.selectList() + " FROM " + table.name
                + " WHERE " + table.column(field) + " = ? ORDER BY " + table.idColumn + " DESC", value);
    }

    /** SELECT ... ORDER BY id DESC LIMIT n  -  the newest records of a table. */
    public static List<Map<String, String>> latest(String tableKey, int limit) {
        Table table = table(tableKey);
        return select("SELECT " + table.selectList() + " FROM " + table.name
                + " ORDER BY " + table.idColumn + " DESC LIMIT " + limit, null);
    }

    /** UPDATE table SET column = ? WHERE id = ? */
    public static void update(String tableKey, String id, String field, String value) {
        Table table = table(tableKey);
        String sql = "UPDATE " + table.name + " SET " + table.column(field) + " = ? WHERE " + table.idColumn + " = ?";
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failed(e);
        }
    }

    /** Runs a SELECT and turns every row of the ResultSet into a Map of column name -> value. */
    private static List<Map<String, String>> select(String sql, String value) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (value != null) statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData meta = result.getMetaData();
                while (result.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String text = result.getString(i);
                        if (text == null) text = "";
                        // MySQL returns a TIME as 18:30:00, the pages show 18:30
                        if (meta.getColumnType(i) == java.sql.Types.TIME && text.length() >= 5) {
                            text = text.substring(0, 5);
                        }
                        row.put(meta.getColumnLabel(i), text);
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw failed(e);
        }
        return rows;
    }

    // ------------------------------------------------------------ helpers

    /** Only the tables listed at the top of this class may be used. */
    private static Table table(String key) {
        Table table = TABLES.get(key);
        if (table == null) throw new IllegalArgumentException("Unknown table: " + key);
        return table;
    }

    /** Database errors are printed in the server console; the user only sees a general message. */
    private static ApiException failed(SQLException e) {
        System.err.println("Database error: " + e.getMessage());
        return new ApiException(500, "Something went wrong on the server. Please try again.");
    }
}
