package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface ActivityRequestInter {
    List<DataClass.ActivityRecord> getAllActivities();
    int addActivity(String activityName, String activityType, String activityDate, String location, String requestedBy, String notes); // Step 2
    boolean updateActivityStatus(int requestId, String newStatus, String approvedBy); // Step 3
}