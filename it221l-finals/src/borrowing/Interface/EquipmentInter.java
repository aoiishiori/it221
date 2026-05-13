package borrowing.Interface;

import borrowing.model.DataClass;

import java.util.List;

public interface EquipmentInter {
    List<DataClass.Equipment> getAllEquipment();
    List<DataClass.Equipment> getAvailableEquipment();          // FIX: for borrow picker
    List<DataClass.Equipment> getEquipmentByStatus(String status);
    List<DataClass.Equipment> getEquipmentByCategory(String category);
    List<DataClass.Equipment> searchEquipment(String keyword);
    List<DataClass.Equipment> getEquipmentByExactBarcode(String barcode); // FIX: exact match

    boolean updateEquipmentStatus(String barcode, String newStatus);
}