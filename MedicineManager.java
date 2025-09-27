import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MedicineManager implements Serializable {
 private static final long serialVersionUID = 1L;
 private List<Medicine> medicines;

 public MedicineManager() {
 this.medicines = new ArrayList<>();
 }

 public void addMedicine(Medicine medicine) {
 this.medicines.add(medicine);
 System.out.println("Medicine added: " + medicine.getName());
 }

 public void removeMedicine(Medicine medicine) {
 this.medicines.remove(medicine);
 System.out.println("Medicine removed: " + medicine.getName());
 }

 public List<Medicine> getAllMedicines() {
 return new ArrayList<>(medicines);
 }

 public List<Medicine> getMedicinesSortedByTime() {
 List<Medicine> sortedList = new ArrayList<>(medicines);
 sortedList.sort(Comparator.comparing(Medicine::getTime));
 return sortedList;
 }

 public void saveMedicines(String filename) {
 try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
 oos.writeObject(medicines);
 System.out.println("Medicines saved to " + filename);
 } catch (IOException e) {
 System.err.println("Error saving medicines: " + e.getMessage());
 e.printStackTrace();
 }
 }

 public void loadMedicines(String filename) {
 File file = new File(filename);
 if (file.exists() && file.length() > 0) {
 try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
 this.medicines = (List<Medicine>) ois.readObject();
 System.out.println("Medicines loaded from " + filename);
 } catch (EOFException e) {
 System.out.println("Reached end of file unexpectedly - file might be empty or corrupted: " + e.getMessage());
 } catch (IOException | ClassNotFoundException e) {
 System.err.println("Error loading medicines: " + e.getMessage());
 e.printStackTrace();
 }
 } else {
 System.out.println("Data file does not exist or is empty. Starting with no medicines.");
 this.medicines = new ArrayList<>();
 }
 }

 public Medicine getNextUpcomingMedicine() {
 LocalTime now = LocalTime.now();
 LocalDate today = LocalDate.now();
 Medicine nextMedicine = null;
 LocalTime nextAlarmTime = null;

 List<Medicine> relevantMedicines = new ArrayList<>();
 for (Medicine m : medicines) {
 boolean isDailyAlarm = (m.getFrequency() == AlarmFrequency.DAILY);
 boolean isSingleAlarmOnCorrectDate = (m.getFrequency() == AlarmFrequency.ONCE && m.getSingleAlarmDate() != null && m.getSingleAlarmDate().isEqual(today));

 if (isDailyAlarm || isSingleAlarmOnCorrectDate) {
 relevantMedicines.add(m);
 }
 }

 for (Medicine m : relevantMedicines) {
 LocalTime alarmTime = m.getTime();
 if (alarmTime.isAfter(now)) {
 if (nextMedicine == null || alarmTime.isBefore(nextAlarmTime)) {
 nextMedicine = m;
 nextAlarmTime = alarmTime;
 }
 }
 }
 return nextMedicine;
 }

 public static void main(String[] args) {
 MedicineManager manager = new MedicineManager();

 manager.addMedicine(new Medicine("Paracetamol", 1.0, LocalTime.of(9, 0), "After breakfast", AlarmFrequency.DAILY, null));
 manager.addMedicine(new Medicine("Insulin", 50.0, LocalTime.of(12, 30), "Before lunch", AlarmFrequency.DAILY, null));
 manager.addMedicine(new Medicine("Vitamin D", 1.0, LocalTime.of(18, 0), "After dinner", AlarmFrequency.ONCE, LocalDate.now().plusDays(1)));

 System.out.println("\nAll Medicines:");
 manager.getAllMedicines().forEach(System.out::println);

 System.out.println("\nNext upcoming medicine:");
 Medicine next = manager.getNextUpcomingMedicine();
 if (next != null) {
 System.out.println(next);
 } else {
 System.out.println("No upcoming medicines today.");
 }

 manager.saveMedicines("test_medicines.dat");

 MedicineManager loadedManager = new MedicineManager();
 loadedManager.loadMedicines("test_medicines.dat");
 System.out.println("\nLoaded Medicines:");
 loadedManager.getAllMedicines().forEach(System.out::println);
 }
}
