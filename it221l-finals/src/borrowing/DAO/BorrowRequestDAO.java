package borrowing.DAO;

import borrowing.model.DataClass;
import borrowing.util.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BorrowRequestDAO — handles the borrow_request table.
 *
 * Students submit requests here; Custodians (or Admin) approve them,
 * which triggers an actual borrow_transaction to be created.
 */
public class BorrowRequestDAO {

    // ----------------------------------------------------------------
    // READ — all requests (Admin / Custodian view)
    // ----------------------------------------------------------------
    public List<DataClass.BorrowRequestRecord> getAllRequests() {
        String sql =
                "SELECT br.borrow_req_id, br.requester_id, " +
                        "CONCAT(p.last_name,', ',p.first_name) AS requester_name, " +
                        "p.person_type, br.item_barcodes, br.purpose, " +
                        "br.needed_date, br.expected_return, br.status, " +
                        "br.review_notes, br.transaction_id, " +
                        "DATE_FORMAT(br.created_at,'%Y-%m-%d %H:%i') AS created_at " +
                        "FROM borrow_request br " +
                        "JOIN person p ON br.requester_id = p.person_id " +
                        "ORDER BY br.created_at DESC";
        return fetchList(sql);
    }

    // READ — by requester (Borrower's own requests)
    public List<DataClass.BorrowRequestRecord> getRequestsByRequester(String requesterId) {
        String sql =
                "SELECT br.borrow_req_id, br.requester_id, " +
                        "CONCAT(p.last_name,', ',p.first_name) AS requester_name, " +
                        "p.person_type, br.item_barcodes, br.purpose, " +
                        "br.needed_date, br.expected_return, br.status, " +
                        "br.review_notes, br.transaction_id, " +
                        "DATE_FORMAT(br.created_at,'%Y-%m-%d %H:%i') AS created_at " +
                        "FROM borrow_request br " +
                        "JOIN person p ON br.requester_id = p.person_id " +
                        "WHERE br.requester_id = ? " +
                        "ORDER BY br.created_at DESC";
        List<DataClass.BorrowRequestRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requesterId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getRequestsByRequester: " + e.getMessage());
        }
        return list;
    }

    // READ — pending only
    public List<DataClass.BorrowRequestRecord> getPendingRequests() {
        String sql =
                "SELECT br.borrow_req_id, br.requester_id, " +
                        "CONCAT(p.last_name,', ',p.first_name) AS requester_name, " +
                        "p.person_type, br.item_barcodes, br.purpose, " +
                        "br.needed_date, br.expected_return, br.status, " +
                        "br.review_notes, br.transaction_id, " +
                        "DATE_FORMAT(br.created_at,'%Y-%m-%d %H:%i') AS created_at " +
                        "FROM borrow_request br " +
                        "JOIN person p ON br.requester_id = p.person_id " +
                        "WHERE br.status = 'PENDING' " +
                        "ORDER BY br.needed_date ASC";
        return fetchList(sql);
    }

    // ----------------------------------------------------------------
    // CREATE — student submits a borrow request
    // ----------------------------------------------------------------
    public int submitRequest(String requesterId, String itemBarcodes,
                             String purpose, String neededDate, String expectedReturn) {
        if (requesterId == null || requesterId.isBlank()) return -1;
        if (itemBarcodes == null || itemBarcodes.isBlank()) return -1;

        String sql = "INSERT INTO borrow_request " +
                "(requester_id, item_barcodes, purpose, needed_date, expected_return) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, requesterId);
            ps.setString(2, itemBarcodes);
            ps.setString(3, purpose);
            ps.setString(4, neededDate);
            ps.setString(5, expectedReturn);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] submitRequest: " + e.getMessage());
        }
        return -1;
    }

    // ----------------------------------------------------------------
    // UPDATE — custodian approves or rejects
    // Returns the new transaction_id if approved and transaction created, else -1.
    // ----------------------------------------------------------------
    public int approveRequest(int borrowReqId, String custodianId,
                              String custodianPersonId, String reviewNotes,
                              Borrow borrowDAO) {
        // 1. Load the request
        DataClass.BorrowRequestRecord req = getById(borrowReqId);
        if (req == null || !"PENDING".equals(req.status)) return -1;

        // 2. Create a real borrow_transaction
        List<String> barcodes = new java.util.Arrays.asList(req.itemBarcodes.split(",")).stream()
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        // Use java.util.Arrays properly
        barcodes = new ArrayList<>(barcodes);

        int txnId = borrowDAO.createBorrowTransaction(
                req.requesterId, custodianPersonId,
                null, null,
                req.expectedReturn, barcodes);

        if (txnId <= 0) return -1; // items no longer available

        // 3. Mark request APPROVED and link transaction
        String sql = "UPDATE borrow_request SET status='APPROVED', reviewed_by=?, " +
                "review_notes=?, transaction_id=? WHERE borrow_req_id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, custodianPersonId);
            ps.setString(2, reviewNotes);
            ps.setInt(3, txnId);
            ps.setInt(4, borrowReqId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] approveRequest: " + e.getMessage());
        }
        return txnId;
    }

    public boolean rejectRequest(int borrowReqId, String custodianPersonId, String reviewNotes) {
        String sql = "UPDATE borrow_request SET status='REJECTED', reviewed_by=?, " +
                "review_notes=? WHERE borrow_req_id=? AND status='PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, custodianPersonId);
            ps.setString(2, reviewNotes);
            ps.setInt(3, borrowReqId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[DB ERROR] rejectRequest: " + e.getMessage());
        }
        return false;
    }

    // ----------------------------------------------------------------
    // READ single
    // ----------------------------------------------------------------
    public DataClass.BorrowRequestRecord getById(int borrowReqId) {
        String sql =
                "SELECT br.borrow_req_id, br.requester_id, " +
                        "CONCAT(p.last_name,', ',p.first_name) AS requester_name, " +
                        "p.person_type, br.item_barcodes, br.purpose, " +
                        "br.needed_date, br.expected_return, br.status, " +
                        "br.review_notes, br.transaction_id, " +
                        "DATE_FORMAT(br.created_at,'%Y-%m-%d %H:%i') AS created_at " +
                        "FROM borrow_request br " +
                        "JOIN person p ON br.requester_id = p.person_id " +
                        "WHERE br.borrow_req_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, borrowReqId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[DB ERROR] getById: " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------
    private List<DataClass.BorrowRequestRecord> fetchList(String sql) {
        List<DataClass.BorrowRequestRecord> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("[DB ERROR] fetchList BorrowRequest: " + e.getMessage());
        }
        return list;
    }

    private DataClass.BorrowRequestRecord mapRow(ResultSet rs) throws SQLException {
        return new DataClass.BorrowRequestRecord(
                rs.getInt("borrow_req_id"),
                rs.getString("requester_id"),
                rs.getString("requester_name"),
                rs.getString("person_type"),
                rs.getString("item_barcodes"),
                rs.getString("purpose"),
                rs.getString("needed_date"),
                rs.getString("expected_return"),
                rs.getString("status"),
                rs.getString("review_notes"),
                rs.getObject("transaction_id") != null ? rs.getInt("transaction_id") : null,
                rs.getString("created_at")
        );
    }
}