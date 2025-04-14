package models; // Updated package

import enums.FlatType; // Updated import
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);

    private int projectId;
    private String projectName;
    private String neighborhood;
    private Map<FlatType, Integer> totalUnits;
    private Map<FlatType, Integer> availableUnits;
    private Date applicationOpeningDate;
    private Date applicationClosingDate;
    private String assignedHDBManagerNric;
    private List<String> assignedHDBOfficerNrics;
    private int maxOfficerSlots;
    private boolean visibility;

    // Constructor for creating a new project
    public Project(String projectName, String neighborhood, Map<FlatType, Integer> totalUnits,
                   Date applicationOpeningDate, Date applicationClosingDate,
                   String assignedHDBManagerNric, int maxOfficerSlots) {
        this.projectId = idCounter.incrementAndGet();
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.totalUnits = new HashMap<>(totalUnits);
        this.availableUnits = new HashMap<>(totalUnits);
        this.applicationOpeningDate = applicationOpeningDate;
        this.applicationClosingDate = applicationClosingDate;
        this.assignedHDBManagerNric = assignedHDBManagerNric;
        this.maxOfficerSlots = maxOfficerSlots;
        this.assignedHDBOfficerNrics = new ArrayList<>();
        this.visibility = false;
    }

     // Constructor for loading existing project
    public Project(int projectId, String projectName, String neighborhood, Map<FlatType, Integer> totalUnits, Map<FlatType, Integer> availableUnits,
                   Date applicationOpeningDate, Date applicationClosingDate,
                   String assignedHDBManagerNric, List<String> assignedHDBOfficerNrics, int maxOfficerSlots, boolean visibility) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.neighborhood = neighborhood;
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
        this.applicationOpeningDate = applicationOpeningDate;
        this.applicationClosingDate = applicationClosingDate;
        this.assignedHDBManagerNric = assignedHDBManagerNric;
        this.assignedHDBOfficerNrics = assignedHDBOfficerNrics;
        this.maxOfficerSlots = maxOfficerSlots;
        this.visibility = visibility;
         if (projectId >= idCounter.get()) {
             idCounter.set(projectId + 1);
         }
    }

    // --- Getters ---
    public int getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getNeighborhood() { return neighborhood; }
    public Map<FlatType, Integer> getTotalUnits() { return new HashMap<>(totalUnits); }
    public Map<FlatType, Integer> getAvailableUnits() { return new HashMap<>(availableUnits); }
    public int getAvailableUnits(FlatType type) { return availableUnits.getOrDefault(type, 0); }
    public Date getApplicationOpeningDate() { return applicationOpeningDate; }
    public Date getApplicationClosingDate() { return applicationClosingDate; }
    public String getAssignedHDBManagerNric() { return assignedHDBManagerNric; }
    public List<String> getAssignedHDBOfficerNrics() { return new ArrayList<>(assignedHDBOfficerNrics); }
    public int getMaxOfficerSlots() { return maxOfficerSlots; }
    public boolean isVisible() { return visibility; }
    public int getCurrentOfficerCount() { return assignedHDBOfficerNrics.size(); }
     public int getRemainingOfficerSlots() { return maxOfficerSlots - getCurrentOfficerCount(); }

    // --- Setters ---
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public void setTotalUnits(Map<FlatType, Integer> totalUnits) {
        this.totalUnits = new HashMap<>(totalUnits);
        this.availableUnits = new HashMap<>(totalUnits);
    }
    public void setAvailableUnits(Map<FlatType, Integer> availableUnits) {
        this.availableUnits = new HashMap<>(availableUnits);
    }
    public void setApplicationOpeningDate(Date applicationOpeningDate) { this.applicationOpeningDate = applicationOpeningDate; }
    public void setApplicationClosingDate(Date applicationClosingDate) { this.applicationClosingDate = applicationClosingDate; }
    public void setMaxOfficerSlots(int maxOfficerSlots) { this.maxOfficerSlots = maxOfficerSlots; }
    public void setVisibility(boolean visibility) { this.visibility = visibility; }

    // --- Business Logic Methods ---
    public boolean addOfficer(String officerNric) {
        if (assignedHDBOfficerNrics.size() < maxOfficerSlots && !assignedHDBOfficerNrics.contains(officerNric)) {
            assignedHDBOfficerNrics.add(officerNric);
            return true;
        }
        return false;
    }
    public boolean removeOfficer(String officerNric) {
        return assignedHDBOfficerNrics.remove(officerNric);
    }
    public boolean decrementAvailableUnits(FlatType type) {
        int currentAvailable = availableUnits.getOrDefault(type, 0);
        if (currentAvailable > 0) {
            availableUnits.put(type, currentAvailable - 1);
            return true;
        }
        return false;
    }
    public void incrementAvailableUnits(FlatType type) {
        int currentAvailable = availableUnits.getOrDefault(type, 0);
        int total = totalUnits.getOrDefault(type, 0);
        if (currentAvailable < total) {
             availableUnits.put(type, currentAvailable + 1);
        } else {
            System.err.println("Warning: Attempted to increment available units for " + type +
                               " beyond total units for project " + projectId);
            availableUnits.put(type, total);
        }
    }
    public boolean isWithinApplicationPeriod(Date currentDate) {
        if (applicationOpeningDate == null || applicationClosingDate == null) {
            return false;
        }
        return !currentDate.before(applicationOpeningDate) && !currentDate.after(applicationClosingDate);
    }

    @Override
    public String toString() {
        return String.format("Project ID: %d, Name: %s, Neighborhood: %s, Visible: %b",
                             projectId, projectName, neighborhood, visibility);
    }
    public static void resetIdCounter() { idCounter.set(0); }
    public static void updateIdCounter(int maxId) { if (maxId >= idCounter.get()) idCounter.set(maxId + 1); }
}