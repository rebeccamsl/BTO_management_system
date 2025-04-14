package services;

import interfaces.IHDBOfficerService;
import interfaces.IProjectService; // Needed for checks
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class HDBOfficerServiceImpl implements IHDBOfficerService {

    private final IProjectService projectService; // Inject dependency

    public HDBOfficerServiceImpl() {
        this.projectService = new ProjectServiceImpl(); // Simple instantiation
    }

    @Override
    public HDBOfficerRegistration registerForProject(String officerNric, int projectId) {
        User officer = DataStore.getUserByNric(officerNric);
        Project project = DataStore.getProjectById(projectId);

        if (officer == null || officer.getRole() != UserRole.OFFICER || project == null) {
            System.err.println(TextFormatUtil.error("Registration failed: Officer or Project not found/invalid."));
            return null;
        }

        // Check eligibility
        if (!checkOfficerEligibilityForRegistration(officerNric, projectId)) {
            // Eligibility method prints specific error
            return null;
        }

        // Check if already registered for this project (pending/approved/rejected)
        boolean alreadyRegistered = DataStore.getOfficerRegistrations().values().stream()
                .anyMatch(reg -> reg.getOfficerNric().equals(officerNric) && reg.getProjectId() == projectId);
        if (alreadyRegistered) {
             System.err.println(TextFormatUtil.error("Registration failed: You have already submitted a registration request for this project."));
             return null;
        }


        HDBOfficerRegistration newRegistration = new HDBOfficerRegistration(officerNric, projectId);
        DataStore.addOfficerRegistration(newRegistration);
        DataStore.saveAllData(); // Persist
        return newRegistration;
    }

    @Override
    public List<HDBOfficerRegistration> getOfficerRegistrations(String officerNric) {
        return DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> reg.getOfficerNric().equals(officerNric))
                .sorted(Comparator.comparing(HDBOfficerRegistration::getRequestDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<HDBOfficerRegistration> getPendingRegistrationsForProject(int projectId) {
         Project project = DataStore.getProjectById(projectId);
          if (project == null) return List.of(); // Return empty list if project doesn't exist

        return DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> reg.getProjectId() == projectId && reg.getStatus() == RequestStatus.PENDING)
                .sorted(Comparator.comparing(HDBOfficerRegistration::getRequestDate)) // Show oldest first
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkOfficerEligibilityForRegistration(String officerNric, int projectId) {
        Project projectToRegister = DataStore.getProjectById(projectId);
        if (projectToRegister == null) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: Project not found."));
             return false; // Project must exist
        }

        // 1. Check if officer has applied for THIS project as an Applicant
        boolean isApplicantForThisProject = DataStore.getApplications().values().stream()
                .anyMatch(app -> app.getApplicantNric().equals(officerNric) && app.getProjectId() == projectId);
        if (isApplicantForThisProject) {
            System.err.println(TextFormatUtil.error("Eligibility check failed: Cannot register for a project you have applied for as an applicant."));
            return false;
        }

        // 2. Check if officer is already handling another project within the SAME application period
        Date targetOpen = projectToRegister.getApplicationOpeningDate();
        Date targetClose = projectToRegister.getApplicationClosingDate();

        if (targetOpen == null || targetClose == null) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: Target project application dates are invalid."));
             return false; // Cannot check period if dates are missing
        }

        // Find projects the officer is ALREADY APPROVED for
        List<Integer> handledProjectIds = DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> reg.getOfficerNric().equals(officerNric) && reg.getStatus() == RequestStatus.APPROVED)
                .map(HDBOfficerRegistration::getProjectId)
                .collect(Collectors.toList());

        for (int handledProjectId : handledProjectIds) {
             if (handledProjectId == projectId) continue; // Ignore the project they are trying to register for

             Project handledProject = DataStore.getProjectById(handledProjectId);
             if (handledProject != null) {
                 Date handledOpen = handledProject.getApplicationOpeningDate();
                 Date handledClose = handledProject.getApplicationClosingDate();
                 if (handledOpen != null && handledClose != null) {
                     // Check for period overlap: (StartA <= EndB) and (EndA >= StartB)
                     if (!targetOpen.after(handledClose) && !targetClose.before(handledOpen)) {
                          System.err.println(TextFormatUtil.error("Eligibility check failed: Already handling another project (" + handledProject.getProjectName() + ") during the application period of the target project."));
                          return false; // Overlap found
                     }
                 }
             }
        }

        // 3. Check if officer has a PENDING registration for another project in the same period
        boolean hasPendingConflict = DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> reg.getOfficerNric().equals(officerNric) && reg.getStatus() == RequestStatus.PENDING && reg.getProjectId() != projectId)
                .map(reg -> DataStore.getProjectById(reg.getProjectId()))
                .filter(Objects::nonNull)
                .anyMatch(pendingProject -> {
                    Date pendingOpen = pendingProject.getApplicationOpeningDate();
                    Date pendingClose = pendingProject.getApplicationClosingDate();
                    return pendingOpen != null && pendingClose != null &&
                           !targetOpen.after(pendingClose) && !targetClose.before(pendingOpen);
                });

         if (hasPendingConflict) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: You have a PENDING registration for another project during the application period of the target project."));
             return false;
         }

        return true; // Eligible
    }

    @Override
    public BTOApplication retrieveApplicationForBooking(String applicantNric) {
        // Find the application for this NRIC with status SUCCESSFUL
        return DataStore.getApplications().values().stream()
                .filter(app -> app.getApplicantNric().equals(applicantNric) &&
                               app.getStatus() == BTOApplicationStatus.SUCCESSFUL)
                .findFirst()
                .orElse(null);
    }
}