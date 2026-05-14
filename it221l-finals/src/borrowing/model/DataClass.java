package borrowing.model;

/**
 * DataClass — Plain Old Java Objects (POJOs) for all borrowing-system entities.
 *
 * v2 additions:
 *   - AuditEntry         (audit_log table)
 *   - BorrowRequestRecord (borrow_request table — student-initiated requests)
 */
public class DataClass {

    // ----------------------------------------------------------------
    // Equipment
    // ----------------------------------------------------------------
    public static class Equipment {
        public String barcode, itemName, category, brand, model, status, remarks;
        public String expectedReturn, returnDate;

        public String getBarcode()       { return barcode; }
        public String getItemName()      { return itemName; }
        public String getCategory()      { return category; }
        public String getBrand()         { return brand; }
        public String getModel()         { return model; }
        public String getStatus()        { return status; }
        public String getRemarks()       { return remarks; }
        public String getExpectedReturn(){ return expectedReturn; }
        public String getReturnDate()    { return returnDate; }

        public Equipment(String barcode, String itemName, String category,
                         String brand, String model, String status, String remarks) {
            this.barcode   = barcode;
            this.itemName  = itemName;
            this.category  = category;
            this.brand     = brand;
            this.model     = model;
            this.status    = status;
            this.remarks   = remarks;
        }

        @Override
        public String toString() {
            return barcode + " — " + itemName + " (" + brand + " " + model + ") [" + status + "]";
        }
    }

    // ----------------------------------------------------------------
    // BorrowRecord (summary row in transaction list)
    // ----------------------------------------------------------------
    public static class BorrowRecord {
        public int    transactionId;
        public String borrowerName, borrowerId, classId, activityName;
        public String borrowDate, expectedReturn, returnDate, status;

        public int    getTransactionId() { return transactionId; }
        public String getBorrowerName()  { return borrowerName; }
        public String getBorrowerId()    { return borrowerId; }
        public String getClassId()       { return classId; }
        public String getActivityName()  { return activityName; }
        public String getBorrowDate()    { return borrowDate; }
        public String getExpectedReturn(){ return expectedReturn; }
        public String getReturnDate()    { return returnDate; }
        public String getStatus()        { return status; }

        public BorrowRecord(int transactionId, String borrowerName, String borrowerId,
                            String classId, String activityName,
                            String borrowDate, String expectedReturn,
                            String returnDate, String status) {
            this.transactionId  = transactionId;
            this.borrowerName   = borrowerName;
            this.borrowerId     = borrowerId;
            this.classId        = classId       != null ? classId       : "-";
            this.activityName   = activityName  != null ? activityName  : "-";
            this.borrowDate     = borrowDate;
            this.expectedReturn = expectedReturn;
            this.returnDate     = returnDate    != null ? returnDate     : "NOT RETURNED";
            this.status         = status;
        }

        @Override
        public String toString() {
            return String.format("TXN#%-4d | %-25s | Class: %-12s | Activity: %-25s | " +
                            "Borrowed: %-20s | Return Due: %-12s | Returned: %-20s | Status: %s",
                    transactionId, borrowerName + " (" + borrowerId + ")",
                    classId, activityName, borrowDate, expectedReturn, returnDate, status);
        }
    }

    // ----------------------------------------------------------------
    // BorrowItem (individual item within a transaction)
    // ----------------------------------------------------------------
    public static class BorrowItem {
        public int    transactionId;
        public String barcode, itemName, conditionOut, conditionIn, damageNotes;

        public int    getTransactionId()  { return transactionId; }
        public String getBarcode()        { return barcode; }
        public String getItemName()       { return itemName; }
        public String getConditionOut()   { return conditionOut; }
        public String getConditionIn()    { return conditionIn; }
        public String getDamageNotes()    { return damageNotes; }

        public BorrowItem(int transactionId, String barcode, String itemName,
                          String conditionOut, String conditionIn, String damageNotes) {
            this.transactionId = transactionId;
            this.barcode       = barcode;
            this.itemName      = itemName;
            this.conditionOut  = conditionOut;
            this.conditionIn   = conditionIn   != null ? conditionIn   : "NOT RETURNED";
            this.damageNotes   = damageNotes   != null ? damageNotes   : "-";
        }

        @Override
        public String toString() {
            return String.format(" Barcode: %-12s | Item: %-22s | Out: %-8s | In: %-15s | Damage: %s",
                    barcode, itemName, conditionOut, conditionIn, damageNotes);
        }
    }

    // ----------------------------------------------------------------
    // UnreturnedRecord — problematic / overdue summary
    // ----------------------------------------------------------------
    public static class UnreturnedRecord {
        public int    transactionId;
        public String borrowerName, borrowerId, borrowDate, expectedReturn, status, items;

        public int    getTransactionId()  { return transactionId; }
        public String getBorrowerName()   { return borrowerName; }
        public String getBorrowerId()     { return borrowerId; }
        public String getBorrowDate()     { return borrowDate; }
        public String getExpectedReturn() { return expectedReturn; }
        public String getStatus()         { return status; }
        public String getItems()          { return items; }

        public UnreturnedRecord(int transactionId, String borrowerName, String borrowerId,
                                String borrowDate, String expectedReturn,
                                String status, String items) {
            this.transactionId  = transactionId;
            this.borrowerName   = borrowerName;
            this.borrowerId     = borrowerId;
            this.borrowDate     = borrowDate;
            this.expectedReturn = expectedReturn;
            this.status         = status;
            this.items          = items;
        }

        @Override
        public String toString() {
            return String.format("TXN#%-4d | %-25s | Due: %-12s | Status: %-22s | Items: %s",
                    transactionId, borrowerName + " (" + borrowerId + ")",
                    expectedReturn, status, items);
        }
    }

    // ----------------------------------------------------------------
    // PersonSummary
    // ----------------------------------------------------------------
    public static class PersonSummary {
        public String personId, fullName, personType;
        public int    totalBorrows;

        public PersonSummary(String personId, String fullName, String personType, int totalBorrows) {
            this.personId    = personId;
            this.fullName    = fullName;
            this.personType  = personType;
            this.totalBorrows = totalBorrows;
        }
    }

    // ----------------------------------------------------------------
    // StudentRecord
    // ----------------------------------------------------------------
    public static class StudentRecord {
        public String personId, lastName, firstName, middleName, email, contactNo, status;
        public int classCount;

        public String getPersonId() { return personId; }
        public String getLastName() { return lastName; }
        public String getFirstName() { return firstName; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }

        public StudentRecord(String personId, String lastName, String firstName,
                             String middleName, String email, String contactNo,
                             String status, int classCount) {
            this.personId = personId;
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.email = email;
            this.contactNo = contactNo;
            this.status = status;
            this.classCount = classCount;
        }
    }

    // ----------------------------------------------------------------
    // StaffFacultyRecord
    // ----------------------------------------------------------------
    public static class StaffFacultyRecord {
        public String personId, lastName, firstName, middleName, personType, email, contactNo, status;
        public int classesHandled;

        public String getStatus() { return status; }

        public StaffFacultyRecord(String personId, String lastName, String firstName,
                                  String middleName, String personType,
                                  String email, String contactNo, String status,
                                  int classesHandled) {
            this.personId = personId;
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.personType = personType;
            this.email = email;
            this.contactNo = contactNo;
            this.status = status;
            this.classesHandled = classesHandled;
        }
    }

    // ----------------------------------------------------------------
    // LabClassRecord
    // ----------------------------------------------------------------
    public static class LabClassRecord {
        public String classId, courseCode, section, semester, schoolYear, room, schedule, facultyName;
        public int    enrolledCount;

        public String getClassId()      { return classId; }
        public String getCourseCode()   { return courseCode; }
        public String getSection()      { return section; }
        public String getSemester()     { return semester; }
        public String getSchoolYear()   { return schoolYear; }
        public String getRoom()         { return room; }
        public String getSchedule()     { return schedule; }
        public String getFacultyName()  { return facultyName; }
        public int    getEnrolledCount(){ return enrolledCount; }

        public LabClassRecord(String classId, String courseCode, String section,
                              String semester, String schoolYear, String room,
                              String schedule, String facultyName, int enrolledCount) {
            this.classId       = classId;
            this.courseCode    = courseCode;
            this.section       = section;
            this.semester      = semester;
            this.schoolYear    = schoolYear;
            this.room          = room;
            this.schedule      = schedule;
            this.facultyName   = facultyName;
            this.enrolledCount = enrolledCount;
        }
    }

    // ----------------------------------------------------------------
    // ActivityRecord  — added requestedBy + requesterName
    // ----------------------------------------------------------------
    public static class ActivityRecord {
        public int    requestId;
        public String activityName, activityType, activityDate, status;
        public String requestedBy;
        public String requesterName;

        public int    getRequestId()     { return requestId; }
        public String getActivityName()  { return activityName; }
        public String getActivityType()  { return activityType; }
        public String getActivityDate()  { return activityDate; }
        public String getStatus()        { return status; }
        public String getRequestedBy()   { return requestedBy; }
        public String getRequesterName() { return requesterName; }

        public ActivityRecord(int requestId, String activityName, String activityType,
                              String activityDate, String status,
                              String requestedBy, String requesterName) {
            this.requestId     = requestId;
            this.activityName  = activityName;
            this.activityType  = activityType;
            this.activityDate  = activityDate;
            this.status        = status;
            this.requestedBy   = requestedBy;
            this.requesterName = requesterName;
        }
    }

    // ----------------------------------------------------------------
    // BorrowItemReturnInfo — used when processing a return
    // ----------------------------------------------------------------
    public static class BorrowItemReturnInfo {
        public String barcode, conditionIn, damageNotes;

        public BorrowItemReturnInfo(String barcode, String conditionIn, String damageNotes) {
            this.barcode      = barcode;
            this.conditionIn  = conditionIn;
            this.damageNotes  = damageNotes;
        }
    }

    // ----------------------------------------------------------------
    // AuditEntry — NEW: represents one row from audit_log
    // ----------------------------------------------------------------
    public static class AuditEntry {
        public int    logId;
        public String personId, username, action, entity, entityId, details, loggedAt;

        public int    getLogId()    { return logId; }
        public String getPersonId() { return personId; }
        public String getUsername() { return username; }
        public String getAction()   { return action; }
        public String getEntity()   { return entity; }
        public String getEntityId() { return entityId; }
        public String getDetails()  { return details; }
        public String getLoggedAt() { return loggedAt; }

        public AuditEntry(int logId, String personId, String username,
                          String action, String entity, String entityId,
                          String details, String loggedAt) {
            this.logId    = logId;
            this.personId = personId;
            this.username = username;
            this.action   = action;
            this.entity   = entity;
            this.entityId = entityId;
            this.details  = details;
            this.loggedAt = loggedAt;
        }
    }

    // ----------------------------------------------------------------
    // BorrowRequestRecord — NEW: student-initiated borrow request
    // ----------------------------------------------------------------
    public static class BorrowRequestRecord {
        public int     borrowReqId;
        public String  requesterId, requesterName, personType;
        public String  itemBarcodes, purpose;
        public String  neededDate, expectedReturn, status;
        public String  reviewNotes, createdAt;
        public Integer transactionId; // null until approved

        public int     getBorrowReqId()   { return borrowReqId; }
        public String  getRequesterId()   { return requesterId; }
        public String  getRequesterName() { return requesterName; }
        public String  getPersonType()    { return personType; }
        public String  getItemBarcodes()  { return itemBarcodes; }
        public String  getPurpose()       { return purpose != null ? purpose : "—"; }
        public String  getNeededDate()    { return neededDate; }
        public String  getExpectedReturn(){ return expectedReturn; }
        public String  getStatus()        { return status; }
        public String  getReviewNotes()   { return reviewNotes != null ? reviewNotes : "—"; }
        public String  getCreatedAt()     { return createdAt; }
        public String  getTransactionId() {
            return transactionId != null ? String.valueOf(transactionId) : "—";
        }

        public BorrowRequestRecord(int borrowReqId, String requesterId, String requesterName,
                                   String personType, String itemBarcodes, String purpose,
                                   String neededDate, String expectedReturn, String status,
                                   String reviewNotes, Integer transactionId, String createdAt) {
            this.borrowReqId   = borrowReqId;
            this.requesterId   = requesterId;
            this.requesterName = requesterName;
            this.personType    = personType;
            this.itemBarcodes  = itemBarcodes;
            this.purpose       = purpose;
            this.neededDate    = neededDate;
            this.expectedReturn = expectedReturn;
            this.status        = status;
            this.reviewNotes   = reviewNotes;
            this.transactionId = transactionId;
            this.createdAt     = createdAt;
        }
    }
}