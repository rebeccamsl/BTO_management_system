package controllers;

import views.ApplicantMenu;
import views.CommonView;
import services.*; // Import necessary services
import interfaces.*; // Import necessary interfaces
import models.*;
import enums.*;
import stores.AuthStore; // To get current user NRIC
import stores.DataStore; // For quick lookups if needed, but prefer services
import utils.InputUtil; // If controller needs direct input sometimes
import utils.TextFormatUtil;

import java.util.*; // For List, Map etc.


// Implement the interface defined in UserController
public class ApplicantController extends UserController implements UserController.PasswordChangeView {

    private final ApplicantMenu applicantMenu;
    private final IApplicantService applicantService;
    private final IProjectService projectService;
    private final IEnquiryService enquiryService;
     // userService is inherited from UserController

     // Keep track of last used project filters
     private Map<String, String> lastProjectFilters = new HashMap<>();


    public ApplicantController() {
        super(); // Call UserController constructor
        this.applicantMenu = new ApplicantMenu();
        // Instantiate actual service implementations
        this.applicantService = new ApplicantServiceImpl();
        this.projectService = new ProjectServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
    }

    public void showApplicantMenu() {
        int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            System.err.println("Error: No logged-in user found for Applicant menu.");
            return;
        }

        do {
            choice = applicantMenu.displayApplicantMenu();
            switch (choice) {
                case 1:
                    viewEligibleProjects(currentNric);
                    break;
                case 2:
                    applyForProject(currentNric);
                    break;
                case 3:
                    viewMyApplication(currentNric);
                    break;
                case 4:
                     requestWithdrawal(currentNric);
                    break;
                case 5:
                    manageEnquiries(currentNric);
                    break;
                case 6:
                     // Call the inherited method, passing 'this' as the view
                     handleChangePassword(currentNric, this);
                    break;
                case 0:
                    AuthController.logout();
                    break;
                default:
                    CommonView.displayInvalidChoice();
            }
             if (choice != 0 && AuthStore.isLoggedIn()) { // Check if still logged in (password change forces logout)
                 CommonView.pressEnterToContinue();
             }

        } while (choice != 0 && AuthStore.isLoggedIn());
    }

    private void viewEligibleProjects(String applicantNric) {
         // Ask for filters
         this.lastProjectFilters = applicantMenu.getProjectFilters(); // Store the filters used

        List<Project> projects = projectService.getVisibleProjectsForApplicant(applicantNric);
        // Apply filters
         projects = applicantService.filterProjects(projects, lastProjectFilters); // Use the stored filters
        applicantMenu.displayProjectList(projects);
    }

    private void applyForProject(String applicantNric) {
         BTOApplication existingApp = applicantService.viewApplicationStatus(applicantNric);
         if (existingApp != null) {
             System.out.println(TextFormatUtil.warning("You already have an active application (Status: " + existingApp.getStatus() + "). Cannot apply for another project."));
             applicantMenu.displayApplicationStatus(existingApp);
             return;
         }

        // Display eligible projects using stored filters for consistency
        List<Project> projects = projectService.getVisibleProjectsForApplicant(applicantNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         if (projects == null || projects.isEmpty()) {
             System.out.println("No eligible projects available to apply for (based on current filters). Try changing filters.");
             return;
         }
        applicantMenu.displayProjectList(projects);

        int projectId = applicantMenu.getProjectSelection("Enter the Project ID you wish to apply for");
        if (projectId == 0) return;

        Project selectedProject = projectService.getProjectById(projectId);
        // Verify it was in the displayed list
         final int finalProjectId = projectId; // Need final variable for lambda
         boolean wasDisplayed = projects.stream().anyMatch(p -> p.getProjectId() == finalProjectId);
         if (selectedProject == null || !wasDisplayed) {
            System.out.println(TextFormatUtil.error("Invalid Project ID or not eligible/visible based on current filters."));
            return;
        }

         User applicant = DataStore.getUserByNric(applicantNric);
          List<FlatType> applicableTypes = getApplicableFlatTypesForApplicant(applicant, selectedProject);

        if (applicableTypes.isEmpty()) {
            System.out.println(TextFormatUtil.error("No suitable flat types available for you in this specific project."));
            return;
        }

         FlatType selectedFlatType = applicantMenu.getFlatTypeSelection(applicableTypes);
         if (selectedFlatType == null) return;

        BTOApplication newApplication = applicantService.applyForProject(applicantNric, projectId, selectedFlatType);
        applicantMenu.displayApplicationResult(newApplication);
    }

    private List<FlatType> getApplicableFlatTypesForApplicant(User applicant, Project project) {
        List<FlatType> types = new ArrayList<>();
         if (applicant == null || project == null) return types;
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();
        boolean hasTwoRoom = project.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0) > 0;
        boolean hasThreeRoom = project.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0) > 0;

        if (status == MaritalStatus.SINGLE && age >= 35 && hasTwoRoom) {
            types.add(FlatType.TWO_ROOM);
        } else if (status == MaritalStatus.MARRIED && age >= 21) {
            if (hasTwoRoom) types.add(FlatType.TWO_ROOM);
            if (hasThreeRoom) types.add(FlatType.THREE_ROOM);
        }
        return types;
    }

    private void viewMyApplication(String applicantNric) {
        BTOApplication application = applicantService.viewApplicationStatus(applicantNric);
        applicantMenu.displayApplicationStatus(application);
    }

    private void requestWithdrawal(String applicantNric) {
         BTOApplication application = applicantService.viewApplicationStatus(applicantNric);
          if (application == null || application.isWithdrawalRequested()) {
             System.out.println(TextFormatUtil.warning("No active application found or withdrawal already requested."));
              if(application != null) applicantMenu.displayApplicationStatus(application);
             return;
         }

          applicantMenu.displayApplicationStatus(application);
         if (applicantMenu.confirmWithdrawal()) {
             // Pass NRIC for verification in service layer
             boolean success = applicantService.requestWithdrawal(application.getApplicationId(), applicantNric);
             applicantMenu.displayWithdrawalResult(success);
         } else {
             System.out.println("Withdrawal request cancelled.");
         }
     }

     private void manageEnquiries(String applicantNric) {
         int enquiryChoice;
         do {
             enquiryChoice = applicantMenu.displayEnquiryMenu();
             switch (enquiryChoice) {
                 case 1: submitNewEnquiry(applicantNric); break;
                 case 2: viewMyEnquiries(applicantNric); break;
                 case 3: editMyEnquiry(applicantNric); break;
                 case 4: deleteMyEnquiry(applicantNric); break;
                 case 0: break;
                 default: CommonView.displayInvalidChoice();
             }
              if (enquiryChoice != 0) CommonView.pressEnterToContinue();
         } while (enquiryChoice != 0);
     }

     private void submitNewEnquiry(String submitterNric) {
         List<Project> allProjects = projectService.getAllProjects();
         int projectId = applicantMenu.getEnquiryProjectIdInput(allProjects);
         if (projectId <= 0) return;

         Project targetProject = projectService.getProjectById(projectId);
         if (targetProject == null) {
             System.out.println(TextFormatUtil.error("Invalid Project ID selected."));
             return;
         }

         String content = applicantMenu.getEnquiryContentInput();
         Enquiry created = enquiryService.submitEnquiry(submitterNric, projectId, content);
         applicantMenu.displaySubmitEnquiryResult(created != null);
     }

     private void viewMyEnquiries(String submitterNric) {
         List<Enquiry> enquiries = enquiryService.viewMyEnquiries(submitterNric);
         applicantMenu.displayEnquiryList("My Enquiries", enquiries);
     }

     private void editMyEnquiry(String editorNric) {
         List<Enquiry> myEnquiries = enquiryService.viewMyEnquiries(editorNric);
         applicantMenu.displayEnquiryList("My Enquiries", myEnquiries);
          if (myEnquiries.isEmpty()) return;

         int enquiryId = applicantMenu.getEnquiryIdToManage("edit");
         if (enquiryId == 0) return;

         // Verify the selected ID belongs to the user before prompting for content
         boolean ownsEnquiry = myEnquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
         if (!ownsEnquiry) {
             CommonView.displayError("Invalid Enquiry ID selected.");
             return;
         }

         String newContent = applicantMenu.getEditedEnquiryContentInput();
         boolean success = enquiryService.editEnquiry(enquiryId, newContent, editorNric);
         applicantMenu.displayEditEnquiryResult(success);
     }

     private void deleteMyEnquiry(String deleterNric) {
         List<Enquiry> myEnquiries = enquiryService.viewMyEnquiries(deleterNric);
         applicantMenu.displayEnquiryList("My Enquiries", myEnquiries);
         if (myEnquiries.isEmpty()) return;

         int enquiryId = applicantMenu.getEnquiryIdToManage("delete");
         if (enquiryId == 0) return;

          boolean ownsEnquiry = myEnquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
         if (!ownsEnquiry) {
             CommonView.displayError("Invalid Enquiry ID selected.");
             return;
         }

         if (applicantMenu.confirmDeleteEnquiry()) {
            boolean success = enquiryService.deleteEnquiry(enquiryId, deleterNric);
            applicantMenu.displayDeleteEnquiryResult(success);
         } else {
             System.out.println("Deletion cancelled.");
         }
     }

     // Implement PasswordChangeView methods required by UserController
     @Override public void displayPasswordChangePrompt() { applicantMenu.displayPasswordChangePrompt(); }
     @Override public String readOldPassword() { return applicantMenu.readOldPassword(); }
     @Override public String readNewPassword() { return applicantMenu.readNewPassword(); }
     @Override public String readConfirmNewPassword() { return applicantMenu.readConfirmNewPassword(); }
     @Override public void displayPasswordChangeSuccess() { applicantMenu.displayPasswordChangeSuccess(); }
     @Override public void displayPasswordChangeError(String message) { applicantMenu.displayPasswordChangeError(message); }

}