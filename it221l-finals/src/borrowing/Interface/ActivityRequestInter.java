package borrowing.Interface;

import borrowing.model.DataClass;
import java.util.List;

public interface ActivityRequestInter {
    List<DataClass.ActivityRecord> getAllActivities();
    String  getActivitySummary();                                              // plain Statement
    int     addActivity(String activityName, String activityType,
                        String activityDate, String location,
                        String requestedBy, String notes);
    boolean updateActivityStatus(int requestId, String newStatus, String approvedBy);
}