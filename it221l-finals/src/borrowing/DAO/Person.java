package borrowing.DAO;

import borrowing.Interface.PersonInter;
import borrowing.model.DataClass;
import borrowing.util.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Person implements PersonInter {

    // ----------------------------------------------------------------
    // AUTH
    // ----------------------------------------------------------------
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
                return new String[]{
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("person_id")
                };
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getCredentials: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getPersonType(String personId) {
        String sql = "SELECT person_type FROM person WHERE person_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("person_type");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getPersonType: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getPersonName(String personId) {
        String sql = "SELECT CONCAT(last_name,', ',first_name) AS full_name FROM person WHERE person_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getPersonName: " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // READ — all ACTIVE persons (for borrower picker dropdown)
    // ----------------------------------------------------------------
    public List<String> getAllPersonsForPicker() {
        String sql = "SELECT person_id, last_name, first_name, person_type " +
                "FROM person WHERE status = 'ACTIVE' ORDER BY person_type, last_name, first_name";
        List<String> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("person_id") + "|" +
                        rs.getString("last_name") + ", " +
                        rs.getString("first_name") + " (" +
                        rs.getString("person_type") + ")");
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllPersonsForPicker: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<DataClass.StudentRecord> getAllStudents() {
        String sql = "SELECT p.person_id, p.last_name, p.first_name, p.middle_name, " +
                "p.email, p.contact_no, p.status, COUNT(DISTINCT ce.class_id) AS class_count " +
                "FROM person p " +
                "LEFT JOIN class_enrollment ce ON p.person_id = ce.student_id " +
                "WHERE p.person_type = 'STUDENT' " +
                "GROUP BY p.person_id, p.last_name, p.first_name, p.middle_name, p.email, p.contact_no, p.status " +
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
                        rs.getString("status"),
                        rs.getInt("class_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllStudents: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // CREATE — Unified Person + Account via Stored Procedure
    // ----------------------------------------------------------------
    public boolean registerNewUser(String personId, String lastName, String firstName,
                                   String middleName, String personType, String email,
                                   String contactNo, String username, String passwordHash,
                                   String role) {
        // Pre-validation: Check username unique
        String checkSql = "SELECT account_id FROM user_account WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            if (ps.executeQuery().next()) {
                System.err.println("[VALIDATION] registerNewUser: username already exists: " + username);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] registerNewUser check: " + e.getMessage());
            return false;
        }

        // Pre-validation: Check person_id unique
        String checkPersonSql = "SELECT person_id FROM person WHERE person_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkPersonSql)) {
            ps.setString(1, personId);
            if (ps.executeQuery().next()) {
                System.err.println("[VALIDATION] registerNewUser: person_id already exists: " + personId);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] registerNewUser person check: " + e.getMessage());
            return false;
        }

        String sql = "{CALL sp_register_new_user(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, personId.trim());
            cs.setString(2, lastName.trim());
            cs.setString(3, firstName.trim());
            cs.setString(4, middleName != null && !middleName.isBlank() ? middleName.trim() : null);
            cs.setString(5, personType);
            cs.setString(6, email != null && !email.isBlank() ? email.trim() : null);
            cs.setString(7, contactNo != null && !contactNo.isBlank() ? contactNo.trim() : null);
            cs.setString(8, username.trim());
            cs.setString(9, passwordHash);
            cs.setString(10, role);
            cs.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] registerNewUser: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------
    // ARCHIVE (Soft Delete) — via Stored Procedure
    // ----------------------------------------------------------------
    public boolean archivePerson(String personId) {
        String sql = "{CALL sp_archive_person(?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setString(1, personId);
            cs.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] archivePerson: " + e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------------------------
    // TOGGLE Account Active/Inactive
    // ----------------------------------------------------------------
    public boolean setAccountActive(int accountId, boolean active) {
        String sql = "UPDATE user_account SET is_active = ? WHERE account_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, accountId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] setAccountActive: " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------------
    // GET all user accounts (Admin account management view)
    // ----------------------------------------------------------------
    public List<String[]> getAllAccounts() {
        String sql = "SELECT ua.account_id, ua.person_id, ua.username, ua.role, " +
                "ua.is_active, p.status AS person_status, " +
                "CONCAT(p.last_name,', ',p.first_name) AS full_name, " +
                "DATE_FORMAT(ua.created_at,'%Y-%m-%d') AS created_at " +
                "FROM user_account ua " +
                "JOIN person p ON ua.person_id = p.person_id " +
                "ORDER BY ua.role, p.last_name";
        List<String[]> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("account_id")),
                        rs.getString("person_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                        rs.getString("person_status"),
                        rs.getString("full_name"),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllAccounts: " + e.getMessage());
        }
        return list;
    }

    // Stubs
    @Override public List<DataClass.PersonSummary>      getPersonBorrowSummary()              { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord>       searchStudents(String keyword)        { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord>       getStudentsByClassId(String classId)  { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  getAllStaffFaculty()                  { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  getStaffFacultyByType(String type)    { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  searchStaffFaculty(String keyword)    { return new ArrayList<>(); }
}