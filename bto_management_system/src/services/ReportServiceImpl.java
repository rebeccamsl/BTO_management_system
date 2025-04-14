package services;

import interfaces.IReportService;
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil; // For potential warnings

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportServiceImpl implements IReportService {

    @Override
    public Report generateBookingReport(Map<String, String> filters) {
        String reportTitle = "Flat Booking Report";

        // 1. Get all successful bookings (applications with status BOOKED)
        List<BTOApplication> bookedApplications = DataStore.getApplications().values().stream()
                .filter(app -> app.getStatus() == BTOApplicationStatus.BOOKED)
                .collect(Collectors.toList());

        // 2. Prepare data rows by joining with User and Project info
        List<Report.ReportRow> reportRows = new ArrayList<>();
        for (BTOApplication app : bookedApplications) {
            User applicant = DataStore.getUserByNric(app.getApplicantNric());
            Project project = DataStore.getProjectById(app.getProjectId());

            if (applicant == null || project == null || app.getBookedFlatType() == null) {
                 System.err.println(TextFormatUtil.warning("Skipping booking report row: Missing data for application ID " + app.getApplicationId()));
                continue; // Skip if essential data is missing
            }

            // 3. Apply Filters to each potential row
            if (matchesReportFilters(applicant, project, app, filters)) {
                reportRows.add(new Report.ReportRow(
                        applicant.getName(),
                        applicant.getNric(),
                        applicant.getAge(),
                        applicant.getMaritalStatus().name(),
                        app.getBookedFlatType().getDisplayName(), // Use display name
                        project.getProjectName()
                ));
            }
        }

        // 4. Sort the results (e.g., by project name, then applicant name)
         reportRows.sort(Comparator.comparing(Report.ReportRow::getProjectName)
                                  .thenComparing(Report.ReportRow::getApplicantName));


        // 5. Create and return the Report object
        return new Report(reportTitle, filters, reportRows);
    }

    // Helper method to check if a booked application matches the filters
    private boolean matchesReportFilters(User applicant, Project project, BTOApplication application, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true; // No filters, always matches
        }

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            if (value == null || value.trim().isEmpty()) continue;

            switch (key) {
                case "maritalstatus":
                     try {
                         MaritalStatus filterStatus = MaritalStatus.valueOf(value.trim().toUpperCase());
                         if (applicant.getMaritalStatus() != filterStatus) return false;
                     } catch (IllegalArgumentException e) {
                          System.err.println(TextFormatUtil.warning("Ignoring invalid marital status filter: " + value));
                     }
                    break;
                case "flattype":
                    FlatType filterType = FlatType.fromDisplayName(value.trim());
                     if (filterType == null) {
                          System.err.println(TextFormatUtil.warning("Ignoring invalid flat type filter: " + value));
                     } else if (application.getBookedFlatType() != filterType) {
                         return false;
                     }
                    break;
                case "projectname":
                    if (!project.getProjectName().equalsIgnoreCase(value.trim())) {
                        return false;
                    }
                    break;
                 case "projectid":
                    try {
                         int filterProjectId = Integer.parseInt(value.trim());
                         if (project.getProjectId() != filterProjectId) return false;
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Ignoring invalid project ID filter: " + value));
                     }
                     break;
                 case "neighborhood":
                 case "location":
                     if (!project.getNeighborhood().equalsIgnoreCase(value.trim())) {
                         return false;
                     }
                     break;
                 case "minage":
                     try {
                         int minAge = Integer.parseInt(value.trim());
                         if (applicant.getAge() < minAge) return false;
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Ignoring invalid min age filter: " + value));
                     }
                     break;
                  case "maxage":
                     try {
                         int maxAge = Integer.parseInt(value.trim());
                         if (applicant.getAge() > maxAge) return false;
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Ignoring invalid max age filter: " + value));
                     }
                     break;

                // Add other relevant filters (e.g., NRIC, Applicant Name - case-insensitive contains?)

                default:
                    System.err.println(TextFormatUtil.warning("Ignoring unknown report filter key: " + key));
            }
        }
        return true; // All applied filters matched
    }
}