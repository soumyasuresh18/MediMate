import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.List; // Keep this import

public class Medicine implements Serializable {
    private static final long serialVersionUID = 1L; // For serialization

    private String name;
    private double quantity;
    private LocalTime time;
    private String description;
    private AlarmFrequency frequency;
    private LocalDate singleAlarmDate;      // Used only for ONCE frequency
    private LocalDate startDate;            // Used only for DAILY frequency (start of recurrence)
    private int recurrenceIntervalDays;     // Used only for DAILY frequency (e.g., 1 for daily, 2 for every other day)

    // Constructor for ONCE frequency
    public Medicine(String name, double quantity, LocalTime time, String description, AlarmFrequency frequency, LocalDate singleAlarmDate) {
        this.name = name;
        this.quantity = quantity;
        this.time = time;
        this.description = description;
        this.frequency = frequency;
        this.singleAlarmDate = singleAlarmDate;
        // For ONCE, recurrence fields are not applicable
        this.startDate = null;
        this.recurrenceIntervalDays = 0;
    }

    // Constructor for DAILY frequency with recurrence
    public Medicine(String name, double quantity, LocalTime time, String description, AlarmFrequency frequency, LocalDate startDate, int recurrenceIntervalDays) {
        this.name = name;
        this.quantity = quantity;
        this.time = time;
        this.description = description;
        this.frequency = frequency;
        // For DAILY, singleAlarmDate is not applicable
        this.singleAlarmDate = null;
        this.startDate = startDate;
        this.recurrenceIntervalDays = recurrenceIntervalDays;
    }

    // Getters
    private List<LocalDate> multipleAlarmDates;  // For multiple alarm dates

public List<LocalDate> getMultipleAlarmDates() {
    return multipleAlarmDates;
}

public void setMultipleAlarmDates(List<LocalDate> multipleAlarmDates) {
    this.multipleAlarmDates = multipleAlarmDates;
}

    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public AlarmFrequency getFrequency() {
        return frequency;
    }

    public LocalDate getSingleAlarmDate() {
        return singleAlarmDate;
    }

    public LocalDate getStartDate() { // New getter
        return startDate;
    }

    public int getRecurrenceIntervalDays() { // New getter
        return recurrenceIntervalDays;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Medicine{");
        sb.append("name='").append(name).append('\'');
        sb.append(", quantity=").append(quantity);
        sb.append(", time=").append(time.format(DateTimeFormatter.ofPattern("hh:mm a")));
        sb.append(", description='").append(description).append('\'');
        sb.append(", frequency=").append(frequency);

        if (frequency == AlarmFrequency.ONCE && singleAlarmDate != null) {
            sb.append(", Date: ").append(singleAlarmDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        } else if (frequency == AlarmFrequency.DAILY && startDate != null) {
            sb.append(", Starts: ").append(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
            sb.append(", Repeats Every: ").append(recurrenceIntervalDays).append(" days");
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medicine medicine = (Medicine) o;
        return Double.compare(medicine.quantity, quantity) == 0 &&
               recurrenceIntervalDays == medicine.recurrenceIntervalDays && // Include recurrence interval
               Objects.equals(name, medicine.name) &&
               Objects.equals(time, medicine.time) &&
               Objects.equals(description, medicine.description) &&
               frequency == medicine.frequency &&
               Objects.equals(singleAlarmDate, medicine.singleAlarmDate) && // For ONCE
               Objects.equals(startDate, medicine.startDate);               // For DAILY (start date)
    }

    @Override
    public int hashCode() {
        // Include new fields in hash code calculation
        return Objects.hash(name, quantity, time, description, frequency, singleAlarmDate, startDate, recurrenceIntervalDays);
    }
}