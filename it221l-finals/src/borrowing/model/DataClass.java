package borrowing.model;

/**
 * DataClasses - Plain Old Java Objects (POJOs) for the borrowing system entities.
 */
public class DataClass {

    // ----------------------------------------------------------------
    // Equipment
    // ----------------------------------------------------------------
    public static class Equipment {
        public String barcode;
        public String itemName;
        public String category;
        public String brand;
        public String model;
        public String status;
        public String remarks;
        public String expectedReturn; // Needed for the new getter
        public String returnDate;     // Needed for the new getter

        public String getBarcode() { return barcode; }
        public String getItemName() { return itemName; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public String getRemarks() { return remarks; }

        public String getExpectedReturn() { return expectedReturn; } // Needed for the new getter
        public String getReturnDate()     { return returnDate; } // Needed for the new getter

        public Equipment(String barcode, String itemName, String category,
                         String brand, String model, String status, String remarks) {
            this.barcode = barcode;
            this.itemName = itemName;
            this.category = category;
            this.brand = brand;
            this.model = model;
            this.status = status;
            this.remarks = remarks;
        }

        @Override
        public String toString() {
            return String.format("%-12s %-22s %-12s %-10s %-16s %-12s %s",
                    barcode, itemName, category, brand, model, status,
                    (remarks != null ? remarks : "-"));
        }
    }

    // ----------------------------------------------------------------
    // BorrowTransaction (summary for listing)
    // ----------------------------------------------------------------
    public static class BorrowRecord {
        public int transactionId;
        public String borrowerName;
        public String borrowerId;
        public String classId;
        public String activityName;
        public String borrowDate;
        public String expectedReturn;
        public String returnDate;
        public String status;

        public int getTransactionId() { return transactionId; }
        public String getBorrowerName() { return borrowerName; }
        public String getBorrowDate() { return borrowDate; }
        public String getStatus() { return status; }

        public BorrowRecord(int transactionId, String borrowerName, String borrowerId,
                            String classId, String activityName,
                            String borrowDate, String expectedReturn,
                            String returnDate, String status) {
            this.transactionId = transactionId;
            this.borrowerName = borrowerName;
            this.borrowerId = borrowerId;
            this.classId = classId != null ? classId : "-";
            this.activityName = activityName != null ? activityName : "-";
            this.borrowDate = borrowDate;
            this.expectedReturn = expectedReturn;
            this.returnDate = returnDate != null ? returnDate : "NOT RETURNED";
            this.status = status;
        }

        @Override
        public String toString() {
            return String.format("TXN#%-4d | %-25s | Class: %-12s | Activity: %-25s | Borrowed: %-20s | Return Due: %-12s | Returned: %-20s | Status: %s",
                    transactionId, borrowerName + " (" + borrowerId + ")",
                    classId, activityName, borrowDate, expectedReturn, returnDate, status);
        }
    }

    // ----------------------------------------------------------------
    // BorrowItem (individual item within a transaction)
    // ----------------------------------------------------------------
    public static class BorrowItem {
        public int transactionId;
        public String barcode;
        public String itemName;
        public String conditionOut;
        public String conditionIn;
        public String damageNotes;

        public BorrowItem(int transactionId, String barcode, String itemName,
                          String conditionOut, String conditionIn, String damageNotes) {
            this.transactionId = transactionId;
            this.barcode = barcode;
            this.itemName = itemName;
            this.conditionOut = conditionOut;
            this.conditionIn = conditionIn != null ? conditionIn : "NOT RETURNED";
            this.damageNotes = damageNotes != null ? damageNotes : "-";
        }

        @Override
        public String toString() {
            return String.format(" Barcode: %-12s | Item: %-22s | Out: %-8s | In: %-15s | Damage: %s",
                    barcode, itemName, conditionOut, conditionIn, damageNotes);
        }
    }

    // ----------------------------------------------------------------
    // Unreturned / Problematic summary
    // ----------------------------------------------------------------
    public static class UnreturnedRecord {
        public int transactionId;
        public String borrowerName;
        public String borrowerId;
        public String borrowDate;
        public String expectedReturn;
        public String status;
        public String items;

        public UnreturnedRecord(int transactionId, String borrowerName, String borrowerId,
                                String borrowDate, String expectedReturn,
                                String status, String items) {
            this.transactionId = transactionId;
            this.borrowerName = borrowerName;
            this.borrowerId = borrowerId;
            this.borrowDate = borrowDate;
            this.expectedReturn = expectedReturn;
            this.status = status;
            this.items = items;
        }

        @Override
        public String toString() {
            return String.format("TXN#%-4d | %-25s | Due: %-12s | Status: %-22s | Items: %s",
                    transactionId,
                    borrowerName + " (" + borrowerId + ")",
                    expectedReturn, status, items);
        }
    }

    // ----------------------------------------------------------------
    // Additional Models for Clean DAO Returns
    // ----------------------------------------------------------------
    public static class PersonSummary {
        public String personId;
        public String fullName;
        public String personType;
        public int totalBorrows;

        public PersonSummary(String personId, String fullName, String personType, int totalBorrows) {
            this.personId = personId;
            this.fullName = fullName;
            this.personType = personType;
            this.totalBorrows = totalBorrows;
        }
    }

    public static class StudentRecord {
        public String personId;
        public String lastName;
        public String firstName;
        public String middleName;
        public String email;
        public String contactNo;
        public int classCount;

        public String getPersonId() { return personId; }
        public String getLastName() { return lastName; }
        public String getFirstName() { return firstName; }
        public String getEmail() { return email; }

        public StudentRecord(String personId, String lastName, String firstName, String middleName, String email, String contactNo, int classCount) {
            this.personId = personId;
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.email = email;
            this.contactNo = contactNo;
            this.classCount = classCount;
        }
    }

    public static class StaffFacultyRecord {
        public String personId;
        public String lastName;
        public String firstName;
        public String middleName;
        public String personType;
        public String email;
        public String contactNo;
        public int classesHandled;

        public StaffFacultyRecord(String personId, String lastName, String firstName, String middleName, String personType, String email, String contactNo, int classesHandled) {
            this.personId = personId;
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.personType = personType;
            this.email = email;
            this.contactNo = contactNo;
            this.classesHandled = classesHandled;
        }
    }

    public static class LabClassRecord {
        public String classId;
        public String courseCode;
        public String section;
        public String semester;
        public String schoolYear;
        public String room;
        public String schedule;
        public String facultyName;
        public int enrolledCount;

        public String getClassId() { return classId; }
        public String getCourseCode() { return courseCode; }
        public String getSection() { return section; }
        public String getFacultyName() { return facultyName; }

        public LabClassRecord(String classId, String courseCode, String section, String semester, String schoolYear, String room, String schedule, String facultyName, int enrolledCount) {
            this.classId = classId;
            this.courseCode = courseCode;
            this.section = section;
            this.semester = semester;
            this.schoolYear = schoolYear;
            this.room = room;
            this.schedule = schedule;
            this.facultyName = facultyName;
            this.enrolledCount = enrolledCount;
        }
    }

    public static class ActivityRecord {
        public int requestId;
        public String activityName;
        public String activityType;
        public String activityDate;
        public String status;

        public int getRequestId() { return requestId; }
        public String getActivityName() { return activityName; }
        public String getActivityType() { return activityType; }
        public String getActivityDate() { return activityDate; }
        public String getStatus() { return status; }

        public ActivityRecord(int requestId, String activityName, String activityType, String activityDate, String status) {
            this.requestId = requestId;
            this.activityName = activityName;
            this.activityType = activityType;
            this.activityDate = activityDate;
            this.status = status;
        }
    }

    public static class BorrowItemReturnInfo {
        public String barcode;
        public String conditionIn;
        public String damageNotes;

        public BorrowItemReturnInfo(String barcode, String conditionIn, String damageNotes) {
            this.barcode = barcode;
            this.conditionIn = conditionIn;
            this.damageNotes = damageNotes;
        }
    }
}