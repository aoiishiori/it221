package borrowing.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database — manages JDBC connections to MySQL.
 *
 */

public class Database {

    private static final String DB_URL  =
            "jdbc:mysql://localhost:3306/borrowing_db" +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // set your MySQL password here if needed

    private static boolean driverLoaded = false;

    /**
     * Opens and returns a brand-new Connection.
     * The caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                System.out.println("[DB] Connected to borrowing_db successfully.");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "MySQL JDBC Driver not found. Ensure mysql-connector-java is in /lib.", e);
            }
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}

// commit message: fixed duplicate connection success message by adding a flag to track the first connection.
// in simple terms, Result: Prints only once when app starts