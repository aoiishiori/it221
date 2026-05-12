package borrowing.DAO;

import borrowing.Interface.PersonInter;
import borrowing.model.DataClass;
import borrowing.util.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Person implements PersonInter {

    /**
     * FIX #1: getCredentials now returns 3 values: [password_hash, role, person_id]
     * Previously it returned only [password_hash, role], causing LoginView to crash
     * when it tried to access creds[2] (the person_id) — resulting in an ArrayIndexOutOfBoundsException.
     */
    @Override
    public String[] getCredentials(String username) {
        String sql = "SELECT ua.password_hash, ua.role, ua.person_id " +
                "FROM user_account ua " +
                "WHERE ua.username = ? AND ua.is_active = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Returns [0] password_hash, [1] role, [2] person_id
                return new String[]{
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("person_id")
                };
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getPersonType(String personId) {
        String sql = "SELECT person_type FROM PERSON WHERE person_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("person_type");
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getPersonName(String personId) {
        String sql = "SELECT CONCAT(last_name, ', ', first_name) AS full_name FROM PERSON WHERE person_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<DataClass.StudentRecord> getAllStudents() {
        String sql = "SELECT p.person_id, p.last_name, p.first_name, p.middle_name, " +
                "p.email, p.contact_no, COUNT(DISTINCT ce.class_id) AS class_count " +
                "FROM PERSON p " +
                "LEFT JOIN CLASS_ENROLLMENT ce ON p.person_id = ce.student_id " +
                "WHERE p.person_type = 'STUDENT' " +
                "GROUP BY p.person_id, p.last_name, p.first_name, p.middle_name, p.email, p.contact_no " +
                "ORDER BY p.last_name, p.first_name";
        List<DataClass.StudentRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DataClass.StudentRecord(
                        rs.getString("person_id"), rs.getString("last_name"),
                        rs.getString("first_name"), rs.getString("middle_name"),
                        rs.getString("email"), rs.getString("contact_no"),
                        rs.getInt("class_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    // Stubs — not yet implemented for this sprint
    @Override public List<DataClass.PersonSummary> getPersonBorrowSummary() { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord> searchStudents(String keyword) { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord> getStudentsByClassId(String classId) { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord> getAllStaffFaculty() { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord> getStaffFacultyByType(String type) { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord> searchStaffFaculty(String keyword) { return new ArrayList<>(); }
}