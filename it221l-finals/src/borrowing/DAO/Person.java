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
    // READ — all persons (for borrower picker dropdown)
    // Returns "personId|LASTNAME, Firstname (type)" strings
    // ----------------------------------------------------------------
    public List<String> getAllPersonsForPicker() {
        String sql = "SELECT person_id, last_name, first_name, person_type " +
                "FROM person ORDER BY person_type, last_name, first_name";
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
                "p.email, p.contact_no, COUNT(DISTINCT ce.class_id) AS class_count " +
                "FROM person p " +
                "LEFT JOIN class_enrollment ce ON p.person_id = ce.student_id " +
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
            System.err.println("[DB ERROR] getAllStudents: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // NEW: Admin creates a user account
    // person_type: STUDENT, FACULTY, STAFF
    // role:        BORROWER, CUSTODIAN, ADMIN
    // Returns the new account_id, or -1 on failure.
    // ----------------------------------------------------------------
    public int createAccount(String personId, String username, String passwordHash,
                             String role) {
        // Check person exists
        if (getPersonName(personId) == null) {
            System.err.println("[VALIDATION] createAccount: person_id not found: " + personId);
            return -1;
        }
        // Check username unique
        String checkSql = "SELECT account_id FROM user_account WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, username);
            if (ps.executeQuery().next()) {
                System.err.println("[VALIDATION] createAccount: username already exists: " + username);
                return -2; // special code: duplicate username
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] createAccount check: " + e.getMessage());
            return -1;
        }

        String sql = "INSERT INTO user_account (person_id, username, password_hash, role) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, personId);
            ps.setString(2, username.trim());
            ps.setString(3, passwordHash);
            ps.setString(4, role);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] createAccount insert: " + e.getMessage());
        }
        return -1;
    }

    // ----------------------------------------------------------------
    // NEW: Get all user accounts (Admin account management view)
    // ----------------------------------------------------------------
    public List<String[]> getAllAccounts() {
        // Returns rows: [account_id, person_id, username, role, is_active, full_name]
        String sql = "SELECT ua.account_id, ua.person_id, ua.username, ua.role, " +
                "ua.is_active, " +
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
                        rs.getString("full_name"),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllAccounts: " + e.getMessage());
        }
        return list;
    }

    // NEW: toggle account active/inactive
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

    // Stubs
    @Override public List<DataClass.PersonSummary>      getPersonBorrowSummary()              { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord>       searchStudents(String keyword)        { return new ArrayList<>(); }
    @Override public List<DataClass.StudentRecord>       getStudentsByClassId(String classId)  { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  getAllStaffFaculty()                  { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  getStaffFacultyByType(String type)    { return new ArrayList<>(); }
    @Override public List<DataClass.StaffFacultyRecord>  searchStaffFaculty(String keyword)    { return new ArrayList<>(); }
}