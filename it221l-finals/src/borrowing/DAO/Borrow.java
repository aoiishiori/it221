package borrowing.DAO;

import borrowing.Interface.BorrowInter;
import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Borrow DAO — all fixes applied:
 *   FIX 1: createBorrowTransaction now returns int (the generated transaction_id),
 *           or -1 on failure, instead of boolean. The UI can now show the new ID.
 *   FIX 2: getTransactionById(int) added — used by the Return form to properly
 *           check whether a transaction is still open before loading it.
 *   FIX 3: countActiveBorrows kept as SELECT (not CALL) — correct for MySQL functions.
 *   FIX 4: markOverdueTransactions uses CallableStatement — correct for procedures.
 */
public class Borrow implements BorrowInter {

    // ----------------------------------------------------------------
    // READ — by borrower
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowRecord> getRecordsByBorrowerId(String personId) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, bt.class_id, ar.activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "LEFT JOIN activity_request ar ON bt.request_id = ar.request_id " +
                "WHERE bt.borrower_id = ? " +
                "ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, personId);
    }

    // ----------------------------------------------------------------
    // READ — by status
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowRecord> getRecordsByStatus(String status) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, bt.class_id, ar.activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "LEFT JOIN activity_request ar ON bt.request_id = ar.request_id " +
                "WHERE bt.transaction_status = ? " +
                "ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, status);
    }

    // ----------------------------------------------------------------
    // READ — by class
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowRecord> getRecordsByClassId(String classId) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, bt.class_id, NULL AS activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "WHERE bt.class_id = ? " +
                "ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithParam(sql, classId);
    }

    // ----------------------------------------------------------------
    // READ — by request
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowRecord> getRecordsByRequestId(int requestId) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, bt.class_id, ar.activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "LEFT JOIN activity_request ar ON bt.request_id = ar.request_id " +
                "WHERE bt.request_id = ? " +
                "ORDER BY bt.borrow_date DESC";
        return fetchRecordsWithIntParam(sql, requestId);
    }

    // ----------------------------------------------------------------
    // READ — all records (Custodian / Admin view)
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowRecord> getAllRecords() {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name, " +
                "       ' (', IFNULL(ua.username,'no-user'), ')') AS borrower_name, " +
                "bt.borrower_id, bt.class_id, ar.activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "LEFT JOIN user_account ua ON p.person_id = ua.person_id " +
                "LEFT JOIN activity_request ar ON bt.request_id = ar.request_id " +
                "ORDER BY bt.borrow_date DESC";

        List<DataClass.BorrowRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllRecords: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // READ — single transaction by ID (FIX: used to validate open status)
    // ----------------------------------------------------------------
    @Override
    public DataClass.BorrowRecord getTransactionById(int transactionId) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, bt.class_id, ar.activity_name, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, " +
                "DATE_FORMAT(bt.return_date, '%Y-%m-%d %H:%i') AS return_date, " +
                "bt.transaction_status " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "LEFT JOIN activity_request ar ON bt.request_id = ar.request_id " +
                "WHERE bt.transaction_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRecord(rs);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getTransactionById: " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // READ — items for a specific transaction
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowItem> getItemsByTransactionId(int transactionId) {
        String sql = "SELECT bi.transaction_id, bi.barcode, e.item_name, " +
                "bi.item_condition_out, bi.item_condition_in, " +
                "IFNULL(bi.damage_notes, '-') AS damage_notes " +
                "FROM borrow_item bi " +
                "JOIN equipment e ON bi.barcode = e.barcode " +
                "WHERE bi.transaction_id = ?";

        List<DataClass.BorrowItem> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBorrowItem(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getItemsByTransactionId: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // READ — unreturned by status
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.UnreturnedRecord> getUnreturnedByStatus(String status) {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, bt.transaction_status, " +
                "GROUP_CONCAT(e.item_name ORDER BY e.item_name SEPARATOR ', ') AS items " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "JOIN borrow_item bi ON bt.transaction_id = bi.transaction_id " +
                "JOIN equipment e ON bi.barcode = e.barcode " +
                "WHERE bt.transaction_status = ? " +
                "GROUP BY bt.transaction_id, borrower_name, bt.borrower_id, " +
                "         borrow_date, bt.expected_return, bt.transaction_status " +
                "ORDER BY bt.expected_return ASC";
        return fetchUnreturnedWithParam(sql, status);
    }

    // ----------------------------------------------------------------
    // READ — all problematic (OVERDUE + RETURNED_WITH_ISSUE)
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.UnreturnedRecord> getAllProblematicRecords() {
        String sql = "SELECT bt.transaction_id, " +
                "CONCAT(p.last_name, ', ', p.first_name) AS borrower_name, " +
                "bt.borrower_id, " +
                "DATE_FORMAT(bt.borrow_date, '%Y-%m-%d %H:%i') AS borrow_date, " +
                "bt.expected_return, bt.transaction_status, " +
                "GROUP_CONCAT(e.item_name ORDER BY e.item_name SEPARATOR ', ') AS items " +
                "FROM borrow_transaction bt " +
                "JOIN person p ON bt.borrower_id = p.person_id " +
                "JOIN borrow_item bi ON bt.transaction_id = bi.transaction_id " +
                "JOIN equipment e ON bi.barcode = e.barcode " +
                "WHERE bt.transaction_status IN ('OVERDUE','RETURNED_WITH_ISSUE') " +
                "GROUP BY bt.transaction_id, borrower_name, bt.borrower_id, " +
                "         borrow_date, bt.expected_return, bt.transaction_status " +
                "ORDER BY bt.transaction_status, bt.expected_return ASC";

        List<DataClass.UnreturnedRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUnreturnedRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getAllProblematicRecords: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // READ — damage details for a transaction
    // ----------------------------------------------------------------
    @Override
    public List<DataClass.BorrowItem> getDamageDetailsForTransaction(int transactionId) {
        String sql = "SELECT bi.transaction_id, bi.barcode, e.item_name, " +
                "bi.item_condition_out, bi.item_condition_in, " +
                "IFNULL(bi.damage_notes, 'No notes') AS damage_notes " +
                "FROM borrow_item bi " +
                "JOIN equipment e ON bi.barcode = e.barcode " +
                "WHERE bi.transaction_id = ? " +
                "  AND (bi.item_condition_in IS NULL " +
                "       OR bi.item_condition_in != 'Good' " +
                "       OR bi.damage_notes IS NOT NULL)";

        List<DataClass.BorrowItem> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBorrowItem(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getDamageDetails: " + e.getMessage());
        }
        return list;
    }

    // ----------------------------------------------------------------
    // CREATE — borrow transaction
    // FIX: now returns int (generated transaction_id) instead of boolean.
    //      Returns -1 on any failure so the UI can detect errors.
    //      Availability check is retained.
    // ----------------------------------------------------------------
    @Override
    public int createBorrowTransaction(String borrowerId, String custodianId,
                                       String classId, Integer requestId,
                                       String expectedReturn, List<String> barcodes) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            // Check every barcode is AVAILABLE before inserting
            String checkSql = "SELECT barcode FROM equipment WHERE barcode = ? AND status = 'AVAILABLE'";
            for (String barcode : barcodes) {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, barcode);
                    ResultSet rs = check.executeQuery();
                    if (!rs.next()) {
                        System.err.println("[VALIDATION] Barcode not available: " + barcode);
                        conn.rollback();
                        return -1;
                    }
                }
            }

            // Insert transaction header
            String insertTransSql =
                    "INSERT INTO borrow_transaction " +
                            "(borrower_id, custodian_id, class_id, request_id, expected_return) " +
                            "VALUES (?, ?, ?, ?, ?)";
            int transactionId = -1;
            try (PreparedStatement psTrans = conn.prepareStatement(
                    insertTransSql, Statement.RETURN_GENERATED_KEYS)) {
                psTrans.setString(1, borrowerId);
                psTrans.setString(2, custodianId);
                if (classId != null && !classId.isEmpty()) psTrans.setString(3, classId);
                else                                        psTrans.setNull(3, Types.VARCHAR);
                if (requestId != null) psTrans.setInt(4, requestId);
                else                   psTrans.setNull(4, Types.INTEGER);
                psTrans.setString(5, expectedReturn);
                psTrans.executeUpdate();
                try (ResultSet rs = psTrans.getGeneratedKeys()) {
                    if (rs.next()) transactionId = rs.getInt(1);
                }
            }

            if (transactionId == -1) { conn.rollback(); return -1; }

            // Insert borrow items + update equipment status
            String insertItemSql  = "INSERT INTO borrow_item " +
                    "(transaction_id, barcode, item_condition_out) VALUES (?, ?, 'Good')";
            String updateEquipSql = "UPDATE equipment SET status = 'BORROWED' WHERE barcode = ?";

            try (PreparedStatement psItem  = conn.prepareStatement(insertItemSql);
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

            conn.commit();
            return transactionId; // FIX: return the actual ID

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[DB ERROR] createBorrowTransaction: " + e.getMessage());
            return -1;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ----------------------------------------------------------------
    // UPDATE — return transaction
    // ----------------------------------------------------------------
    @Override
    public boolean returnBorrowTransaction(int transactionId, String returnCustodianId,
                                           String transactionStatus,
                                           List<DataClass.BorrowItemReturnInfo> items) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false);

            String updateTransSql =
                    "UPDATE borrow_transaction " +
                            "SET return_date = CURRENT_TIMESTAMP, return_custodian = ?, transaction_status = ? " +
                            "WHERE transaction_id = ?";
            try (PreparedStatement psTrans = conn.prepareStatement(updateTransSql)) {
                psTrans.setString(1, returnCustodianId);
                psTrans.setString(2, transactionStatus);
                psTrans.setInt(3, transactionId);
                psTrans.executeUpdate();
            }

            String updateItemSql  =
                    "UPDATE borrow_item " +
                            "SET item_condition_in = ?, damage_notes = ? " +
                            "WHERE transaction_id = ? AND barcode = ?";
            String updateEquipSql = "UPDATE equipment SET status = ? WHERE barcode = ?";

            try (PreparedStatement psItem  = conn.prepareStatement(updateItemSql);
                 PreparedStatement psEquip = conn.prepareStatement(updateEquipSql)) {
                for (DataClass.BorrowItemReturnInfo item : items) {
                    psItem.setString(1, item.conditionIn);
                    psItem.setString(2, item.damageNotes);
                    psItem.setInt(3, transactionId);
                    psItem.setString(4, item.barcode);
                    psItem.addBatch();
                    String equipStatus = "Damaged".equalsIgnoreCase(item.conditionIn)
                            ? "DAMAGED" : "AVAILABLE";
                    psEquip.setString(1, equipStatus);
                    psEquip.setString(2, item.barcode);
                    psEquip.addBatch();
                }
                psItem.executeBatch();
                psEquip.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[DB ERROR] returnBorrowTransaction: " + e.getMessage());
            return false;
        } finally {
            try { if (conn != null) conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ----------------------------------------------------------------
    // Stored FUNCTION — SELECT fn_name(?) (correct for MySQL functions)
    // ----------------------------------------------------------------
    @Override
    public int countActiveBorrows(String personId) {
        String sql = "SELECT fn_count_active_borrows(?) AS active_count";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("active_count");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] countActiveBorrows: " + e.getMessage());
        }
        return -1;
    }

    // ----------------------------------------------------------------
    // Stored PROCEDURE — CallableStatement (correct for procedures)
    // ----------------------------------------------------------------
    @Override
    public void markOverdueTransactions() {
        String sql = "{CALL sp_mark_overdue_transactions()}";
        try (Connection conn = Database.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.execute();
            System.out.println("[PROC] sp_mark_overdue_transactions executed.");
        } catch (SQLException e) {
            System.err.println("[DB ERROR] markOverdueTransactions: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------
    private List<DataClass.BorrowRecord> fetchRecordsWithParam(String sql, String param) {
        List<DataClass.BorrowRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRecord(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] fetchRecordsWithParam: " + e.getMessage());
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
            System.err.println("[DB ERROR] fetchRecordsWithIntParam: " + e.getMessage());
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
            System.err.println("[DB ERROR] fetchUnreturnedWithParam: " + e.getMessage());
        }
        return list;
    }

    private DataClass.BorrowRecord mapRecord(ResultSet rs) throws SQLException {
        return new DataClass.BorrowRecord(
                rs.getInt("transaction_id"),
                rs.getString("borrower_name"),
                rs.getString("borrower_id"),
                rs.getString("class_id"),
                rs.getString("activity_name"),
                rs.getString("borrow_date"),
                rs.getString("expected_return"),
                rs.getString("return_date"),
                rs.getString("transaction_status")
        );
    }

    private DataClass.BorrowItem mapBorrowItem(ResultSet rs) throws SQLException {
        return new DataClass.BorrowItem(
                rs.getInt("transaction_id"),
                rs.getString("barcode"),
                rs.getString("item_name"),
                rs.getString("item_condition_out"),
                rs.getString("item_condition_in"),
                rs.getString("damage_notes")
        );
    }

    private DataClass.UnreturnedRecord mapUnreturnedRow(ResultSet rs) throws SQLException {
        return new DataClass.UnreturnedRecord(
                rs.getInt("transaction_id"),
                rs.getString("borrower_name"),
                rs.getString("borrower_id"),
                rs.getString("borrow_date"),
                rs.getString("expected_return"),
                rs.getString("transaction_status"),
                rs.getString("items")
        );
    }
}