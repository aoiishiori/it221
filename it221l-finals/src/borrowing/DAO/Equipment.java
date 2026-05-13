package borrowing.DAO;

import borrowing.Interface.EquipmentInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Equipment DAO — fixes applied:
 *   FIX 1: getAvailableEquipment() added — used by the borrow form picker.
 *   FIX 2: getEquipmentByExactBarcode() added — exact-match search so "EQ-0001"
 *           does not also return EQ-00010, EQ-00011, etc.
 */
public class Equipment implements EquipmentInter {

    @Override
    public List<DataClass.Equipment> getAllEquipment() {
        String sql = "SELECT barcode, item_name, category, brand, model, status, " +
                "IFNULL(remarks, '-') AS remarks " +
                "FROM equipment ORDER BY category, item_name, barcode";
        return fetchEquipment(sql);
    }

    // FIX 1: convenience method for the borrow-form equipment picker
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

    // Keyword LIKE search (contains)
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

    // FIX 2: exact barcode match — "EQ-0001" returns only EQ-0001
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

    // Helpers
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