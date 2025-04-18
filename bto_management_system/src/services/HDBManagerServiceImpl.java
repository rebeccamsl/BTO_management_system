package services;

import interfaces.IHDBManagerService;
import interfaces.IProjectService; // To add/remove officers, check project ownership
import interfaces.IFlatBookingService; // To increment units on withdrawal after booking
import models.*;
import enums.*;
import stores.DataStore;
import stores.AuthStore; // To verify manager identity
import utils.TextFormatUtil;

public class HDBManagerServiceImpl implements IHDBManagerService {

    private final IProjectService projectService; 
    private final IFlatBookingService flatBookingService; 

    public HDBManagerServiceImpl() {
        this.projectService = new ProjectServiceImpl(); 
        this.flatBookingService = new FlatBookingServiceImpl(); 
    }

    @Override
    public boolean approveOfficerRegistration(int registrationId, String managerNric) {
        HDBOfficerRegistration registration = DataStore.getOfficerRegistrationById(registrationId);
        Project project = (registration != null) ? DataStore.getProjectById(registration.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

        // Validation
        if (registration == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Approve registration failed: Invalid registration, project, or manager."));
             return false;
        }
        if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
            System.err.println(TextFormatUtil.error("Approve registration failed: Only the manager in charge (" + project.getAssignedHDBManagerNric() + ") can approve registrations for project " + project.getProjectId() + "."));
            return false;
        }
        if (registration.getStatus() != RequestStatus.PENDING) {
             System.err.println(TextFormatUtil.error("Approve registration failed: Registration is not pending (Status: " + registration.getStatus() + ")."));
             return false;
        }
        // Check eligibility again before approval
         if (!new HDBOfficerServiceImpl().checkOfficerEligibilityForRegistration(registration.getOfficerNric(), registration.getProjectId())) {
              System.err.println(TextFormatUtil.error("Approve registration failed: Officer is no longer eligible."));
             return false;
         }

        // Check slots and add officer to project
        if (projectService.addOfficerToProject(registration.getProjectId(), registration.getOfficerNric())) {
            // If officer added successfully to project, approve the registration
            registration.approve(); 
            DataStore.saveAllData(); 
            return true;
        } else {
            // addOfficerToProject already prints error if slots full or already assigned
             System.err.println(TextFormatUtil.error("Approve registration failed: Could not add officer to project (likely no slots available or already assigned)."));
             registration.reject(); DataStore.saveAllData(); // Auto-reject
            return false;
        }
    }

    @Override
    public boolean rejectOfficerRegistration(int registrationId, String managerNric) {
        HDBOfficerRegistration registration = DataStore.getOfficerRegistrationById(registrationId);
        Project project = (registration != null) ? DataStore.getProjectById(registration.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

        // Validation
        if (registration == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Reject registration failed: Invalid registration, project, or manager."));
             return false;
        }
        if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
             System.err.println(TextFormatUtil.error("Reject registration failed: Only the manager in charge can reject registrations for this project."));
             return false;
        }
         if (registration.getStatus() != RequestStatus.PENDING) {
             System.err.println(TextFormatUtil.error("Reject registration failed: Registration is not pending (Status: " + registration.getStatus() + ")."));
             return false;
        }

        registration.reject(); 
        DataStore.saveAllData(); 
        return true;
    }

    @Override
    public boolean approveApplication(int applicationId, String managerNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        Project project = (application != null) ? DataStore.getProjectById(application.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

        // Validation
         if (application == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Approve application failed: Invalid application, project, or manager."));
             return false;
         }
        // Only manager in charge of the project can approve
         if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
             System.err.println(TextFormatUtil.error("Approve application failed: Only the manager in charge (" + project.getAssignedHDBManagerNric() + ") can approve applications for project " + project.getProjectId() + "."));
             return false;
         }
         if (application.getStatus() != BTOApplicationStatus.PENDING) {
             System.err.println(TextFormatUtil.error("Approve application failed: Application is not pending (Status: " + application.getStatus() + ")."));
             return false;
         }


        // Check Flat Supply (Use Available Units)
        FlatType appliedType = application.getAppliedFlatType();
        if (project.getAvailableUnits(appliedType) <= 0) {
             System.err.println(TextFormatUtil.error("Approve application failed: No available units for " + appliedType.getDisplayName() + " in project " + project.getProjectId() + "."));
             application.setStatus(BTOApplicationStatus.UNSUCCESSFUL); DataStore.saveAllData();
             return false;
        }

        // Approve the application
        application.setStatus(BTOApplicationStatus.SUCCESSFUL);
        // NOTE: We DO NOT decrement units here. Units are decremented upon *booking* by the officer.
        // Approval just means they are invited to book.
        DataStore.saveAllData(); 
        return true;
    }

    @Override
    public boolean rejectApplication(int applicationId, String managerNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        Project project = (application != null) ? DataStore.getProjectById(application.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

        // Validation (similar to approve)
         if (application == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
              System.err.println(TextFormatUtil.error("Reject application failed: Invalid application, project, or manager."));
              return false;
         }
         if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
              System.err.println(TextFormatUtil.error("Reject application failed: Only the manager in charge can reject applications for this project."));
              return false;
         }
         if (application.getStatus() != BTOApplicationStatus.PENDING) {
              System.err.println(TextFormatUtil.error("Reject application failed: Application is not pending (Status: " + application.getStatus() + ")."));
              return false;
         }

        application.setStatus(BTOApplicationStatus.UNSUCCESSFUL);
        DataStore.saveAllData(); // Persist status change
        return true;
    }

    @Override
    public boolean approveWithdrawal(int applicationId, String managerNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        Project project = (application != null) ? DataStore.getProjectById(application.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

        // Validation
         if (application == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Approve withdrawal failed: Invalid application, project, or manager."));
             return false;
         }
         if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
             System.err.println(TextFormatUtil.error("Approve withdrawal failed: Only the manager in charge can manage withdrawals for this project."));
             return false;
         }
         if (!application.isWithdrawalRequested()) {
              System.err.println(TextFormatUtil.error("Approve withdrawal failed: No withdrawal request pending for this application."));
              return false;
         }

        // Handle potential unit return if already booked
        if (application.getStatus() == BTOApplicationStatus.BOOKED) {
            FlatType bookedType = application.getBookedFlatType();
             Integer bookingId = application.getFlatBookingId();
            if (bookedType != null) {
                projectService.incrementProjectUnit(application.getProjectId(), bookedType);
                 System.out.println(TextFormatUtil.info("Unit of type " + bookedType.getDisplayName() + " returned to project " + project.getProjectId() + " inventory."));
            }
             // Remove the associated booking record
             if (bookingId != null) {
                 DataStore.removeFlatBooking(bookingId);
                 System.out.println(TextFormatUtil.info("Associated flat booking record (ID: " + bookingId + ") removed."));
             }
        }

        // Approve withdrawal (updates status and clears request flag)
        application.approveWithdrawal();

        // Clear applicant's current application ID if tracked in Applicant model
         User applicant = DataStore.getUserByNric(application.getApplicantNric());
         if (applicant instanceof Applicant) {
             ((Applicant) applicant).clearCurrentApplication();
         }

        DataStore.saveAllData(); 
        return true;
    }

    @Override
    public boolean rejectWithdrawal(int applicationId, String managerNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        Project project = (application != null) ? DataStore.getProjectById(application.getProjectId()) : null;
        User manager = DataStore.getUserByNric(managerNric);

         // Validation 
         if (application == null || project == null || manager == null || manager.getRole() != UserRole.MANAGER) {
             System.err.println(TextFormatUtil.error("Reject withdrawal failed: Invalid application, project, or manager."));
             return false;
         }
         if (!project.getAssignedHDBManagerNric().equals(managerNric)) {
             System.err.println(TextFormatUtil.error("Reject withdrawal failed: Only the manager in charge can manage withdrawals for this project."));
             return false;
         }
         if (!application.isWithdrawalRequested()) {
             System.err.println(TextFormatUtil.error("Reject withdrawal failed: No withdrawal request pending for this application."));
             return false;
         }

        // Reject withdrawal 
        application.rejectWithdrawal();
        DataStore.saveAllData();
        return true;
    }
}