package borrowing.DAO;

import borrowing.Interface.LabClassInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabClass implements LabClassInter {

    @Override
    public List<DataClass.LabClassRecord> getAllClasses() {
        String sql = "SELECT lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, CONCAT(p.last_name, ', ', p.first_name) AS faculty_name, COUNT(ce.student_id) AS enrolled_count FROM LAB_CLASS lc JOIN PERSON p ON lc.faculty_id = p.person_id LEFT JOIN CLASS_ENROLLMENT ce ON lc.class_id = ce.class_id GROUP BY lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, faculty_name ORDER BY lc.school_year DESC, lc.semester, lc.course_code, lc.section";
        List<DataClass.LabClassRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public String getClassInfo(String classId) {
        String sql = "SELECT CONCAT(lc.course_code, ' ', lc.section, ' | ', lc.semester, ' SY ', lc.school_year, ' | ', IFNULL(lc.room, 'TBD'), ' | ', IFNULL(lc.schedule, 'TBD'), ' | Faculty: ', p.last_name, ', ', p.first_name) AS info FROM LAB_CLASS lc JOIN PERSON p ON lc.faculty_id = p.person_id WHERE lc.class_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, classId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("info");
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<DataClass.LabClassRecord> searchClasses(String keyword, String searchType) {
        String sql;
        if ("1".equals(searchType)) {
            sql = "SELECT lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, CONCAT(p.last_name, ', ', p.first_name) AS faculty_name, COUNT(ce.student_id) AS enrolled_count FROM LAB_CLASS lc JOIN PERSON p ON lc.faculty_id = p.person_id LEFT JOIN CLASS_ENROLLMENT ce ON lc.class_id = ce.class_id WHERE lc.course_code LIKE ? GROUP BY lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, faculty_name ORDER BY lc.school_year DESC, lc.course_code, lc.section";
        } else {
            sql = "SELECT lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, CONCAT(p.last_name, ', ', p.first_name) AS faculty_name, COUNT(ce.student_id) AS enrolled_count FROM LAB_CLASS lc JOIN PERSON p ON lc.faculty_id = p.person_id LEFT JOIN CLASS_ENROLLMENT ce ON lc.class_id = ce.class_id WHERE lc.school_year = ? GROUP BY lc.class_id, lc.course_code, lc.section, lc.semester, lc.school_year, lc.room, lc.schedule, faculty_name ORDER BY lc.course_code, lc.section";
        }

        List<DataClass.LabClassRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if ("1".equals(searchType)) ps.setString(1, "%" + keyword + "%");
            else ps.setString(1, keyword);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    private DataClass.LabClassRecord mapRow(ResultSet rs) throws SQLException {
        return new DataClass.LabClassRecord(
                rs.getString("class_id"), rs.getString("course_code"), rs.getString("section"),
                rs.getString("semester"), rs.getString("school_year"), rs.getString("room"),
                rs.getString("schedule"), rs.getString("faculty_name"), rs.getInt("enrolled_count")
        );
    }
}