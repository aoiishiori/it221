package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface EquipmentInter {
    List<DataClass.Equipment> getAllEquipment();
    List<DataClass.Equipment> getEquipmentByStatus(String status);
    List<DataClass.Equipment> getEquipmentByCategory(String category);
    List<DataClass.Equipment> searchEquipment(String keyword);

    boolean updateEquipmentStatus(String barcode, String newStatus);
}