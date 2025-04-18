package controllers;

import views.HDBManagerMenu;
// Import other views if needed (e.g., ApplicantMenu for list displays)
import views.ApplicantMenu;
import views.HDBOfficerMenu;
import views.CommonView;
import services.*;
import interfaces.*;
import models.*;
import enums.*;
import stores.AuthStore;
import stores.DataStore;
import utils.InputUtil;
import utils.TextFormatUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for handling HDB Manager user interactions and logic flow.
 * Implements PasswordChangeView for the password change functionality.
 */
public class HDBManagerController extends UserController implements UserController.PasswordChangeView {

    private final HDBManagerMenu managerMenu;
    private final ApplicantMenu applicantView; // For reusing list displays
    private final HDBOfficerMenu officerView; // For reusing list displays
    private final IProjectService projectService;
    private final IHDBOfficerService officerService;
    private final IHDBManagerService managerService;
    private final IEnquiryService enquiryService;
    private final IReportService reportService;
    // *** FIX: Declare and instantiate IApplicantService ***
    private final IApplicantService applicantService;
    // *** End Fix ***

    // userService is inherited from UserController

    // Store filters
    private Map<String, String> lastProjectFilters = new HashMap<>();
    private Map<String, String> lastReportFilters = new HashMap<>();


    public HDBManagerController() {
        super(); // Initialize UserController (which initializes userService)
        this.managerMenu = new HDBManagerMenu();
        this.applicantView = new ApplicantMenu();
        this.officerView = new HDBOfficerMenu();
        // Instantiate all required services
        this.projectService = new ProjectServiceImpl();
        this.officerService = new HDBOfficerServiceImpl();
        this.managerService = new HDBManagerServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
        this.reportService = new ReportServiceImpl();
        // *** FIX: Instantiate IApplicantService ***
        this.applicantService = new ApplicantServiceImpl(); // Now it's created
        // *** End Fix ***
    }

    /**
     * Main loop for the Manager menu. Displays options and handles user input.
     */
    public void showManagerMenu() {
         int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            CommonView.displayError("Critical Error: Cannot show Manager menu - no user logged in.");
            return;
        }

        do {
            choice = managerMenu.displayManagerMenu();
            try { // Add try-catch around actions
                switch (choice) {
                    // Project Management
                    case 1: createNewProject(currentNric); break;
                    case 2: viewMyManagedProjects(currentNric); break; // Combined view/select
                    case 3: editMyManagedProject(currentNric); break; // Separate Edit action
                    case 4: deleteMyManagedProject(currentNric); break; // Separate Delete action
                    case 5: viewAllProjects(); break;
                    case 6: toggleProjectVisibility(currentNric); break;
                    // Approvals
                    case 7: manageOfficerRegistrations(currentNric); break;
                    case 8: manageBTOApplications(currentNric); break;
                    case 9: manageWithdrawals(currentNric); break;
                    // Enquiries
                    case 10: viewEnquiries(currentNric); break;
                    case 11: replyToEnquiry(currentNric); break;
                    // Reporting
                    case 12: generateReport(); break;
                    // Account
                    case 13: handleChangePassword(currentNric, this); break;
                    // Exit
                    case 0: AuthController.logout(); break;
                    default: CommonView.displayInvalidChoice();
                }
            } catch (Exception e) {
                 CommonView.displayError("An unexpected error occurred while processing your request.");
                 System.err.println("Error details: " + e.getMessage());
                 e.printStackTrace();
            }
              if (choice != 0 && AuthStore.isLoggedIn()) {
                 CommonView.pressEnterToContinue();
             }
        } while (choice != 0 && AuthStore.isLoggedIn());
    }

    // --- Project Management ---
    private void createNewProject(String managerNric) {
         Project tempProjectData = managerMenu.getNewProjectDetails(managerNric);
         if (tempProjectData == null) {
             // View already displayed cancellation or error message
             return;
         }
         Project createdProject = projectService.createProject(
                 managerNric,
                 tempProjectData.getProjectName(),
                 tempProjectData.getNeighborhood(),
                 tempProjectData.getTotalUnits(),
                 tempProjectData.getApplicationOpeningDate(),
                 tempProjectData.getApplicationClosingDate(),
                 tempProjectData.getMaxOfficerSlots()
         );
         managerMenu.displayCreateProjectResult(createdProject);
    }

     private void viewMyManagedProjects(String managerNric) {
         List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
         managerMenu.displayProjectList("My Managed Projects", myProjects);
         // This method now only views. Edit/Delete are separate options.
     }

     private void editMyManagedProject(String managerNric) {
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
          if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects to edit."); return; }
          managerMenu.displayProjectList("Select Project to Edit", myProjects);
          int projectId = managerMenu.getProjectIdToManage("Edit");
          if (projectId == 0) return;

          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
          if (selectedProject == null) {
              CommonView.displayError("Invalid Project ID selected from your list.");
              return;
          }
          Project updatedData = managerMenu.getEditedProjectDetails(selectedProject);
          if (updatedData != null) {
              boolean success = projectService.editProject(projectId, updatedData, managerNric);
              managerMenu.displayEditProjectResult(success);
          } else {
               CommonView.displayMessage("Edit cancelled or invalid input provided.");
          }
     }

      private void deleteMyManagedProject(String managerNric) {
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
           if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects to delete."); return; }
          managerMenu.displayProjectList("Select Project to Delete", myProjects);
          int projectId = managerMenu.getProjectIdToManage("Delete");
          if (projectId == 0) return;

          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
          if (selectedProject == null) {
              CommonView.displayError("Invalid Project ID selected from your list.");
              return;
          }
          if (managerMenu.confirmDeleteProject(selectedProject.getProjectName())) {
              boolean success = projectService.deleteProject(projectId, managerNric);
              managerMenu.displayDeleteProjectResult(success);
          } else {
              CommonView.displayMessage("Deletion cancelled.");
          }
     }

     private void viewAllProjects() {
          this.lastProjectFilters = applicantView.getProjectFilters(); // Use ApplicantMenu view for filters
         List<Project> allProjects = projectService.getAllProjects();
         // Use ApplicantService filter method as it works on project list + filters map
         allProjects = applicantService.filterProjects(allProjects, lastProjectFilters);
         managerMenu.displayProjectList("All Projects (Filtered)", allProjects); // Use ManagerMenu view for display
     }

     private void toggleProjectVisibility(String managerNric) {
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
           if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects to toggle visibility."); return; }
          managerMenu.displayProjectList("Select Project to Toggle Visibility", myProjects);
          int projectId = managerMenu.getProjectIdToManage("Toggle Visibility");
          if (projectId == 0) return;

          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
           if (selectedProject == null) {
             CommonView.displayError("Invalid Project ID selected from your list.");
             return;
         }

         boolean currentVisibility = selectedProject.isVisible();
         // Ask for confirmation to toggle TO the opposite state
         if (managerMenu.getVisibilityToggleChoice(currentVisibility)) {
              boolean targetVisibility = !currentVisibility; // The state we want to set
              boolean success = projectService.toggleProjectVisibility(projectId, targetVisibility, managerNric);
              managerMenu.displayToggleVisibilityResult(success, targetVisibility);
         } else {
              CommonView.displayMessage("Visibility toggle cancelled.");
         }
     }

    // --- Approvals ---
     private void manageOfficerRegistrations(String managerNric) {
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
           if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
          managerMenu.displayProjectList("Select Project to Manage Officer Registrations", myProjects);
          int projectId = managerMenu.getProjectIdToManage("Manage Officer Registrations");
          if (projectId == 0) return;

           Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
           if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected from your list."); return; }

         List<HDBOfficerRegistration> pendingRegs = officerService.getPendingRegistrationsForProject(projectId);
         managerMenu.displayOfficerRegistrationList("Pending Registrations for " + selectedProject.getProjectName(), pendingRegs);
         if (pendingRegs.isEmpty()) return;

         int choice = managerMenu.displayOfficerRegistrationMenu(); // 1=Approve, 2=Reject, 0=Back
          if (choice == 0) return;

          int regId = managerMenu.getRegistrationIdToManage(choice == 1 ? "Approve" : "Reject");
          if (regId == 0) return;

          boolean isValidId = pendingRegs.stream().anyMatch(r -> r.getRegistrationId() == regId);
           if (!isValidId) { CommonView.displayError("Invalid Registration ID selected from the pending list."); return; }

          boolean success;
          if (choice == 1) {
              success = managerService.approveOfficerRegistration(regId, managerNric);
              managerMenu.displayOfficerApprovalResult(success, "approved");
          } else { // choice == 2
              success = managerService.rejectOfficerRegistration(regId, managerNric);
              managerMenu.displayOfficerApprovalResult(success, "rejected");
          }
     }

     private void manageBTOApplications(String managerNric) {
            List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
            if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
           managerMenu.displayProjectList("Select Project to Manage Applications", myProjects);
           int projectId = managerMenu.getProjectIdToManage("Manage BTO Applications");
           if (projectId == 0) return;
            Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
            if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         List<BTOApplication> pendingApps = DataStore.getApplications().values().stream()
                  .filter(app -> app.getProjectId() == projectId && app.getStatus() == BTOApplicationStatus.PENDING)
                  .sorted(Comparator.comparing(BTOApplication::getSubmissionDate))
                  .collect(Collectors.toList());

          managerMenu.displayBTOApplicationList("Pending Applications for " + selectedProject.getProjectName(), pendingApps);
          if (pendingApps.isEmpty()) return;

          int choice = managerMenu.displayBTOApplicationMenu(); // 1=Approve, 2=Reject, 0=Back
           if (choice == 0) return;

           int appId = managerMenu.getApplicationIdToManage(choice == 1 ? "Approve" : "Reject");
           if (appId == 0) return;

            boolean isValidId = pendingApps.stream().anyMatch(a -> a.getApplicationId() == appId);
            if (!isValidId) { CommonView.displayError("Invalid Application ID selected from the pending list."); return; }

           boolean success;
           if (choice == 1) {
               success = managerService.approveApplication(appId, managerNric);
               managerMenu.displayBTOApprovalResult(success, "approved");
           } else { // choice == 2
               success = managerService.rejectApplication(appId, managerNric);
               managerMenu.displayBTOApprovalResult(success, "rejected");
           }
     }

      private void manageWithdrawals(String managerNric) {
            List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
             if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
            managerMenu.displayProjectList("Select Project to Manage Withdrawals", myProjects);
            int projectId = managerMenu.getProjectIdToManage("Manage Application Withdrawals");
            if (projectId == 0) return;
             Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
             if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         List<BTOApplication> withdrawalRequests = DataStore.getApplications().values().stream()
                 .filter(app -> app.getProjectId() == projectId && app.isWithdrawalRequested())
                  .sorted(Comparator.comparing(BTOApplication::getSubmissionDate))
                 .collect(Collectors.toList());

         // Use same display method, but title clarifies context
         managerMenu.displayBTOApplicationList("Pending Withdrawal Requests for " + selectedProject.getProjectName(), withdrawalRequests);
         if (withdrawalRequests.isEmpty()) return;

         int choice = managerMenu.displayWithdrawalMenu(); // 1=Approve, 2=Reject, 0=Back
          if (choice == 0) return;

          int appId = managerMenu.getApplicationIdToManage(choice == 1 ? "Approve Withdrawal" : "Reject Withdrawal");
          if (appId == 0) return;

           boolean isValidId = withdrawalRequests.stream().anyMatch(a -> a.getApplicationId() == appId);
           if (!isValidId) { CommonView.displayError("Invalid Application ID selected from the list."); return; }

          boolean success;
          if (choice == 1) {
              success = managerService.approveWithdrawal(appId, managerNric);
              managerMenu.displayWithdrawalApprovalResult(success, "approved");
          } else { // choice == 2
              success = managerService.rejectWithdrawal(appId, managerNric);
              managerMenu.displayWithdrawalApprovalResult(success, "rejected");
          }
     }

    // --- Enquiries ---
    private void viewEnquiries(String managerNric) {
         int choice = managerMenu.displayEnquiryViewChoice();
         if (choice == 0) return;

         List<Enquiry> enquiries;
         String title;

         if (choice == 1) { // View for Managed Project
              List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
              if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
              managerMenu.displayProjectList("Select Project to View Enquiries", myProjects);
              int projectId = managerMenu.getProjectIdToManage("View Enquiries");
              if (projectId == 0) return;
              Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
              if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

              enquiries = enquiryService.viewProjectEnquiries(projectId);
              title = "Enquiries for " + selectedProject.getProjectName();
         } else { // View All
             enquiries = enquiryService.viewAllEnquiries();
             if (enquiries.isEmpty() && AuthStore.getCurrentUser().getRole() != UserRole.MANAGER) {
                // If viewAllEnquiries returned empty due to permissions, the service already logged error
                return;
             }
             title = "All Enquiries";
         }
         // Use ApplicantMenu view for displaying the list
         applicantView.displayEnquiryList(title, enquiries);
    }

     private void replyToEnquiry(String managerNric) {
         List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
          if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects to reply to enquiries for."); return; }
         managerMenu.displayProjectList("Select Project to Reply to Enquiry", myProjects);
         int projectId = managerMenu.getProjectIdToManage("Reply to Enquiry");
         if (projectId == 0) return;
          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
          if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         List<Enquiry> enquiries = enquiryService.viewProjectEnquiries(projectId).stream()
                                     .filter(e -> e.getStatus() != EnquiryStatus.CLOSED) // Only show open/answered
                                     .collect(Collectors.toList());
         applicantView.displayEnquiryList("Open/Answered Enquiries for " + selectedProject.getProjectName(), enquiries);
         if (enquiries.isEmpty()) return;

          int enquiryId = applicantView.getEnquiryIdToManage("reply to");
          if (enquiryId == 0) return;

            boolean isValidId = enquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
            if (!isValidId) { CommonView.displayError("Invalid Enquiry ID selected from the list."); return; }

          String replyText = officerView.getReplyInput(); // Reuse officer view prompt
          boolean success = enquiryService.replyToEnquiry(enquiryId, managerNric, replyText); // Service checks permission again
          officerView.displayReplyResult(success); // Reuse officer view result display
     }

    // --- Reporting ---
    private void generateReport() {
         this.lastReportFilters = managerMenu.getReportFilters();
         Report report = reportService.generateBookingReport(this.lastReportFilters);
         managerMenu.displayReport(report);
     }

    // --- Password Change ---
    @Override public void displayPasswordChangePrompt() { managerMenu.displayPasswordChangePrompt(); }
    @Override public String readOldPassword() { return managerMenu.readOldPassword(); }
    @Override public String readNewPassword() { return managerMenu.readNewPassword(); }
    @Override public String readConfirmNewPassword() { return managerMenu.readConfirmNewPassword(); }
    @Override public void displayPasswordChangeSuccess() { managerMenu.displayPasswordChangeSuccess(); }
    @Override public void displayPasswordChangeError(String message) { managerMenu.displayPasswordChangeError(message); }
}