package borrowing.DAO;

import borrowing.Interface.EquipmentInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Equipment implements EquipmentInter {

    @Override
    public List<DataClass.Equipment> getAllEquipment() {
        String sql = "SELECT barcode, item_name, category, brand, model, status, IFNULL(remarks, '-') AS remarks FROM EQUIPMENT ORDER BY category, item_name, barcode";
        return fetchEquipment(sql);
    }

    @Override
    public List<DataClass.Equipment> getEquipmentByStatus(String status) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, IFNULL(remarks, '-') AS remarks FROM EQUIPMENT WHERE status = ? ORDER BY category, item_name, barcode";
        return fetchEquipmentWithParam(sql, status);
    }

    @Override
    public List<DataClass.Equipment> getEquipmentByCategory(String category) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, IFNULL(remarks, '-') AS remarks FROM EQUIPMENT WHERE category = ? ORDER BY item_name, barcode";
        return fetchEquipmentWithParam(sql, category);
    }

    @Override
    public List<DataClass.Equipment> searchEquipment(String keyword) {
        String sql = "SELECT barcode, item_name, category, brand, model, status, IFNULL(remarks, '-') AS remarks FROM EQUIPMENT WHERE item_name LIKE ? OR barcode LIKE ? ORDER BY category, item_name";
        List<DataClass.Equipment> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyword);
            ps.setString(2, keyword);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
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
            System.err.println(" [DB ERROR] " + e.getMessage());
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
            System.err.println(" [DB ERROR] " + e.getMessage());
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
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    private DataClass.Equipment mapRow(ResultSet rs) throws SQLException {
        return new DataClass.Equipment(
                rs.getString("barcode"), rs.getString("item_name"), rs.getString("category"),
                rs.getString("brand"), rs.getString("model"), rs.getString("status"), rs.getString("remarks")
        );
    }
}