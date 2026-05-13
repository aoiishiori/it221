package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface BorrowInter {
    List<DataClass.BorrowRecord> getRecordsByBorrowerId(String personId);
    List<DataClass.BorrowRecord> getRecordsByStatus(String status);
    List<DataClass.BorrowRecord> getRecordsByClassId(String classId);
    List<DataClass.BorrowRecord> getRecordsByRequestId(int requestId);
    List<DataClass.BorrowRecord> getAllRecords();
    List<DataClass.BorrowItem> getItemsByTransactionId(int transactionId);
    List<DataClass.UnreturnedRecord> getUnreturnedByStatus(String status);
    List<DataClass.UnreturnedRecord> getAllProblematicRecords();
    List<DataClass.BorrowItem> getDamageDetailsForTransaction(int transactionId);

    // FIX: returns int (generated transaction_id) instead of boolean so UI can show it
    int createBorrowTransaction(String borrowerId, String custodianId, String classId,
                                Integer requestId, String expectedReturn, List<String> barcodes);

    boolean returnBorrowTransaction(int transactionId, String returnCustodianId,
                                    String transactionStatus,
                                    List<DataClass.BorrowItemReturnInfo> items);

    // FIX: added to safely check transaction status before processing return
    DataClass.BorrowRecord getTransactionById(int transactionId);

    int countActiveBorrows(String personId); // Calls Function
    void markOverdueTransactions();          // Calls Procedure
}