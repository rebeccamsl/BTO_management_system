package services;

import interfaces.IApplicantService;
import interfaces.IProjectService;
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the IApplicantService interface.
 * Handles business logic related to BTO applicants.
 */
public class ApplicantServiceImpl implements IApplicantService {

    private final IProjectService projectService;

    public ApplicantServiceImpl() {
        this.projectService = new ProjectServiceImpl();
    }

    @Override
    public BTOApplication viewApplicationStatus(String applicantNric) {
        if (applicantNric == null || applicantNric.trim().isEmpty()) return null;
        return DataStore.getApplications().values().stream()
                .filter(app -> applicantNric.equals(app.getApplicantNric()))
                .filter(app -> app.getStatus() != BTOApplicationStatus.UNSUCCESSFUL &&
                               app.getStatus() != BTOApplicationStatus.WITHDRAWN)
                .findFirst()
                .orElse(null);
    }

    @Override
    public BTOApplication applyForProject(String applicantNric, int projectId, FlatType flatType) {
        User applicant = DataStore.getUserByNric(applicantNric);
        Project project = projectService.getProjectById(projectId);

        if (applicant == null || project == null) {
            System.err.println(TextFormatUtil.error("Apply failed: Applicant ("+applicantNric+") or Project (ID:"+projectId+") not found."));
            return null;
        }
        if (applicant.getRole() == UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Apply failed: HDB Managers cannot apply for BTO projects."));
             return null;
        }
         if (applicant.getRole() == UserRole.OFFICER) {
             Project handlingProject = projectService.getHandlingProjectForOfficer(applicantNric);
             if (handlingProject != null && handlingProject.getProjectId() == projectId) {
                  System.err.println(TextFormatUtil.error("Apply failed: HDB Officers cannot apply for the project ("+projectId+") they are assigned to handle."));
                  return null;
             }
         }

        BTOApplication existingApp = viewApplicationStatus(applicantNric);
        if (existingApp != null) {
            System.err.println(TextFormatUtil.error("Apply failed: Applicant ("+applicantNric+") already has an active application (ID: " + existingApp.getApplicationId() + ", Status: " + existingApp.getStatus() + "). Cannot apply for multiple projects."));
            return null;
        }

        if (!project.isVisible()) {
             System.err.println(TextFormatUtil.error("Apply failed: Project " + project.getProjectId() + " (" + project.getProjectName() + ") is currently not visible to applicants."));
             return null;
        }
        if (!projectService.isProjectWithinApplicationPeriod(projectId, new java.util.Date())) {
             System.err.println(TextFormatUtil.error("Apply failed: Project " + project.getProjectId() + " (" + project.getProjectName() + ") is not open for applications at this time."));
             return null;
        }
         if (project.getTotalUnits().getOrDefault(flatType, 0) <= 0) {
             System.err.println(TextFormatUtil.error("Apply failed: Project " + project.getProjectId() + " (" + project.getProjectName() + ") does not offer " + flatType.getDisplayName() + " flats."));
             return null;
         }

        if (!isApplicantEligible(applicant, flatType)) {
             System.err.println(TextFormatUtil.error("Apply failed: Applicant profile ("+applicant.getMaritalStatus()+", "+applicant.getAge()+"yo) is not eligible for the selected flat type: " + flatType.getDisplayName() + "."));
             return null;
        }

        BTOApplication newApplication = new BTOApplication(applicantNric, projectId, flatType);
        DataStore.addApplication(newApplication);
        if (applicant instanceof Applicant) {
             ((Applicant) applicant).setCurrentApplicationId(newApplication.getApplicationId());
        }
        DataStore.saveAllData();
        return newApplication;
    }

    private boolean isApplicantEligible(User applicant, FlatType flatType) {
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();
        if (status == MaritalStatus.SINGLE) {
            return age >= 35 && flatType == FlatType.TWO_ROOM;
        } else if (status == MaritalStatus.MARRIED) {
            return age >= 21 && (flatType == FlatType.TWO_ROOM || flatType == FlatType.THREE_ROOM);
        }
        return false;
    }

    @Override
    public boolean requestWithdrawal(int applicationId, String applicantNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        if (application == null) {
             System.err.println(TextFormatUtil.error("Withdrawal request failed: Application ID " + applicationId + " not found."));
            return false;
        }
        if (!Objects.equals(application.getApplicantNric(), applicantNric)) {
             System.err.println(TextFormatUtil.error("Withdrawal request failed: User " + applicantNric + " does not own application " + applicationId + "."));
            return false;
        }
        if (application.getStatus() == BTOApplicationStatus.WITHDRAWN) {
             System.err.println(TextFormatUtil.warning("Withdrawal request failed: Application " + applicationId + " is already withdrawn."));
             return false;
        }
         if (application.isWithdrawalRequested()) {
             System.err.println(TextFormatUtil.warning("Withdrawal request failed: A withdrawal request is already pending for application " + applicationId + "."));
             return false;
        }
          if (application.getStatus() == BTOApplicationStatus.UNSUCCESSFUL) {
              System.err.println(TextFormatUtil.warning("Withdrawal request ignored: Application " + applicationId + " was already unsuccessful."));
              return false;
          }

        application.requestWithdrawal();
        DataStore.saveAllData();
        return true;
    }

    @Override
    public List<Project> filterProjects(List<Project> projects, Map<String, String> filters) {
         if (projects == null) return Collections.emptyList();
         if (filters == null || filters.isEmpty()) {
            return projects;
        }

        return projects.stream()
                .filter(p -> matchesFilters(p, filters))
                .collect(Collectors.toList());
    }

     /** Helper method to check if a single project matches all active filters */
    private boolean matchesFilters(Project project, Map<String, String> filters) {
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey().toLowerCase().trim();
            String value = entry.getValue(); // Filter value (e.g., "3-Room")

            if (value == null || value.trim().isEmpty()) continue;

            String valueTrimmedLower = value.trim().toLowerCase();

            switch (key) {
                case "neighborhood":
                case "location":
                    if (!project.getNeighborhood().equalsIgnoreCase(value.trim())) {
                        return false;
                    }
                    break;
                case "flattype":
                    // The value here should be the display name ("3-Room")
                    FlatType filterType = FlatType.fromDisplayName(value.trim());
                    if (filterType == null) {
                        // This shouldn't happen if the View validated correctly
                        System.err.println("Internal Warning: Invalid flat type '" + value + "' reached service filter logic.");
                        continue; // Ignore invalid filter from map
                    }
                    // *** Check if the project OFFERS this type (Total Units > 0) ***
                    if (project.getTotalUnits().getOrDefault(filterType, 0) <= 0) {
                        return false; // Project doesn't offer this type, so filter fails
                    }
                    break; // IMPORTANT: Added break statement here!
                default:
                    // Ignore unknown filter keys silently
            }
        }
        return true; // All active, valid filter conditions were met
    }
}