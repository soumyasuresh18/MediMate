import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.sound.sampled.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.util.Locale;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.table.DefaultTableCellRenderer;

public class MedicineGUI extends JFrame {

    private static final String DATA_FILE = "medimates.dat";
    private List<Medicine> medicines;
    private DefaultTableModel tableModel;
    private JTable medicineTable;
    private JLabel upcomingMedicineLabel;
    private JLabel headerLabel;
    private BackgroundPanel backgroundPanel;

    private Clip alarmClip;
    private Timer updateTimer;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> acknowledgedToday = new HashSet<>();

    public MedicineGUI() {

        setTitle("MediMate - Your Personal Medicine Reminder");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load background image
        backgroundPanel = new BackgroundPanel("image_c26221.png");
        backgroundPanel.setOpacity(0.8f);

        // Set the background panel as the content pane
        setContentPane(backgroundPanel);
        backgroundPanel.setLayout(new BorderLayout());

        // Header Panel (at the very top)
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        headerLabel = new JLabel("MediMate - Your Personal Medicine Reminder");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 48));
        headerLabel.setForeground(new Color(20, 80, 100));
        headerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageIcon appIcon = new ImageIcon("icon.jpg");
        Image iconImage = appIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(iconImage));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(Box.createVerticalStrut(20));
        topPanel.add(iconLabel);
        topPanel.add(headerLabel);
        topPanel.add(Box.createVerticalStrut(20));

        backgroundPanel.add(topPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel mainContentPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainContentPanel.setOpaque(false);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Left Panel: Medicine Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0, 77, 77), 2),
                "Scheduled Medicines", 0, 0, new Font("Arial", Font.BOLD, 18), new Color(20, 80, 100)));

        String[] columnNames = {"Name", "Time", "Qty", "Instructions", "Freq", "Start Date", "Recur Days", "Single Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    medicineTable = new JTable(tableModel);
medicineTable.setFont(new Font("Arial", Font.PLAIN, 14));
medicineTable.setRowHeight(25);
medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

// Header styling
medicineTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
medicineTable.getTableHeader().setBackground(new Color(205, 237, 224));
medicineTable.getTableHeader().setForeground(new Color(0, 51, 51));

// Alternate row colors
medicineTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
            if (row % 2 == 0)
                c.setBackground(new Color(245, 252, 249));
            else
                c.setBackground(new Color(232, 246, 240));
        } else {
            c.setBackground(new Color(180, 220, 210));
        }

        c.setForeground(Color.BLACK);
        return c;
    }
});



        JScrollPane tableScrollPane = new JScrollPane(medicineTable);
        tableScrollPane.setOpaque(false);
        tableScrollPane.getViewport().setOpaque(false);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Right Panel: Upcoming Medicine
        JPanel upcomingPanel = new JPanel(new BorderLayout());
        upcomingPanel.setOpaque(false);
        upcomingPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0, 77, 77), 2),
                "Upcoming Medicine", 0, 0, new Font("Arial", Font.BOLD, 18), new Color(20, 80, 100)));

        upcomingMedicineLabel = new JLabel();
        upcomingMedicineLabel.setVerticalAlignment(JLabel.TOP);
        upcomingMedicineLabel.setFont(new Font("Arial", Font.ITALIC, 16));
    
        upcomingMedicineLabel.setForeground(new Color(0, 51, 102));
        upcomingMedicineLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        upcomingMedicineLabel.setText("<html><div style='text-align: center;'><br><br><br><br><br><br>✔ No medicines scheduled<br>in the next few minutes.</div></html>");

        JScrollPane upcomingScrollPane = new JScrollPane(upcomingMedicineLabel);
        upcomingScrollPane.setOpaque(false);
        upcomingScrollPane.getViewport().setOpaque(false);
        upcomingPanel.add(upcomingScrollPane, BorderLayout.CENTER);

        mainContentPanel.add(tablePanel);
        mainContentPanel.add(upcomingPanel);
        backgroundPanel.add(mainContentPanel, BorderLayout.CENTER);

        // Bottom Panel: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        Color darkTeal = new Color(0, 77, 77); // Consistent with your theme

JButton addMedicineBtn = new JButton("Add Medicine");
addMedicineBtn.setBackground(darkTeal);
addMedicineBtn.setForeground(Color.WHITE);
addMedicineBtn.setFont(new Font("Arial", Font.BOLD, 16));
addMedicineBtn.setFocusPainted(false);
addMedicineBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
addMedicineBtn.setPreferredSize(new Dimension(160, 45));

JButton deleteMedicineBtn = new JButton("Delete Selected");
deleteMedicineBtn.setBackground(darkTeal);
deleteMedicineBtn.setForeground(Color.WHITE);
deleteMedicineBtn.setFont(new Font("Arial", Font.BOLD, 16));
deleteMedicineBtn.setFocusPainted(false);
deleteMedicineBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
deleteMedicineBtn.setPreferredSize(new Dimension(160, 45));

        addMedicineBtn.addActionListener(e -> addMedicine());
        deleteMedicineBtn.addActionListener(e -> deleteSelectedMedicine());

        buttonPanel.add(addMedicineBtn);
        buttonPanel.add(deleteMedicineBtn);
        backgroundPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Load medicines and start timers
        medicines = loadMedicines();
        updateMedicineTable();
        updateUpcomingMedicine();
        startUpdateTimer();
        startAlarmScheduler();
        setupDailyAlarmReset();
    }
    private void acknowledgeMedicine(Medicine medicine, LocalDate date) {
    String key = medicine.getName() + "_" +
                 medicine.getTime().format(DateTimeFormatter.ofPattern("HHmm")) + "_" +
                 date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    acknowledgedToday.add(key);
}

    private void customizeButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(new Color(150, 200, 220));
        button.setForeground(new Color(20, 80, 100));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(100, 150, 180), 2));
        button.setPreferredSize(new Dimension(150, 40));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(170, 220, 240));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(150, 200, 220));
            }
        });
    }

    private void addMedicine() {
        JTextField nameField = new JTextField(20);
        JTextField quantityField = new JTextField(10);
        JTextField timeField = new JTextField(10);
        JTextField instructionsField = new JTextField(25);

        JRadioButton dailyAlarmBtn = new JRadioButton("Daily Alarm (Recurrence)");
        JRadioButton onceAlarmBtn = new JRadioButton("One Time Alarm");
        ButtonGroup alarmTypeGroup = new ButtonGroup();
        alarmTypeGroup.add(dailyAlarmBtn);
        alarmTypeGroup.add(onceAlarmBtn);
        dailyAlarmBtn.setSelected(true);

        JTextField singleAlarmDateField = new JTextField(10);
        singleAlarmDateField.setToolTipText("YYYY-MM-DD (e.g., 2025-12-31)");
        singleAlarmDateField.setEnabled(false);

        JTextField startDateField = new JTextField(10);
        startDateField.setToolTipText("YYYY-MM-DD (e.g., 2025-07-01)");
        startDateField.setEnabled(true);

        JTextField recurrenceDaysField = new JTextField(5);
        recurrenceDaysField.setToolTipText("e.g., 1 for daily, 7 for weekly");
        recurrenceDaysField.setText("1");
        recurrenceDaysField.setEnabled(true);

        dailyAlarmBtn.addActionListener(e -> {
            singleAlarmDateField.setEnabled(false);
            startDateField.setEnabled(true);
            recurrenceDaysField.setEnabled(true);
        });
        onceAlarmBtn.addActionListener(e -> {
            singleAlarmDateField.setEnabled(true);
            startDateField.setEnabled(false);
            recurrenceDaysField.setEnabled(false);
        });

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        inputPanel.add(new JLabel("Medicine Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Quantity (e.g. 1.5):"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Timings (e.g., 09:00 PM or 09:00 AM):"));
        inputPanel.add(timeField);
        inputPanel.add(new JLabel("Instructions:"));
        inputPanel.add(instructionsField);
        inputPanel.add(new JLabel("Alarm Type:"));
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        radioPanel.add(dailyAlarmBtn);
        radioPanel.add(onceAlarmBtn);
        inputPanel.add(radioPanel);

        inputPanel.add(new JLabel("One-Time Alarm Date (YYYY-MM-DD):"));
        inputPanel.add(singleAlarmDateField);
        inputPanel.add(new JLabel("Recurrence Start Date (YYYY-MM-DD):"));
        inputPanel.add(startDateField);
        inputPanel.add(new JLabel("Repeat Every (days):"));
        inputPanel.add(recurrenceDaysField);


        int result = JOptionPane.showConfirmDialog(this, inputPanel, "Add New Medicine",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            double quantity;
            LocalTime time;
            String instructions = instructionsField.getText().trim();
            AlarmFrequency frequency = dailyAlarmBtn.isSelected() ? AlarmFrequency.DAILY : AlarmFrequency.ONCE;

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Medicine name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                quantity = Double.parseDouble(quantityField.getText().trim());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a number (e.g., 1 or 1.5).", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                time = parseTime(timeField.getText().trim());
            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (frequency == AlarmFrequency.ONCE) {
                String dateStr = singleAlarmDateField.getText().trim();
                if (dateStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a date for one-time alarms.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    LocalDate singleAlarmDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    Medicine newMedicine = new Medicine(name, quantity, time, instructions, frequency, singleAlarmDate);
                    medicines.add(newMedicine);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date format for one-time alarm. Please use YYYY-MM-DD (e.g., 2025-12-31).", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                String dateStr = startDateField.getText().trim();
                String recurrenceStr = recurrenceDaysField.getText().trim();
                if (dateStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a start date for recurring alarms.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    LocalDate startDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    if (startDate.isBefore(LocalDate.now())) {
                        JOptionPane.showMessageDialog(this, "Start date for recurring alarm cannot be in the past.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int recurrenceIntervalDays;
                    try {
                        recurrenceIntervalDays = Integer.parseInt(recurrenceStr);
                        if (recurrenceIntervalDays <= 0) {
                            JOptionPane.showMessageDialog(this, "Number of days to repeat must be a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid number of days to repeat. Please enter a positive integer.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Medicine newMedicine = new Medicine(name, quantity, time, instructions, frequency, startDate, recurrenceIntervalDays);
                    medicines.add(newMedicine);
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid start date format for recurring alarm. Please use YYYY-MM-DD (e.g., 2025-07-01).", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            saveMedicines(medicines);
            updateMedicineTable();
            updateUpcomingMedicine();
        }
    }

    private void deleteSelectedMedicine() {
        int selectedRow = medicineTable.getSelectedRow();
        if (selectedRow >= 0) {
            String name = (String) tableModel.getValueAt(selectedRow, 0);
            LocalTime time = parseTime((String) tableModel.getValueAt(selectedRow, 1));
            String freqStr = (String) tableModel.getValueAt(selectedRow, 4);
            AlarmFrequency frequency = AlarmFrequency.valueOf(freqStr);

            Medicine medicineToDelete = null;

            LocalDate tableSingleDate = null;
            if (frequency == AlarmFrequency.ONCE) {
                String singleDateStr = (String) tableModel.getValueAt(selectedRow, 7);
                if (!singleDateStr.equals("N/A") && !singleDateStr.isEmpty()) {
                    try {
                        tableSingleDate = LocalDate.parse(singleDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (DateTimeParseException e) { /* handle error */ }
                }
            }

            LocalDate tableStartDate = null;
            int tableRecurrenceDays = 0;
            if (frequency == AlarmFrequency.DAILY) {
                String startDateStr = (String) tableModel.getValueAt(selectedRow, 5);
                String recurDaysStr = (String) tableModel.getValueAt(selectedRow, 6);
                if (!startDateStr.equals("N/A") && !startDateStr.isEmpty()) {
                    try {
                        tableStartDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (DateTimeParseException e) { /* handle error */ }
                }
                if (!recurDaysStr.equals("N/A") && !recurDaysStr.isEmpty()) {
                    try {
                        tableRecurrenceDays = Integer.parseInt(recurDaysStr);
                    } catch (NumberFormatException e) { /* handle error */ }
                }
            }

            for (Medicine m : medicines) {
                boolean nameMatch = m.getName().equals(name);
                boolean timeMatch = m.getTime().equals(time);
                boolean freqMatch = m.getFrequency() == frequency;

                if (freqMatch) {
                    if (frequency == AlarmFrequency.ONCE) {
                        boolean dateMatch = Objects.equals(m.getSingleAlarmDate(), tableSingleDate);
                        if (nameMatch && timeMatch && dateMatch) {
                            medicineToDelete = m;
                            break;
                        }
                    } else if (frequency == AlarmFrequency.DAILY) {
                        boolean startDateMatch = Objects.equals(m.getStartDate(), tableStartDate);
                        boolean recurDaysMatch = m.getRecurrenceIntervalDays() == tableRecurrenceDays;
                        if (nameMatch && timeMatch && startDateMatch && recurDaysMatch) {
                            medicineToDelete = m;
                            break;
                        }
                    }
                }
            }

            if (medicineToDelete != null) {
                medicines.remove(medicineToDelete);
                saveMedicines(medicines);
                updateMedicineTable();
                updateUpcomingMedicine();
            } else {
                JOptionPane.showMessageDialog(this, "Could not find selected medicine to delete.", "Error", JOptionPane.ERROR_MESSAGE);
            }

        } else {
            JOptionPane.showMessageDialog(this, "Please select a medicine to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private LocalTime parseTime(String timeString) throws DateTimeParseException {
        timeString = timeString.replaceAll("\\s+", " ").trim();

        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("HH:mm")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(timeString.toUpperCase(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DateTimeParseException("Invalid time format. Please use hh:mm AM/PM (e.g., 08:30 PM or 8:30 AM) or HH:mm (e.g., 14:30).", timeString, 0);
    }

    private void updateMedicineTable() {
        tableModel.setRowCount(0);
        medicines.sort(Comparator.comparing(Medicine::getTime));

        for (Medicine m : medicines) {
            String startDateCol = "N/A";
            String recurDaysCol = "N/A";
            String singleDateCol = "N/A";

            if (m.getFrequency() == AlarmFrequency.DAILY) {
                if (m.getStartDate() != null) {
                    startDateCol = m.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                recurDaysCol = String.valueOf(m.getRecurrenceIntervalDays());
            } else if (m.getFrequency() == AlarmFrequency.ONCE) {
                if (m.getSingleAlarmDate() != null) {
                    singleDateCol = m.getSingleAlarmDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
            }

            tableModel.addRow(new Object[]{
                m.getName(),
                m.getTime().format(DateTimeFormatter.ofPattern("hh:mm a")),
                m.getQuantity(),
                m.getDescription(),
                m.getFrequency().name(),
                startDateCol,
                recurDaysCol,
                singleDateCol
            });
        }
    }

    private void updateUpcomingMedicine() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        StringBuilder upcomingText = new StringBuilder();
        upcomingText.append("<html><div style='text-align: center;'>");

        List<Medicine> relevantMedicines = new ArrayList<>();
        for (Medicine m : medicines) {
            boolean isDueToday = false;
            if (m.getFrequency() == AlarmFrequency.ONCE && m.getSingleAlarmDate() != null && m.getSingleAlarmDate().isEqual(today)) {
                isDueToday = true;
            } else if (m.getFrequency() == AlarmFrequency.DAILY && m.getStartDate() != null) {
                if (!today.isBefore(m.getStartDate())) {
                    long daysSinceStart = ChronoUnit.DAYS.between(m.getStartDate(), today);
                    if (m.getRecurrenceIntervalDays() > 0 && (daysSinceStart % m.getRecurrenceIntervalDays() == 0)) {
                        isDueToday = true;
                    }
                }
            }

            if (isDueToday) {
    // Skip already acknowledged
    String ackKey = m.getName() + "_" +
                    m.getTime().format(DateTimeFormatter.ofPattern("HHmm")) + "_" +
                    today.format(DateTimeFormatter.ISO_LOCAL_DATE);
    if (!acknowledgedToday.contains(ackKey)) {
        relevantMedicines.add(m);
    }
}

        }

        if (relevantMedicines.isEmpty()) {
            upcomingText.append("<br><br><br><br><br><br>✔ No medicines scheduled<br>in the next few minutes.");
        } else {
            relevantMedicines.sort(Comparator.comparing(Medicine::getTime));

            boolean foundUpcoming = false;
            for (Medicine upcoming : relevantMedicines) {
                LocalTime alarmTime = upcoming.getTime();

                // Check if due within the next 60 minutes or just passed within 5 minutes
                boolean isDueSoon = (alarmTime.isAfter(now) && alarmTime.isBefore(now.plusMinutes(60))) ||
                                     (alarmTime.isBefore(now) && alarmTime.isAfter(now.minusMinutes(5)));

                if (isDueSoon) {
                    upcomingText.append("<div style='margin-bottom: 15px;'>");
                    upcomingText.append("<span style='font-size: 18px; font-weight: bold;'>").append(upcoming.getName()).append("</span><br>");
                    upcomingText.append("Quantity: ").append(upcoming.getQuantity()).append("<br>");
                    upcomingText.append("Time: ").append(upcoming.getTime().format(DateTimeFormatter.ofPattern("hh:mm a"))).append("<br>");

                    if (upcoming.getFrequency() == AlarmFrequency.ONCE && upcoming.getSingleAlarmDate() != null) {
                        upcomingText.append("Date: ").append(upcoming.getSingleAlarmDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("<br>");
                    } else if (upcoming.getFrequency() == AlarmFrequency.DAILY && upcoming.getStartDate() != null) {
                        upcomingText.append("Starts: ").append(upcoming.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("<br>");
                        upcomingText.append("Repeats Every: ").append(upcoming.getRecurrenceIntervalDays()).append(" days<br>");
                    }
                    upcomingText.append("Instructions: ").append(upcoming.getDescription()).append("</div>");
                    foundUpcoming = true;
                }
            }
            if (!foundUpcoming) {
                upcomingText.append("<br><br><br><br><br><br>✔ No medicines scheduled<br>in the next few minutes.");
            }
        }
        upcomingText.append("</div></html>");
        upcomingMedicineLabel.setText(upcomingText.toString());
    }

    private void startUpdateTimer() {
        updateTimer = new Timer(true);
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateUpcomingMedicine());
            }
        }, 0, 30 * 1000);
    }

    private void startAlarmScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            checkReminders();
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void checkReminders() {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();

        List<Medicine> currentMedicines = loadMedicines();

        for (Medicine medicine : currentMedicines) {
            LocalTime alarmTime = medicine.getTime();

            boolean isAlarmActiveToday = false;
            if (medicine.getFrequency() == AlarmFrequency.ONCE && medicine.getSingleAlarmDate() != null) {
                if (medicine.getSingleAlarmDate().isEqual(today)) {
                    isAlarmActiveToday = true;
                }
            } else if (medicine.getFrequency() == AlarmFrequency.DAILY && medicine.getStartDate() != null) {
                if (!today.isBefore(medicine.getStartDate())) {
                    long daysSinceStart = ChronoUnit.DAYS.between(medicine.getStartDate(), today);
                    if (medicine.getRecurrenceIntervalDays() > 0 && (daysSinceStart % medicine.getRecurrenceIntervalDays() == 0)) {
                        isAlarmActiveToday = true;
                    }
                }
            }

            if (isAlarmActiveToday &&
                (alarmTime.getHour() == now.getHour() && alarmTime.getMinute() == now.getMinute())) {
                if (!hasAlarmFired(medicine, today)) {
                    SwingUtilities.invokeLater(() -> showReminderDialog(medicine));
                    markAlarmFired(medicine, today);
                }
            }
        }
    }

    private List<String> firedAlarms = new ArrayList<>();

    private boolean hasAlarmFired(Medicine medicine, LocalDate date) {
        String uniqueId = medicine.getName() + "_" +
                          medicine.getTime().format(DateTimeFormatter.ofPattern("HHmm")) + "_" +
                          medicine.getFrequency().name();

        if (medicine.getFrequency() == AlarmFrequency.ONCE) {
            uniqueId += "_" + (medicine.getSingleAlarmDate() != null ? medicine.getSingleAlarmDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        } else if (medicine.getFrequency() == AlarmFrequency.DAILY) {
            uniqueId += "_" + (medicine.getStartDate() != null ? medicine.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "") +
                        "_" + medicine.getRecurrenceIntervalDays();
        }

        return firedAlarms.contains(uniqueId + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private void markAlarmFired(Medicine medicine, LocalDate date) {
        String uniqueId = medicine.getName() + "_" +
                          medicine.getTime().format(DateTimeFormatter.ofPattern("HHmm")) + "_" +
                          medicine.getFrequency().name();

        if (medicine.getFrequency() == AlarmFrequency.ONCE) {
            uniqueId += "_" + (medicine.getSingleAlarmDate() != null ? medicine.getSingleAlarmDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "");
        } else if (medicine.getFrequency() == AlarmFrequency.DAILY) {
            uniqueId += "_" + (medicine.getStartDate() != null ? medicine.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : "") +
                        "_" + medicine.getRecurrenceIntervalDays();
        }

        firedAlarms.add(uniqueId + "_" + date.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private void setupDailyAlarmReset() {
        Timer dailyResetTimer = new Timer(true);
        dailyResetTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDate today = LocalDate.now();
                List<String> toRemove = new ArrayList<>();
                for (String firedId : firedAlarms) {
                    String[] parts = firedId.split("_");
                    if (parts.length >= 4) {
                        String dateFiredStr = parts[parts.length - 1];
                        try {
                            LocalDate firedDate = LocalDate.parse(dateFiredStr, DateTimeFormatter.ISO_LOCAL_DATE);
                            if (firedDate.isBefore(today)) {
                                toRemove.add(firedId);
                            }
                        } catch (DateTimeParseException e) {
                            System.err.println("Could not parse date from fired alarm ID for reset: " + dateFiredStr);
                        }
                    }
                }
                firedAlarms.removeAll(toRemove);
                acknowledgedToday.clear();
System.out.println("Acknowledged medicine list cleared for new day.");

                System.out.println("Fired alarm status reset for past dates.");
            }
        }, getTomorrowMidnightDelay(), TimeUnit.DAYS.toMillis(1));
    }

    private long getTomorrowMidnightDelay() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long tomorrowMidnightMillis = tomorrow.atTime(midnight).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        return tomorrowMidnightMillis - nowMillis;
    }

    private void showReminderDialog(Medicine medicine) {
        ReminderDialog dialog = new ReminderDialog(this, medicine, this);
        dialog.setVisible(true);
    }

    public void playSound(String soundFile) {
        try {
            File file = new File(soundFile);
            if (!file.exists()) {
                URL resourceUrl = getClass().getClassLoader().getResource(soundFile);
                if (resourceUrl != null) {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(resourceUrl);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clip.start();
                    this.alarmClip = clip;
                    return;
                } else {
                    System.err.println("Sound file not found: " + soundFile + " (neither as file nor resource)");
                    return;
                }
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            this.alarmClip = clip;
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopAlarm() {
        if (this.alarmClip != null && this.alarmClip.isRunning()) {
            this.alarmClip.stop();
            this.alarmClip.close();
            this.alarmClip = null;
        }
    }

    // Nested ReminderDialog class
    private class ReminderDialog extends JDialog {
        private Medicine medicine;
        private MedicineGUI parentGUI;

        public ReminderDialog(JFrame parent, Medicine med, MedicineGUI gui) {
            super(parent, "Time to Take Medicine!", true);
            this.medicine = med;
            this.parentGUI = gui;

            setResizable(false);
            setUndecorated(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            initUI();
            pack();
            setLocationRelativeTo(parent);

            gui.playSound("alert.wav");

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    parentGUI.stopAlarm();
                    parentGUI.updateMedicineTable();
                    parentGUI.updateUpcomingMedicine();
                }
            });
        }

        private void initUI() {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JTextArea reminderText = new JTextArea();
            reminderText.setEditable(false);
            reminderText.setLineWrap(true);
            reminderText.setWrapStyleWord(true);
            reminderText.setFont(new Font("Segoe UI", Font.BOLD, 16));
            reminderText.setBackground(Color.WHITE);
            reminderText.setPreferredSize(new Dimension(400, reminderText.getPreferredSize().height));

            // --- THIS IS THE CRITICAL SECTION FOR THE POP-UP MESSAGE ---
            StringBuilder text = new StringBuilder();
text.append("Time to Take Your Medicine!\n\n");
text.append("~ Medicine: ").append(medicine.getName()).append("\n");
text.append("~ Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
text.append("~ Time: ").append(medicine.getTime().format(DateTimeFormatter.ofPattern("h:mm a"))).append("\n");
text.append("~ Instructions: ").append(medicine.getDescription()).append("\n");


System.out.println("Medicine Reminder Triggered for: " + medicine.getName());
System.out.println("Quantity: " + medicine.getQuantity());
System.out.println("Time: " + medicine.getTime());
System.out.println("Instructions: " + medicine.getDescription());

            

            reminderText.setText(text.toString());

            JScrollPane scrollPane = new JScrollPane(reminderText);
scrollPane.setPreferredSize(new Dimension(400, 200)); // Or calculate dynamically
panel.add(scrollPane, BorderLayout.CENTER);

            JButton okButton = new JButton("OK, Got it!");
            okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            okButton.setBackground(new Color(0, 102, 153));
            okButton.setForeground(Color.WHITE);

            okButton.addActionListener(e -> {
    parentGUI.stopAlarm();
    parentGUI.acknowledgeMedicine(medicine, LocalDate.now()); // ✅ Track it
    dispose();
    parentGUI.updateMedicineTable();
    parentGUI.updateUpcomingMedicine(); // ✅ Immediately refresh UI
});


            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(Color.WHITE);
            buttonPanel.add(okButton);

            panel.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(panel);
        }
    }

    private List<Medicine> loadMedicines() {
        List<Medicine> loadedMedicines = new ArrayList<>();
        File file = new File(DATA_FILE);
        if (file.exists() && file.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                loadedMedicines = (List<Medicine>) ois.readObject();
                System.out.println("Medicines loaded successfully.");
            } catch (EOFException e) {
                System.out.println("Reached end of file unexpectedly - file might be empty or corrupted: " + e.getMessage());
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading medicines: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Data file does not exist or is empty. Starting with no medicines.");
        }
        return loadedMedicines;
    }

    private void saveMedicines(List<Medicine> medicinesToSave) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(medicinesToSave);
            System.out.println("Medicines saved to " + DATA_FILE);
        } catch (IOException e) {
            System.err.println("Error saving medicines: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MedicineGUI gui = new MedicineGUI();
            gui.setVisible(true);
        });
    }
}