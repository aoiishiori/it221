package borrowing.DAO;

import borrowing.Interface.PersonInter;
import borrowing.model.DataClass;
import borrowing.util.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Person implements PersonInter {
    @Override
    public String[] getCredentials(String username) {
        // Updated to target the user_account table from your screenshot
        String sql = "SELECT password_hash, role FROM user_account WHERE username = ? AND is_active = 1";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Returns [0] hash, [1] role (e.g., CUSTODIAN, ADMIN, BORROWER)
                return new String[]{rs.getString("password_hash"), rs.getString("role")};
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
        String sql = "SELECT p.person_id, p.last_name, p.first_name, p.middle_name, p.email, p.contact_no, COUNT(DISTINCT ce.class_id) AS class_count FROM PERSON p LEFT JOIN CLASS_ENROLLMENT ce ON p.person_id = ce.student_id WHERE p.person_type = 'STUDENT' GROUP BY p.person_id, p.last_name, p.first_name, p.middle_name, p.email, p.contact_no ORDER BY p.last_name, p.first_name";
        List<DataClass.StudentRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new DataClass.StudentRecord(
                        rs.getString("person_id"), rs.getString("last_name"), rs.getString("first_name"),
                        rs.getString("middle_name"), rs.getString("email"), rs.getString("contact_no"), rs.getInt("class_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override public List<DataClass.PersonSummary> getPersonBorrowSummary() { return null; }
    @Override public List<DataClass.StudentRecord> searchStudents(String keyword) { return null; }
    @Override public List<DataClass.StudentRecord> getStudentsByClassId(String classId) { return null; }
    @Override public List<DataClass.StaffFacultyRecord> getAllStaffFaculty() { return null; }
    @Override public List<DataClass.StaffFacultyRecord> getStaffFacultyByType(String type) { return null; }
    @Override public List<DataClass.StaffFacultyRecord> searchStaffFaculty(String keyword) { return null; }
}