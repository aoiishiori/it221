package borrowing.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database - Manages the JDBC connection to MySQL.
 * Updated to only show the success message on the first connection.
 */
public class Database {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/borrowing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila";
    private static final String DB_USER = "root";
    private static final String DB_PASS = ""; // change to your MySQL password if present

    private static Connection connection = null;
    private static boolean firstConnection = true; // Fix: Flag to track initial connection

    /** Returns a singleton Connection instance. */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

                // Fix: Only print success message once
                if (firstConnection) {
                    System.out.println("[DB] Connected to borrowing_db successfully.");
                    firstConnection = false;
                }

            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. Ensure mysql-connector-java is in /lib.", e);
            }
        }
        return connection;
    }

    /** Closes the connection if open. */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null; // Ensure connection is set to null after closing
                System.out.println("[DB] Connection closed.");
            } catch (SQLException e) {
                System.err.println("[DB] Error closing connection: " + e.getMessage());
            }
        }
    }
}

// commit message: fixed duplicate connection success message by adding a flag to track the first connection.
// in simple terms, Result: Prints only once when app starts