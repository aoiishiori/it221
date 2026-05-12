package borrowing.DAO;

import borrowing.Interface.BorrowInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Borrow implements BorrowInter {

    @Override
    public List<DataClass.BorrowRecord> getRecordsByBorrowerId(String personId) {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, bt.class_id, ar.activity_name, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, bt.transaction_status FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id LEFT JOIN ACTIVITY_REQUEST ar ON bt.request_id = ar.request_id WHERE bt.borrower_id = ? ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, personId);
    }

    @Override
    public List<DataClass.BorrowRecord> getRecordsByStatus(String status) {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, bt.class_id, ar.activity_name, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, bt.transaction_status FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id LEFT JOIN ACTIVITY_REQUEST ar ON bt.request_id = ar.request_id WHERE bt.transaction_status = ? ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, status);
    }

    @Override
    public List<DataClass.BorrowRecord> getRecordsByClassId(String classId) {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, bt.class_id, NULL AS activity_name, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, bt.transaction_status FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id WHERE bt.class_id = ? ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, classId);
    }

    @Override
    public List<DataClass.BorrowRecord> getRecordsByRequestId(int requestId) {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, bt.class_id, ar.activity_name, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, bt.transaction_status FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id LEFT JOIN ACTIVITY_REQUEST ar ON bt.request_id = ar.request_id WHERE bt.request_id = ? ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithIntParam(sql, requestId);
    }

    @Override
    public List<DataClass.BorrowRecord> getAllRecords() {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, bt.class_id, ar.activity_name, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, bt.transaction_status FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id LEFT JOIN ACTIVITY_REQUEST ar ON bt.request_id = ar.request_id ORDER BY bt.borrow_date DESC";
        List<DataClass.BorrowRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<DataClass.BorrowItem> getItemsByTransactionId(int transactionId) {
        String sql = "SELECT bi.transaction_id, bi.barcode, e.item_name, bi.item_condition_out, bi.item_condition_in, IFNULL(bi.damage_notes, '-') AS damage_notes FROM BORROW_ITEM bi JOIN EQUIPMENT e ON bi.barcode = e.barcode WHERE bi.transaction_id = ?";
        List<DataClass.BorrowItem> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new DataClass.BorrowItem(
                        rs.getInt("transaction_id"), rs.getString("barcode"), rs.getString("item_name"),
                        rs.getString("item_condition_out"), rs.getString("item_condition_in"), rs.getString("damage_notes")
                ));
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<DataClass.UnreturnedRecord> getUnreturnedByStatus(String status) {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, bt.transaction_status, GROUP_CONCAT(e.item_name ORDER BY e.item_name SEPARATOR ', ') AS items FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id JOIN BORROW_ITEM bi ON bt.transaction_id = bi.transaction_id JOIN EQUIPMENT e ON bi.barcode = e.barcode WHERE bt.transaction_status = ? GROUP BY bt.transaction_id, borrower_name, bt.borrower_id, borrow_date, bt.expected_return, bt.transaction_status ORDER BY bt.expected_return ASC";
        return fetchUnreturnedWithParam(sql, status);
    }

    @Override
    public List<DataClass.UnreturnedRecord> getAllProblematicRecords() {
        String sql = "SELECT bt.transaction_id, CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, bt.borrower_id, DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, bt.expected_return, bt.transaction_status, GROUP_CONCAT(e.item_name ORDER BY e.item_name SEPARATOR ', ') AS items FROM BORROW_TRANSACTION bt JOIN PERSON p ON bt.borrower_id = p.person_id JOIN BORROW_ITEM bi ON bt.transaction_id = bi.transaction_id JOIN EQUIPMENT e ON bi.barcode = e.barcode WHERE bt.transaction_status IN ('OVERDUE', 'RETURNED_WITH_ISSUE') GROUP BY bt.transaction_id, borrower_name, bt.borrower_id, borrow_date, bt.expected_return, bt.transaction_status ORDER BY bt.transaction_status, bt.expected_return ASC";
        List<DataClass.UnreturnedRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUnreturnedRow(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<DataClass.BorrowItem> getDamageDetailsForTransaction(int transactionId) {
        String sql = "SELECT bi.transaction_id, bi.barcode, e.item_name, bi.item_condition_out, bi.item_condition_in, IFNULL(bi.damage_notes, 'No notes') AS damage_notes FROM BORROW_ITEM bi JOIN EQUIPMENT e ON bi.barcode = e.barcode WHERE bi.transaction_id = ? AND (bi.item_condition_in IS NULL OR bi.item_condition_in != 'Good' OR bi.damage_notes IS NOT NULL)";
        List<DataClass.BorrowItem> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new DataClass.BorrowItem(
                        rs.getInt("transaction_id"), rs.getString("barcode"), rs.getString("item_name"),
                        rs.getString("item_condition_out"), rs.getString("item_condition_in"), rs.getString("damage_notes")
                ));
            }
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean createBorrowTransaction(String borrowerId, String custodianId, String classId, Integer requestId, String expectedReturn, List<String> barcodes) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Insert Borrow Transaction
            String insertTransSql = "INSERT INTO borrow_transaction (borrower_id, custodian_id, class_id, request_id, expected_return) VALUES (?, ?, ?, ?, ?)";
            int transactionId = -1;
            try (PreparedStatement psTrans = conn.prepareStatement(insertTransSql, Statement.RETURN_GENERATED_KEYS)) {
                psTrans.setString(1, borrowerId);
                psTrans.setString(2, custodianId);
                psTrans.setString(3, classId);
                if (requestId != null) psTrans.setInt(4, requestId); else psTrans.setNull(4, java.sql.Types.INTEGER);
                psTrans.setString(5, expectedReturn);
                psTrans.executeUpdate();

                try (ResultSet rs = psTrans.getGeneratedKeys()) {
                    if (rs.next()) transactionId = rs.getInt(1);
                }
            }

            // 2. Insert Borrow Items & Update Equipment Status to BORROWED
            String insertItemSql = "INSERT INTO borrow_item (transaction_id, barcode, item_condition_out) VALUES (?, ?, 'Good')";
            String updateEquipSql = "UPDATE equipment SET status = 'BORROWED' WHERE barcode = ?";

            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement psEquip = conn.prepareStatement(updateEquipSql)) {

                for (String barcode : barcodes) {
                    psItem.setInt(1, transactionId);
                    psItem.setString(2, barcode);
                    psItem.addBatch();

                    psEquip.setString(1, barcode);
                    psEquip.addBatch();
                }
                psItem.executeBatch();
                psEquip.executeBatch();
            }

            conn.commit(); // Commit Transaction
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback(); // Rollback on error
            } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println(" [DB ERROR] " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    @Override
    public boolean returnBorrowTransaction(int transactionId, String returnCustodianId, String transactionStatus, List<DataClass.BorrowItemReturnInfo> items) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Update Borrow Transaction
            String updateTransSql = "UPDATE borrow_transaction SET return_date = CURRENT_TIMESTAMP, return_custodian = ?, transaction_status = ? WHERE transaction_id = ?";
            try (PreparedStatement psTrans = conn.prepareStatement(updateTransSql)) {
                psTrans.setString(1, returnCustodianId);
                psTrans.setString(2, transactionStatus);
                psTrans.setInt(3, transactionId);
                psTrans.executeUpdate();
            }

            // 2. Update Borrow Items & Equipment Status
            String updateItemSql = "UPDATE borrow_item SET item_condition_in = ?, damage_notes = ? WHERE transaction_id = ? AND barcode = ?";
            String updateEquipSql = "UPDATE equipment SET status = ? WHERE barcode = ?";

            try (PreparedStatement psItem = conn.prepareStatement(updateItemSql);
                 PreparedStatement psEquip = conn.prepareStatement(updateEquipSql)) {

                for (DataClass.BorrowItemReturnInfo item : items) {
                    psItem.setString(1, item.conditionIn);
                    psItem.setString(2, item.damageNotes);
                    psItem.setInt(3, transactionId);
                    psItem.setString(4, item.barcode);
                    psItem.addBatch();

                    // If damaged, set equipment to DAMAGED, else AVAILABLE
                    String equipStatus = "Damaged".equalsIgnoreCase(item.conditionIn) ? "DAMAGED" : "AVAILABLE";
                    psEquip.setString(1, equipStatus);
                    psEquip.setString(2, item.barcode);
                    psEquip.addBatch();
                }
                psItem.executeBatch();
                psEquip.executeBatch();
            }

            conn.commit(); // Commit Transaction
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println(" [DB ERROR] " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    @Override
    public int countActiveBorrows(String personId) {
        String sql = "{? = CALL fn_count_active_borrows(?)}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, java.sql.Types.INTEGER);
            cs.setString(2, personId);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return -1;
    }

    // STEP 4: CallableStatement - Procedure
    @Override
    public void markOverdueTransactions() {
        String sql = "{CALL sp_mark_overdue_transactions()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.execute();
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
    }

    // Helpers
    private List<DataClass.BorrowRecord> fetchRecordsWithParam(String sql, String param) {
        List<DataClass.BorrowRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    private List<DataClass.BorrowRecord> fetchRecordsWithIntParam(String sql, int param) {
        List<DataClass.BorrowRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    private List<DataClass.UnreturnedRecord> fetchUnreturnedWithParam(String sql, String status) {
        List<DataClass.UnreturnedRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapUnreturnedRow(rs));
        } catch (SQLException e) {
            System.err.println(" [DB ERROR] " + e.getMessage());
        }
        return list;
    }

    private DataClass.BorrowRecord mapRecord(ResultSet rs) throws SQLException {
        return new DataClass.BorrowRecord(
                rs.getInt("transaction_id"), rs.getString("borrower_name"), rs.getString("borrower_id"),
                rs.getString("class_id"), rs.getString("activity_name"), rs.getString("borrow_date"),
                rs.getString("expected_return"), rs.getString("return_date"), rs.getString("transaction_status")
        );
    }

    private DataClass.UnreturnedRecord mapUnreturnedRow(ResultSet rs) throws SQLException {
        return new DataClass.UnreturnedRecord(
                rs.getInt("transaction_id"), rs.getString("borrower_name"), rs.getString("borrower_id"),
                rs.getString("borrow_date"), rs.getString("expected_return"),
                rs.getString("transaction_status"), rs.getString("items")
        );
    }
}