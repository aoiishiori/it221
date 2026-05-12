package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface PersonInter {
    String getPersonType(String personId);
    String getPersonName(String personId);
    String[] getCredentials(String username);
    List<DataClass.PersonSummary> getPersonBorrowSummary();
    List<DataClass.StudentRecord> getAllStudents();
    List<DataClass.StudentRecord> searchStudents(String keyword);
    List<DataClass.StudentRecord> getStudentsByClassId(String classId);
    List<DataClass.StaffFacultyRecord> getAllStaffFaculty();
    List<DataClass.StaffFacultyRecord> getStaffFacultyByType(String type);
    List<DataClass.StaffFacultyRecord> searchStaffFaculty(String keyword);
}