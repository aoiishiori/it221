package borrowing.DAO;

import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLog DAO — writes and reads the audit_log table.
 *
 * All writes use PreparedStatement (SQL-injection safe, even though
 * the data is internal, it is still best practice).
 * The read (getRecentLogs) uses a plain Statement per the
 * "demonstrate all three JDBC statement types" requirement.
 */
public class AuditLog {

    // ----------------------------------------------------------------
    // WRITE — insert one log entry
    // ----------------------------------------------------------------
    public static void log(String personId, String username,
                           String action, String entity,
                           String entityId, String details) {
        String sql = "INSERT INTO audit_log " +
                "(person_id, username, action, entity, entity_id, details) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId  != null ? personId  : "SYSTEM");
            ps.setString(2, username  != null ? username  : "system");
            ps.setString(3, action);
            ps.setString(4, entity);
            ps.setString(5, entityId);
            // Truncate details to 500 chars to respect the column width
            ps.setString(6, details != null && details.length() > 500
                    ? details.substring(0, 497) + "..." : details);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Logging failures must never crash the application
            System.err.println("[AUDIT] Failed to write log: " + e.getMessage());
        }
    }

    /** Convenience — no entity context needed (e.g. LOGIN, LOGOUT) */
    public static void log(String personId, String username,
                           String action, String details) {
        log(personId, username, action, null, null, details);
    }

    // ----------------------------------------------------------------
    // READ — all logs, newest first (plain Statement — no user input,
    // satisfies the "demonstrate Statement" requirement in the Admin view)
    // ----------------------------------------------------------------
    public List<DataClass.AuditEntry> getRecentLogs(int limit) {
        // Plain Statement is safe here because `limit` is an int from the UI
        // (no string concatenation risk) and the query contains no user-provided text.
        String sql = "SELECT log_id, person_id, username, action, " +
                "IFNULL(entity,'—') AS entity, " +
                "IFNULL(entity_id,'—') AS entity_id, " +
                "IFNULL(details,'') AS details, " +
                "DATE_FORMAT(logged_at,'%Y-%m-%d %H:%i:%s') AS logged_at " +
                "FROM audit_log ORDER BY logged_at DESC LIMIT " + limit;

        List<DataClass.AuditEntry> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new DataClass.AuditEntry(
                        rs.getInt("log_id"),
                        rs.getString("person_id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("entity"),
                        rs.getString("entity_id"),
                        rs.getString("details"),
                        rs.getString("logged_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getRecentLogs: " + e.getMessage());
        }
        return list;
    }

    // READ — filtered by action keyword
    public List<DataClass.AuditEntry> searchLogs(String keyword) {
        String sql = "SELECT log_id, person_id, username, action, " +
                "IFNULL(entity,'—') AS entity, " +
                "IFNULL(entity_id,'—') AS entity_id, " +
                "IFNULL(details,'') AS details, " +
                "DATE_FORMAT(logged_at,'%Y-%m-%d %H:%i:%s') AS logged_at " +
                "FROM audit_log " +
                "WHERE action LIKE ? OR username LIKE ? OR details LIKE ? " +
                "ORDER BY logged_at DESC LIMIT 500";

        List<DataClass.AuditEntry> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new DataClass.AuditEntry(
                        rs.getInt("log_id"),
                        rs.getString("person_id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("entity"),
                        rs.getString("entity_id"),
                        rs.getString("details"),
                        rs.getString("logged_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] searchLogs: " + e.getMessage());
        }
        return list;
    }
}