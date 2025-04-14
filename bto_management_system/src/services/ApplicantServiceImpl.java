package services;

import interfaces.IApplicantService;
import interfaces.IProjectService; // Needed for eligibility checks
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApplicantServiceImpl implements IApplicantService {

    private final IProjectService projectService; // Inject dependency

    public ApplicantServiceImpl() {
        // Simple instantiation, consider dependency injection framework in larger apps
        this.projectService = new ProjectServiceImpl();
    }

    @Override
    public BTOApplication viewApplicationStatus(String applicantNric) {
        // Find the application that is not Unsuccessful or Withdrawn
        return DataStore.getApplications().values().stream()
                .filter(app -> app.getApplicantNric().equals(applicantNric))
                .filter(app -> app.getStatus() != BTOApplicationStatus.UNSUCCESSFUL &&
                               app.getStatus() != BTOApplicationStatus.WITHDRAWN)
                .findFirst()
                .orElse(null); // Return null if no active application found
    }

    @Override
    public BTOApplication applyForProject(String applicantNric, int projectId, FlatType flatType) {
        User applicant = DataStore.getUserByNric(applicantNric);
        Project project = DataStore.getProjectById(projectId);

        // 1. Basic Validation
        if (applicant == null || project == null) {
            System.err.println(TextFormatUtil.error("Apply failed: Applicant or Project not found."));
            return null;
        }
        if (applicant.getRole() == UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Apply failed: HDB Managers cannot apply for BTO projects."));
             return null;
        }

        // 2. Check for Existing Application
        BTOApplication existingApp = viewApplicationStatus(applicantNric);
        if (existingApp != null) {
            System.err.println(TextFormatUtil.error("Apply failed: Applicant already has an active application (ID: " + existingApp.getApplicationId() + ", Status: " + existingApp.getStatus() + ")."));
            return null;
        }

        // 3. Check Project Application Period
        if (!projectService.isProjectWithinApplicationPeriod(projectId, new java.util.Date())) {
             System.err.println(TextFormatUtil.error("Apply failed: Project " + project.getProjectName() + " is not open for applications now."));
             return null;
        }

        // 4. Check Applicant Eligibility for Flat Type
        if (!isApplicantEligible(applicant, flatType)) {
             System.err.println(TextFormatUtil.error("Apply failed: Applicant ("+applicant.getMaritalStatus()+", "+applicant.getAge()+"yo) is not eligible for " + flatType.getDisplayName() + "."));
             return null;
        }

        // 5. Check if Project offers the Flat Type (basic check, manager handles supply later)
         if (project.getTotalUnits().getOrDefault(flatType, 0) <= 0) {
             System.err.println(TextFormatUtil.error("Apply failed: Project " + project.getProjectName() + " does not offer " + flatType.getDisplayName() + "."));
             return null;
         }


        // 6. Create and Store Application
        BTOApplication newApplication = new BTOApplication(applicantNric, projectId, flatType);
        DataStore.addApplication(newApplication);

        // Update Applicant's state if tracking currentApplicationId in Applicant model
        if (applicant instanceof Applicant) { // Check if it's actually an Applicant object
             ((Applicant) applicant).setCurrentApplicationId(newApplication.getApplicationId());
        }


        DataStore.saveAllData(); // Persist the new application

        return newApplication;
    }

    // Helper method for eligibility check based on brief rules
    private boolean isApplicantEligible(User applicant, FlatType flatType) {
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();

        if (status == MaritalStatus.SINGLE) {
            // Singles >= 35 can ONLY apply for 2-Room
            return age >= 35 && flatType == FlatType.TWO_ROOM;
        } else if (status == MaritalStatus.MARRIED) {
            // Married >= 21 can apply for any (2-Room or 3-Room)
            return age >= 21 && (flatType == FlatType.TWO_ROOM || flatType == FlatType.THREE_ROOM);
        }
        return false; // Default ineligible
    }

    @Override
    public boolean requestWithdrawal(int applicationId, String applicantNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);

        if (application == null) {
             System.err.println(TextFormatUtil.error("Withdrawal failed: Application not found."));
            return false;
        }

        // Verify ownership
        if (!application.getApplicantNric().equals(applicantNric)) {
             System.err.println(TextFormatUtil.error("Withdrawal failed: You can only withdraw your own application."));
            return false;
        }

        // Check if already withdrawn or requested
        if (application.getStatus() == BTOApplicationStatus.WITHDRAWN || application.isWithdrawalRequested()) {
             System.err.println(TextFormatUtil.warning("Withdrawal failed: Application already withdrawn or request pending."));
             return false;
        }

        // Mark for withdrawal
        application.requestWithdrawal();
        DataStore.saveAllData(); // Persist the request flag change
        return true;
    }

    @Override
    public List<Project> filterProjects(List<Project> projects, Map<String, String> filters) {
         if (filters == null || filters.isEmpty()) {
            return projects; // No filters applied
        }

        return projects.stream()
                .filter(p -> matchesFilters(p, filters))
                .collect(Collectors.toList());
    }

     private boolean matchesFilters(Project project, Map<String, String> filters) {
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue(); // Keep value case for potential exact match needs

            if (value == null || value.trim().isEmpty()) continue; // Ignore empty filter values

            switch (key) {
                case "neighborhood":
                case "location": // Allow alias
                    if (!project.getNeighborhood().equalsIgnoreCase(value.trim())) {
                        return false;
                    }
                    break;
                case "flattype":
                    FlatType filterType = FlatType.fromDisplayName(value.trim());
                     if (filterType == null) {
                         System.err.println(TextFormatUtil.warning("Ignoring invalid flat type filter: " + value));
                         continue; // Ignore invalid filter, or return false? Decide behavior. Let's ignore.
                     }
                    // Check if the project *offers* this flat type
                    if (project.getTotalUnits().getOrDefault(filterType, 0) <= 0) {
                        return false;
                    }
                    break;
                // Add more filters as needed (e.g., project name contains, date range)
                default:
                    System.err.println(TextFormatUtil.warning("Ignoring unknown filter key: " + key));
            }
        }
        return true; // All applied filters matched
    }

}