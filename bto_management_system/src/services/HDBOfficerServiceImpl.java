package services;

import interfaces.IHDBOfficerService;
import interfaces.IProjectService; 
import models.*;
import enums.*;
import stores.DataStore;
import utils.TextFormatUtil;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects; 
import java.util.stream.Collectors;
import java.util.Collections; 

/**
 * Implementation of the IHDBOfficerService interface.
 * Handles business logic related to HDB Officers, primarily registration and retrieving applications for booking.
 */
public class HDBOfficerServiceImpl implements IHDBOfficerService {

    private final IProjectService projectService;

    public HDBOfficerServiceImpl() {
        this.projectService = new ProjectServiceImpl();
    }

    /**
     * Creates a registration request for an HDB Officer for a specific project, after eligibility checks.
     * @param officerNric NRIC of the officer applying.
     * @param projectId ID of the project to register for.
     * @return The created HDBOfficerRegistration object (status PENDING), or null if ineligible or errors occur.
     */
    @Override
    public HDBOfficerRegistration registerForProject(String officerNric, int projectId) {
        User officer = DataStore.getUserByNric(officerNric);
        Project project = projectService.getProjectById(projectId);

        if (officer == null || officer.getRole() != UserRole.OFFICER || project == null) {
            System.err.println(TextFormatUtil.error("Registration failed: Officer ("+officerNric+") or Project (ID:"+projectId+") not found/invalid role."));
            return null;
        }

        // Check if already registered (any status) for this specific project FIRST
        boolean alreadyRegistered = DataStore.getOfficerRegistrations().values().stream()
                .anyMatch(reg -> officerNric.equals(reg.getOfficerNric()) && reg.getProjectId() == projectId);
        if (alreadyRegistered) {
             System.err.println(TextFormatUtil.error("Registration failed: A registration request (pending/approved/rejected) already exists for you for project " + projectId + "."));
             return null;
        }

        // Check core eligibility rules (prints specific errors if fails)
        if (!checkOfficerEligibilityForRegistration(officerNric, projectId)) {
            return null;
        }

        // checks passed, create registration request
        HDBOfficerRegistration newRegistration = new HDBOfficerRegistration(officerNric, projectId);
        DataStore.addOfficerRegistration(newRegistration);
        DataStore.saveAllData(); 
        return newRegistration;
    }

    /**
     * Retrieves all registration requests (regardless of status) made by a specific officer.
     * @param officerNric NRIC of the officer.
     * @return List of HDBOfficerRegistration objects, sorted newest first. Returns empty list if officer not found.
     */
    @Override
    public List<HDBOfficerRegistration> getOfficerRegistrations(String officerNric) {
         User officer = DataStore.getUserByNric(officerNric);
         if (officer == null || officer.getRole() != UserRole.OFFICER) {
              System.err.println(TextFormatUtil.error("Error viewing registrations: User (" + officerNric + ") not found or not an officer."));
              return Collections.emptyList();
         }
        return DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> officerNric.equals(reg.getOfficerNric()))
                .sorted(Comparator.comparing(HDBOfficerRegistration::getRequestDate).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves pending registration requests for a specific project (typically for Manager view).
     * @param projectId ID of the project.
     * @return List of pending HDBOfficerRegistration objects, sorted oldest first. Returns empty list if project not found.
     */
    @Override
    public List<HDBOfficerRegistration> getPendingRegistrationsForProject(int projectId) {
         Project project = projectService.getProjectById(projectId);
         if (project == null) {
              return Collections.emptyList();
         }

        return DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> reg.getProjectId() == projectId && reg.getStatus() == RequestStatus.PENDING)
                .sorted(Comparator.comparing(HDBOfficerRegistration::getRequestDate)) // Show oldest pending first
                .collect(Collectors.toList());
    }

    /**
     * Performs comprehensive eligibility checks for an officer registering for a project.
     * @param officerNric Officer's NRIC.
     * @param projectId Project ID.
     * @return true if eligible according to all rules, false otherwise (prints specific error messages).
     */
    @Override
    public boolean checkOfficerEligibilityForRegistration(String officerNric, int projectId) {
         User officer = DataStore.getUserByNric(officerNric);
         Project projectToRegister = projectService.getProjectById(projectId);

         // Basic validation
         if (officer == null || officer.getRole() != UserRole.OFFICER) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: User " + officerNric + " is not a valid HDB Officer."));
             return false;
         }
        if (projectToRegister == null) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: Target Project ID " + projectId + " does not exist."));
             return false;
        }

        // Rule 1: No intention to apply (check if already applied)
        boolean isApplicantForThisProject = DataStore.getApplications().values().stream()
                .anyMatch(app -> officerNric.equals(app.getApplicantNric()) && app.getProjectId() == projectId);
        if (isApplicantForThisProject) {
            System.err.println(TextFormatUtil.error("Eligibility check failed: Cannot register for project " + projectId + " because you have previously submitted/held a BTO application for it."));
            return false;
        }

        // Rule 2: Not already APPROVED for another project with overlapping period
        Date targetOpen = projectToRegister.getApplicationOpeningDate();
        Date targetClose = projectToRegister.getApplicationClosingDate();

        if (targetOpen == null || targetClose == null) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: Target project (ID:"+projectId+") application dates are missing or invalid."));
             return false;
        }

        boolean handlingConflict = DataStore.getOfficerRegistrations().values().stream()
            .filter(reg -> officerNric.equals(reg.getOfficerNric()) && reg.getStatus() == RequestStatus.APPROVED && reg.getProjectId() != projectId)
            .map(reg -> projectService.getProjectById(reg.getProjectId()))
            .filter(Objects::nonNull)
            .anyMatch(handledProject -> {
                 Date handledOpen = handledProject.getApplicationOpeningDate();
                 Date handledClose = handledProject.getApplicationClosingDate();
                 return handledOpen != null && handledClose != null &&
                        !targetClose.before(handledOpen) && !targetOpen.after(handledClose); // Check overlap
             });

        if (handlingConflict) {
            System.err.println(TextFormatUtil.error("Eligibility check failed: You are already an approved officer for another project with an overlapping application period."));
            return false;
        }

        // Rule 3: No PENDING registration for another project with overlapping period
        boolean pendingConflict = DataStore.getOfficerRegistrations().values().stream()
                .filter(reg -> officerNric.equals(reg.getOfficerNric()) && reg.getStatus() == RequestStatus.PENDING && reg.getProjectId() != projectId)
                .map(reg -> projectService.getProjectById(reg.getProjectId()))
                .filter(Objects::nonNull)
                .anyMatch(pendingProject -> {
                    Date pendingOpen = pendingProject.getApplicationOpeningDate();
                    Date pendingClose = pendingProject.getApplicationClosingDate();
                    return pendingOpen != null && pendingClose != null &&
                           !targetClose.before(pendingOpen) && !targetOpen.after(pendingClose); // Check overlap
                });

         if (pendingConflict) {
             System.err.println(TextFormatUtil.error("Eligibility check failed: You have a PENDING registration for another project with an overlapping application period. Please wait for it to be resolved."));
             return false;
         }

        return true;
    }

    /**
     * Retrieves a SUCCESSFUL BTO application for a given applicant NRIC.
     * @param applicantNric NRIC of the applicant.
     * @return The BTOApplication object if found and successful, null otherwise.
     */
    @Override
    public BTOApplication retrieveApplicationForBooking(String applicantNric) {
         User applicant = DataStore.getUserByNric(applicantNric);
         if (applicant == null) {
             System.err.println(TextFormatUtil.error("Retrieve application failed: Applicant NRIC " + applicantNric + " not found."));
             return null;
         }

        return DataStore.getApplications().values().stream()
                .filter(app -> applicantNric.equals(app.getApplicantNric()) &&
                               app.getStatus() == BTOApplicationStatus.SUCCESSFUL)
                .findFirst()
                .orElse(null);
    }
}