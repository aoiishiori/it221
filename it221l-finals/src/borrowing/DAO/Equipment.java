package borrowing.DAO;

import borrowing.Interface.EquipmentInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Equipment DAO — v2 additions:
 *   NEW: addEquipment() — Custodian/Admin can register new equipment.
 *   NEW: getAllPersons() — for borrower ID picker dropdown.
 */
public class Equipment implements EquipmentInter {

    @Override
    public List<DataClass.Equipment> getAllEquipment() {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment ORDER BY category, item_name, barcode";
        return fetchEquipment(sql);
    }

    @Override
    public List<DataClass.Equipment> getAvailableEquipment() {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment WHERE status = 'AVAILABLE' " +
                "ORDER BY category, item_name, barcode";
        return fetchEquipment(sql);
    }

    @Override
    public List<DataClass.Equipment> getEquipmentByStatus(String status) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment WHERE status = ? ORDER BY category, item_name, barcode";
        return fetchEquipmentWithParam(sql, status);
    }

    @Override
    public List<DataClass.Equipment> getEquipmentByCategory(String category) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment WHERE category = ? ORDER BY item_name, barcode";
        return fetchEquipmentWithParam(sql, category);
    }

    @Override
    public List<DataClass.Equipment> searchEquipment(String keyword) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment WHERE item_name LIKE ? OR barcode LIKE ? " +
                "ORDER BY category, item_name";
        List<DataClass.Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyword);
            ps.setString(2, keyword);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] searchEquipment: " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<DataClass.Equipment> getEquipmentByExactBarcode(String barcode) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment WHERE barcode = ?";
        return fetchEquipmentWithParam(sql, barcode.trim());
    }

    @Override
    public boolean updateEquipmentStatus(String barcode, String newStatus) {
        String sql = "UPDATE equipment SET status = ? WHERE barcode = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, barcode);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] updateEquipmentStatus: " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------------
    // NEW: Add a brand-new equipment record
    // Returns the generated barcode string, or null on failure.
    // Barcode is auto-generated as EQ-XXXX (next available number).
    // ----------------------------------------------------------------
    public String addEquipment(String itemName, String category, String brand,
                               String model, String acquisitionDate, String remarks) {
        // Validation
        if (itemName == null || itemName.isBlank() || itemName.length() > 100) return null;
        if (brand    != null && brand.length()    > 50)  return null;
        if (model    != null && model.length()    > 50)  return null;
        if (remarks  != null && remarks.length()  > 255) return null;

        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // Generate next barcode: find the highest existing EQ-NNNN number
            String maxSql = "SELECT MAX(CAST(SUBSTRING(barcode,4) AS UNSIGNED)) AS maxNum " +
                    "FROM equipment WHERE barcode REGEXP '^EQ-[0-9]+$'";
            int nextNum = 1;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(maxSql)) {
                if (rs.next()) nextNum = rs.getInt("maxNum") + 1;
            }
            String newBarcode = String.format("EQ-%04d", nextNum);

            String insertSql =
                    "INSERT INTO equipment " +
                            "(barcode, item_name, category, brand, model, status, acquisition_date, remarks) " +
                            "VALUES (?, ?, ?, ?, ?, 'AVAILABLE', ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, newBarcode);
                ps.setString(2, itemName.trim());
                ps.setString(3, category);
                ps.setString(4, brand  != null && !brand.isBlank()  ? brand.trim()  : null);
                ps.setString(5, model  != null && !model.isBlank()  ? model.trim()  : null);
                ps.setString(6, acquisitionDate != null && !acquisitionDate.isBlank()
                        ? acquisitionDate : null);
                ps.setString(7, remarks != null && !remarks.isBlank() ? remarks.trim() : null);
                ps.executeUpdate();
            }

            conn.commit();
            return newBarcode;

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[DB ERROR] addEquipment: " + e.getMessage());
            return null;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------
    private List<DataClass.Equipment> fetchEquipment(String sql) {
        List<DataClass.Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] fetchEquipment: " + e.getMessage());
        }
        return list;
    }

    private List<DataClass.Equipment> fetchEquipmentWithParam(String sql, String param) {
        List<DataClass.Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] fetchEquipmentWithParam: " + e.getMessage());
        }
        return list;
    }

    private DataClass.Equipment mapRow(ResultSet rs) throws SQLException {
        return new DataClass.Equipment(
                rs.getString("barcode"), rs.getString("item_name"),
                rs.getString("category"), rs.getString("brand"),
                rs.getString("model"), rs.getString("status"),
                rs.getString("remarks")
        );
    }
}