package services;

import interfaces.IReportService;
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil; 

import java.util.ArrayList;
import java.util.Collections; 
import java.util.Comparator; 
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the IReportService interface.
 * Handles logic for generating reports based on application data.
 */
public class ReportServiceImpl implements IReportService {

    /**
     * Generates a report of applicants with successful flat bookings based on specified filters.
     * @param filters A map containing filter criteria (e.g., "maritalStatus": "MARRIED", "flatType": "THREE_ROOM", "projectName": "SkyView"). Keys match potential filterable fields.
     * @return A Report object containing the filtered data and metadata.
     */
    @Override
    public Report generateBookingReport(Map<String, String> filters) {
        // Permission check
        String reportTitle = "BTO Flat Booking Report";
        Map<String, String> actualFilters = (filters != null) ? filters : Collections.emptyMap(); // Use empty map if null
        if (!actualFilters.isEmpty()) {
            reportTitle += " (Filtered)";
        }

        // 1. Get applications with status BOOKED
        List<BTOApplication> bookedApplications = DataStore.getApplications().values().stream()
                .filter(app -> app.getStatus() == BTOApplicationStatus.BOOKED)
                .collect(Collectors.toList());

        // 2. Prepare data rows by joining with User and Project info, applying filters
        List<Report.ReportRow> reportRows = new ArrayList<>();
        for (BTOApplication app : bookedApplications) {
            User applicant = DataStore.getUserByNric(app.getApplicantNric());
            Project project = DataStore.getProjectById(app.getProjectId());

            // Skip if essential linked data is missing
            if (applicant == null || project == null || app.getBookedFlatType() == null) {
                 System.err.println(TextFormatUtil.warning("Skipping booking report row: Missing essential data for application ID " + app.getApplicationId()));
                continue;
            }

            // Apply Filters to this specific record
            if (matchesReportFilters(applicant, project, app, actualFilters)) {
                reportRows.add(new Report.ReportRow(
                        applicant.getName(),
                        applicant.getNric(),
                        applicant.getAge(),
                        applicant.getMaritalStatus().name(), 
                        app.getBookedFlatType().getDisplayName(), 
                        project.getProjectName()
                ));
            }
        }

        // 3. Sort the results 
        // Ensure ReportRow has the necessary getter methods (getProjectName, getApplicantName)
         reportRows.sort(Comparator.comparing(Report.ReportRow::getProjectName)
                                  .thenComparing(Report.ReportRow::getApplicantName));

        // 4. Create and return the Report object
        return new Report(reportTitle, actualFilters, reportRows);
    }

    /**
     * Helper method to check if a specific booked application record matches the filter criteria.
     * @param applicant The User object for the applicant.
     * @param project The Project object.
     * @param application The BTOApplication object (must be BOOKED).
     * @param filters The map of filters to apply.
     * @return true if the record matches all active filters, false otherwise.
     */
    private boolean matchesReportFilters(User applicant, Project project, BTOApplication application, Map<String, String> filters) {
        // If filters map is null or empty, auto match
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey().toLowerCase().trim(); 
            String filterValue = entry.getValue();           

            // Skip filter if value is empty
            if (filterValue == null || filterValue.trim().isEmpty()) continue;

            String filterValueTrimmed = filterValue.trim();
            String filterValueLower = filterValueTrimmed.toLowerCase();

            switch (key) {
                case "maritalstatus":
                     try {
                         MaritalStatus filterStatus = MaritalStatus.valueOf(filterValueTrimmed.toUpperCase());
                         if (applicant.getMaritalStatus() != filterStatus) return false; // No match
                     } catch (IllegalArgumentException e) {
                          System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring invalid marital status filter value: '" + filterValue + "'"));

                     }
                    break;
                case "flattype":
                    FlatType filterType = FlatType.fromDisplayName(filterValueTrimmed); 
                     if (filterType == null) {
                          System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring invalid flat type filter value: '" + filterValue + "'"));
                     } else if (application.getBookedFlatType() != filterType) { 
                         return false; 
                     }
                    break;
                case "projectname":
                    if (!project.getProjectName().equalsIgnoreCase(filterValueTrimmed)) {
                        return false; 
                    }
                    break;
                 case "projectid":
                    try {
                         int filterProjectId = Integer.parseInt(filterValueTrimmed);
                         if (project.getProjectId() != filterProjectId) return false; // No match
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring invalid project ID filter value: '" + filterValue + "'"));
                     }
                     break;
                 case "neighborhood":
                 case "location":
                     if (!project.getNeighborhood().equalsIgnoreCase(filterValueTrimmed)) {
                         return false; // No match
                     }
                     break;
                 case "minage":
                     try {
                         int minAge = Integer.parseInt(filterValueTrimmed);
                         if (applicant.getAge() < minAge) return false; // too young
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring invalid minimum age filter value: '" + filterValue + "'"));
                     }
                     break;
                  case "maxage":
                     try {
                         int maxAge = Integer.parseInt(filterValueTrimmed);
                         if (applicant.getAge() > maxAge) return false; // too old
                     } catch (NumberFormatException e) {
                          System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring invalid maximum age filter value: '" + filterValue + "'"));
                     }
                     break;

                default:
                    System.err.println(TextFormatUtil.warning("Report Filter Warning: Ignoring unknown report filter key: '" + key + "'"));
            }
        }
        // all active filters matched
        return true;
    }
}