package models; // Updated package

import java.util.List;
import java.util.Map;

public class Report {
    private String reportTitle;
    private Map<String, String> filtersUsed;
    private List<ReportRow> reportData;

    // Constructor
    public Report(String reportTitle, Map<String, String> filtersUsed, List<ReportRow> reportData) {
        this.reportTitle = reportTitle;
        this.filtersUsed = filtersUsed;
        this.reportData = reportData;
    }

    // Getters
    public String getReportTitle() { return reportTitle; }
    public Map<String, String> getFiltersUsed() { return filtersUsed; }
    public List<ReportRow> getReportData() { return reportData; }

    // Inner class or separate class for report rows
    public static class ReportRow {
        private String applicantName;
        private String applicantNric;
        private int applicantAge;
        private String applicantMaritalStatus;
        private String flatTypeBooked;
        private String projectName;

        public ReportRow(String applicantName, String applicantNric, int applicantAge, String applicantMaritalStatus, String flatTypeBooked, String projectName) {
            this.applicantName = applicantName;
            this.applicantNric = applicantNric;
            this.applicantAge = applicantAge;
            this.applicantMaritalStatus = applicantMaritalStatus;
            this.flatTypeBooked = flatTypeBooked;
            this.projectName = projectName;
        }

        // Getters for ReportRow fields
        public String getApplicantName() { return applicantName; }
        public String getApplicantNric() { return applicantNric; }
        public int getApplicantAge() { return applicantAge; }
        public String getApplicantMaritalStatus() { return applicantMaritalStatus; }
        public String getFlatTypeBooked() { return flatTypeBooked; }
        public String getProjectName() { return projectName; }

        @Override
        public String toString() {
            return String.format("Name: %-20s | NRIC: %s | Age: %d | Status: %-8s | Flat: %-8s | Project: %s",
                                 applicantName, applicantNric, applicantAge, applicantMaritalStatus, flatTypeBooked, projectName);
        }
    }

    // Method to display the report
    public void display() {
        System.out.println("\n==================================================");
        System.out.println("      " + reportTitle);
        System.out.println("==================================================");
        System.out.println("Filters Applied: " + (filtersUsed.isEmpty() ? "None" : filtersUsed));
        System.out.println("--------------------------------------------------");
        if (reportData == null || reportData.isEmpty()) {
            System.out.println("No records found matching the criteria.");
        } else {
            // Example Header - adjust spacing as needed
             System.out.println(String.format("%-20s | %-9s | %-3s | %-8s | %-8s | %s",
                                 "Applicant Name", "NRIC", "Age", "Marital", "FlatType", "Project Name"));
             System.out.println("-".repeat(100)); // Separator
            for (ReportRow row : reportData) {
                System.out.println(row.toString());
            }
        }
        System.out.println("================== End of Report ==================\n");
    }
}