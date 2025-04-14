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


public class HDBManagerController extends UserController implements UserController.PasswordChangeView {

    private final HDBManagerMenu managerMenu;
     private final ApplicantMenu applicantView; // For reusing list displays
     private final HDBOfficerMenu officerView; // For reusing list displays
    private final IProjectService projectService;
    private final IHDBOfficerService officerService; // For pending regs list
    private final IHDBManagerService managerService; // For approvals
    private final IEnquiryService enquiryService;
    private final IReportService reportService;
     // userService inherited

     // Store filters like applicant controller
     private Map<String, String> lastProjectFilters = new HashMap<>(); // For View All Projects filter
     private Map<String, String> lastReportFilters = new HashMap<>(); // For report generation


    public HDBManagerController() {
        super();
        this.managerMenu = new HDBManagerMenu();
        this.applicantView = new ApplicantMenu();
        this.officerView = new HDBOfficerMenu();
        this.projectService = new ProjectServiceImpl();
        this.officerService = new HDBOfficerServiceImpl();
        this.managerService = new HDBManagerServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
        this.reportService = new ReportServiceImpl();
    }

    public void showManagerMenu() {
         int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            System.err.println("Error: No logged-in user found for Manager menu.");
            return;
        }

        do {
            choice = managerMenu.displayManagerMenu();
            switch (choice) {
                // Project Management
                case 1: createNewProject(currentNric); break;
                case 2: viewManageMyProjects(currentNric); break;
                case 3: viewAllProjects(); break;
                case 4: toggleProjectVisibility(currentNric); break;
                // Approvals
                case 5: manageOfficerRegistrations(currentNric); break;
                case 6: manageBTOApplications(currentNric); break;
                case 7: manageWithdrawals(currentNric); break;
                // Enquiries
                case 8: viewEnquiries(currentNric); break;
                case 9: replyToEnquiry(currentNric); break;
                // Reporting
                case 10: generateReport(); break;
                // Account
                case 11: handleChangePassword(currentNric, this); break;
                // Exit
                case 0: AuthController.logout(); break;
                default: CommonView.displayInvalidChoice();
            }
              if (choice != 0 && AuthStore.isLoggedIn()) {
                 CommonView.pressEnterToContinue();
             }
        } while (choice != 0 && AuthStore.isLoggedIn());
    }

    // --- Project Management ---
    private void createNewProject(String managerNric) {
         // Get details from view
         Project tempProjectData = managerMenu.getNewProjectDetails(managerNric);
         if (tempProjectData == null) {
             CommonView.displayError("Project creation cancelled or invalid input provided.");
             return; // Error or cancellation in view
         }

         // Call service to create
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

     private void viewManageMyProjects(String managerNric) {
         List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
         managerMenu.displayProjectList("My Managed Projects", myProjects);
         if (myProjects.isEmpty()) return;

         int projectId = managerMenu.getProjectIdToManage("Edit or Delete");
         if (projectId == 0) return;

         Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
         if (selectedProject == null) {
             CommonView.displayError("Invalid Project ID selected from your list.");
             return;
         }

         // Offer Edit or Delete
         int action = InputUtil.readIntInRange("Select action: 1. Edit Project, 2. Delete Project, 0. Cancel: ", 0, 2);
         if (action == 1) {
             // Edit
             Project updatedData = managerMenu.getEditedProjectDetails(selectedProject);
             if (updatedData != null) {
                 boolean success = projectService.editProject(projectId, updatedData, managerNric);
                 managerMenu.displayEditProjectResult(success);
             }
         } else if (action == 2) {
             // Delete
             if (managerMenu.confirmDeleteProject(selectedProject.getProjectName())) {
                 boolean success = projectService.deleteProject(projectId, managerNric);
                 managerMenu.displayDeleteProjectResult(success);
             } else {
                 System.out.println("Deletion cancelled.");
             }
         }
     }


     private void viewAllProjects() {
         // Ask for filters
          this.lastProjectFilters = applicantView.getProjectFilters(); // Reuse applicant filter input

         List<Project> allProjects = projectService.getAllProjects();
          // Apply filters
          allProjects = applicantService.filterProjects(allProjects, lastProjectFilters); // Reuse applicant filter logic service

         managerMenu.displayProjectList("All Projects (Filtered)", allProjects);
     }

     private void toggleProjectVisibility(String managerNric) {
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
          managerMenu.displayProjectList("My Managed Projects", myProjects);
          if (myProjects.isEmpty()) return;

          int projectId = managerMenu.getProjectIdToManage("Toggle Visibility");
          if (projectId == 0) return;

          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
           if (selectedProject == null) {
             CommonView.displayError("Invalid Project ID selected from your list.");
             return;
         }

         boolean currentVisibility = selectedProject.isVisible();
         boolean makeVisible = !currentVisibility; // The target state

         if (managerMenu.getVisibilityToggleChoice(currentVisibility)) { // Confirms the toggle
              boolean success = projectService.toggleProjectVisibility(projectId, makeVisible, managerNric);
              managerMenu.displayToggleVisibilityResult(success, makeVisible);
         } else {
              System.out.println("Visibility toggle cancelled.");
         }
     }

    // --- Approvals ---
     private void manageOfficerRegistrations(String managerNric) {
         // Select project first
          List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
           if (myProjects.isEmpty()){
               CommonView.displayWarning("You are not managing any projects.");
               return;
           }
          managerMenu.displayProjectList("Select Project to Manage Registrations", myProjects);
          int projectId = managerMenu.getProjectIdToManage("Manage Officer Registrations");
          if (projectId == 0) return;

           Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
           if (selectedProject == null) {
             CommonView.displayError("Invalid Project ID selected from your list.");
             return;
            }

         // Show pending list for selected project
          List<HDBOfficerRegistration> pendingRegs = officerService.getPendingRegistrationsForProject(projectId);
          managerMenu.displayOfficerRegistrationList("Pending Registrations for " + selectedProject.getProjectName(), pendingRegs);
          if (pendingRegs.isEmpty()) return;


         // Offer Approve/Reject
         int action = InputUtil.readIntInRange("Action: 1. Approve, 2. Reject, 0. Cancel: ", 0, 2);
          if (action == 0) return;

          int regId = managerMenu.getRegistrationIdToManage(action == 1 ? "Approve" : "Reject");
          if (regId == 0) return;

          // Verify ID is in the pending list shown
          boolean isValidId = pendingRegs.stream().anyMatch(r -> r.getRegistrationId() == regId);
           if (!isValidId) {
               CommonView.displayError("Invalid Registration ID selected from the pending list.");
               return;
           }

          boolean success = false;
          if (action == 1) {
              success = managerService.approveOfficerRegistration(regId, managerNric);
              managerMenu.displayOfficerApprovalResult(success, "approved");
          } else { // action == 2
              success = managerService.rejectOfficerRegistration(regId, managerNric);
              managerMenu.displayOfficerApprovalResult(success, "rejected");
          }
     }

     private void manageBTOApplications(String managerNric) {
          // Select project first (similar to officer registration)
           List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
            if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
           managerMenu.displayProjectList("Select Project to Manage Applications", myProjects);
           int projectId = managerMenu.getProjectIdToManage("Manage BTO Applications");
           if (projectId == 0) return;
            Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
            if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         // Get pending applications for this project
          List<BTOApplication> pendingApps = DataStore.getApplications().values().stream()
                  .filter(app -> app.getProjectId() == projectId && app.getStatus() == BTOApplicationStatus.PENDING)
                  .sorted(Comparator.comparing(BTOApplication::getSubmissionDate)) // Show oldest first
                  .collect(Collectors.toList());

          managerMenu.displayBTOApplicationList("Pending Applications for " + selectedProject.getProjectName(), pendingApps);
          if (pendingApps.isEmpty()) return;

           // Offer Approve/Reject
          int action = InputUtil.readIntInRange("Action: 1. Approve, 2. Reject, 0. Cancel: ", 0, 2);
           if (action == 0) return;

           int appId = managerMenu.getApplicationIdToManage(action == 1 ? "Approve" : "Reject");
           if (appId == 0) return;

            boolean isValidId = pendingApps.stream().anyMatch(a -> a.getApplicationId() == appId);
            if (!isValidId) { CommonView.displayError("Invalid Application ID selected from the pending list."); return; }

           boolean success = false;
           if (action == 1) {
               success = managerService.approveApplication(appId, managerNric);
               managerMenu.displayBTOApprovalResult(success, "approved");
           } else { // action == 2
               success = managerService.rejectApplication(appId, managerNric);
               managerMenu.displayBTOApprovalResult(success, "rejected");
           }
     }

      private void manageWithdrawals(String managerNric) {
           // Select project first
            List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
             if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
            managerMenu.displayProjectList("Select Project to Manage Withdrawals", myProjects);
            int projectId = managerMenu.getProjectIdToManage("Manage Application Withdrawals");
            if (projectId == 0) return;
             Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
             if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         // Get applications with pending withdrawal requests for this project
         List<BTOApplication> withdrawalRequests = DataStore.getApplications().values().stream()
                 .filter(app -> app.getProjectId() == projectId && app.isWithdrawalRequested())
                  .sorted(Comparator.comparing(BTOApplication::getSubmissionDate)) // Or request date if tracked
                 .collect(Collectors.toList());

         managerMenu.displayBTOApplicationList("Pending Withdrawal Requests for " + selectedProject.getProjectName(), withdrawalRequests);
         if (withdrawalRequests.isEmpty()) return;

         // Offer Approve/Reject
         int action = InputUtil.readIntInRange("Action: 1. Approve Withdrawal, 2. Reject Withdrawal, 0. Cancel: ", 0, 2);
          if (action == 0) return;

          int appId = managerMenu.getApplicationIdToManage(action == 1 ? "Approve Withdrawal" : "Reject Withdrawal");
          if (appId == 0) return;

           boolean isValidId = withdrawalRequests.stream().anyMatch(a -> a.getApplicationId() == appId);
           if (!isValidId) { CommonView.displayError("Invalid Application ID selected from the list."); return; }

          boolean success = false;
          if (action == 1) {
              success = managerService.approveWithdrawal(appId, managerNric);
              managerMenu.displayWithdrawalApprovalResult(success, "approved");
          } else { // action == 2
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
              // Select project first
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
             enquiries = enquiryService.viewAllEnquiries(); // Service checks manager role
             title = "All Enquiries";
         }

         applicantView.displayEnquiryList(title, enquiries); // Reuse applicant view display
    }

     private void replyToEnquiry(String managerNric) {
         // Select project first
         List<Project> myProjects = projectService.getProjectsManagedBy(managerNric);
          if (myProjects.isEmpty()){ CommonView.displayWarning("You are not managing any projects."); return; }
         managerMenu.displayProjectList("Select Project to Reply to Enquiry", myProjects);
         int projectId = managerMenu.getProjectIdToManage("Reply to Enquiry");
         if (projectId == 0) return;
          Project selectedProject = myProjects.stream().filter(p -> p.getProjectId() == projectId).findFirst().orElse(null);
          if (selectedProject == null) { CommonView.displayError("Invalid Project ID selected."); return; }

         // Show enquiries for that project
         List<Enquiry> enquiries = enquiryService.viewProjectEnquiries(projectId);
         applicantView.displayEnquiryList("Enquiries for " + selectedProject.getProjectName(), enquiries);
         if (enquiries.isEmpty()) return;

          int enquiryId = applicantView.getEnquiryIdToManage("reply to"); // Reuse prompt
          if (enquiryId == 0) return;

            boolean isValidId = enquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
            if (!isValidId) { CommonView.displayError("Invalid Enquiry ID selected from the list."); return; }

          String replyText = officerView.getReplyInput(); // Reuse officer view prompt
          boolean success = enquiryService.replyToEnquiry(enquiryId, managerNric, replyText);
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