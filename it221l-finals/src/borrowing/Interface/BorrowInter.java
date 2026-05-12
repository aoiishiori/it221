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

    boolean createBorrowTransaction(String borrowerId, String custodianId, String classId, Integer requestId, String expectedReturn, List<String> barcodes);
    boolean returnBorrowTransaction(int transactionId, String returnCustodianId, String transactionStatus, List<DataClass.BorrowItemReturnInfo> items);
    int countActiveBorrows(String personId); // Calls Function
    void markOverdueTransactions();          // Calls Procedure
}