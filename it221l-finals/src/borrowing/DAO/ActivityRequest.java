package borrowing.DAO;

import borrowing.Interface.ActivityRequestInter;
import borrowing.model.DataClass;
import borrowing.util.Database;
import java.sql.PreparedStatement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityRequest implements ActivityRequestInter {

    @Override
    public List<DataClass.ActivityRecord> getAllActivities() {

        String sql = "SELECT request_id, activity_name, activity_type, activity_date, status FROM ACTIVITY_REQUEST ORDER BY activity_date";

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
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }

        return list;
    }

    @Override
    public int addActivity(String activityName, String activityType, String activityDate, String location, String requestedBy, String notes) {
        String sql = "INSERT INTO activity_request (activity_name, activity_type, activity_date, location, requested_by, notes) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, activityName);
            ps.setString(2, activityType);
            ps.setString(3, activityDate);
            ps.setString(4, location);
            ps.setString(5, requestedBy);
            ps.setString(6, notes);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1); // Return the new request_id
                }
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean updateActivityStatus(int requestId, String newStatus, String approvedBy) {
        String sql = "UPDATE activity_request SET status = ?, approved_by = ? WHERE request_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, approvedBy);
            ps.setInt(3, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return false;
    }
}