package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface LabClassInter {
    List<DataClass.LabClassRecord> getAllClasses();
    String getClassInfo(String classId);
    List<DataClass.LabClassRecord> searchClasses(String keyword, String searchType);
}