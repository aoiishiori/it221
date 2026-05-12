package borrowing.DAO;

import borrowing.Interface.ActivityRequestInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ActivityRequest DAO
 *
 * Fixes applied:
 *   1. getAllActivities() now JOINs person to expose requester name
 *   2. getActivitySummary() uses a plain Statement (satisfies the JDBC Statement
 *      requirement from Task 4a — all three interfaces must be demonstrated)
 *   3. addActivity() and updateActivityStatus() use PreparedStatement (unchanged logic)
 */
public class ActivityRequest implements ActivityRequestInter {

    // ------------------------------------------------------------------
    // READ — all activities (PreparedStatement, no filter param needed
    //        but kept prepared for consistency and SQL-injection safety)
    // ------------------------------------------------------------------
    @Override
    public List<DataClass.ActivityRecord> getAllActivities() {

        // FIX: JOIN person on requested_by to get the requester's name
        String sql =
                "SELECT ar.request_id, ar.activity_name, ar.activity_type, " +
                        "       ar.activity_date, ar.status, ar.requested_by, " +
                        "       CONCAT(p.last_name, ', ', p.first_name) AS requester_name " +
                        "FROM activity_request ar " +
                        "JOIN person p ON ar.requested_by = p.person_id " +
                        "ORDER BY ar.activity_date";

        List<DataClass.ActivityRecord> list = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new DataClass.ActivityRecord(
                        rs.getInt("request_id"),
                        rs.getString("activity_name"),
                        rs.getString("activity_type"),
                        rs.getString("activity_date"),
                        rs.getString("status"),
                        rs.getString("requested_by"),
                        rs.getString("requester_name")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllActivities: " + e.getMessage());
        }

        return list;
    }

    // ------------------------------------------------------------------
    // READ — plain Statement demo (satisfies Task 4a Statement requirement)
    // Returns a simple count string used in the dashboard header label.
    // Because the query is fully internal (no user input), a plain
    // Statement is appropriate and safe here.
    // ------------------------------------------------------------------
    @Override
    public String getActivitySummary() {
        String sql = "SELECT " +
                "SUM(status = 'PENDING')  AS pending, " +
                "SUM(status = 'APPROVED') AS approved, " +
                "SUM(status = 'REJECTED') AS rejected " +
                "FROM activity_request";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();          // <-- plain Statement
             ResultSet rs   = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return String.format("Pending: %d | Approved: %d | Rejected: %d",
                        rs.getInt("pending"),
                        rs.getInt("approved"),
                        rs.getInt("rejected"));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getActivitySummary: " + e.getMessage());
        }
        return "Summary unavailable";
    }

    // ------------------------------------------------------------------
    // CREATE — insert new activity request (PreparedStatement)
    // ------------------------------------------------------------------
    @Override
    public int addActivity(String activityName, String activityType, String activityDate,
                           String location, String requestedBy, String notes) {

        // Validation: activity name must not exceed VARCHAR(100)
        if (activityName == null || activityName.trim().isEmpty()) return -1;
        if (activityName.length() > 100) {
            System.err.println("[VALIDATION] Activity name exceeds 100 characters.");
            return -1;
        }
        if (location != null && location.length() > 100) {
            System.err.println("[VALIDATION] Location exceeds 100 characters.");
            return -1;
        }
        if (notes != null && notes.length() > 255) {
            System.err.println("[VALIDATION] Notes exceed 255 characters.");
            return -1;
        }

        String sql = "INSERT INTO activity_request " +
                "(activity_name, activity_type, activity_date, location, requested_by, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, activityName.trim());
            ps.setString(2, activityType);
            ps.setString(3, activityDate);
            ps.setString(4, location);
            ps.setString(5, requestedBy);
            ps.setString(6, notes);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] addActivity: " + e.getMessage());
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // UPDATE — approve or reject (PreparedStatement)
    // ------------------------------------------------------------------
    @Override
    public boolean updateActivityStatus(int requestId, String newStatus, String approvedBy) {

        // Guard: only valid transitions accepted
        if (!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED")) {
            System.err.println("[VALIDATION] Invalid status: " + newStatus);
            return false;
        }

        String sql = "UPDATE activity_request SET status = ?, approved_by = ? " +
                "WHERE request_id = ? AND status = 'PENDING'";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newStatus);
            ps.setString(2, approvedBy);
            ps.setInt   (3, requestId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DB ERROR] updateActivityStatus: " + e.getMessage());
        }
        return false;
    }
}