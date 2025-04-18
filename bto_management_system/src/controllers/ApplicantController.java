package controllers;

import views.ApplicantMenu;
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


/**
 * Controller for handling Applicant user interactions and logic flow.
 * Implements PasswordChangeView to integrate with UserController's password change logic.
 */
public class ApplicantController extends UserController implements UserController.PasswordChangeView {

    private final ApplicantMenu applicantMenu;
    private final IApplicantService applicantService;
    private final IProjectService projectService;
    private final IEnquiryService enquiryService;
    // userService is inherited from UserController

    // Store last used project filters for consistent view/apply experience
    private Map<String, String> lastProjectFilters = new HashMap<>();

    public ApplicantController() {
        super(); // Call UserController constructor to initialize userService
        this.applicantMenu = new ApplicantMenu();
        this.applicantService = new ApplicantServiceImpl();
        this.projectService = new ProjectServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
    }

    /**
     * Main loop for the Applicant menu. Displays options and handles user input.
     */
    public void showApplicantMenu() {
        int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            CommonView.displayError("Critical Error: Cannot show Applicant menu - no user logged in.");
            return;
        }

        do {
            choice = applicantMenu.displayApplicantMenu(); // Get choice from view

            try {
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
                        handleChangePassword(currentNric, this);
                        break;
                    case 0:
                        AuthController.logout();
                        break;
                    default:
                        CommonView.displayInvalidChoice();
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

    /** Handles viewing eligible projects with filtering */
    private void viewEligibleProjects(String applicantNric) {
         this.lastProjectFilters = applicantMenu.getProjectFilters();
        List<Project> projects = projectService.getVisibleProjectsForApplicant(applicantNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
        applicantMenu.displayProjectList(projects);
    }

    /** Handles the process of applying for a BTO project */
    private void applyForProject(String applicantNric) {
         BTOApplication existingApp = applicantService.viewApplicationStatus(applicantNric);
         if (existingApp != null) {
             CommonView.displayWarning("You already have an active application (Status: " + existingApp.getStatus() + "). Cannot apply for another project.");
             applicantMenu.displayApplicationStatus(existingApp);
             return;
         }

        List<Project> projects = projectService.getVisibleProjectsForApplicant(applicantNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         if (projects == null || projects.isEmpty()) {
             CommonView.displayWarning("No eligible projects available to apply for (based on current filters). Try changing filters via Option 1.");
             return;
         }
        applicantMenu.displayProjectList(projects);

        int projectId = applicantMenu.getProjectSelection("Enter the Project ID you wish to apply for");
        if (projectId == 0) { CommonView.displayMessage("Application cancelled."); return; }

        Project selectedProject = projectService.getProjectById(projectId);
          final int finalProjectId = projectId;
         boolean wasDisplayed = projects.stream().anyMatch(p -> p.getProjectId() == finalProjectId);
         if (selectedProject == null || !wasDisplayed) {
            CommonView.displayError("Invalid Project ID selected or it wasn't in the eligible list shown.");
            return;
        }

         User applicant = DataStore.getUserByNric(applicantNric);
          List<FlatType> applicableTypes = getApplicableFlatTypesForApplicant(applicant, selectedProject);

        if (applicableTypes.isEmpty()) {
            CommonView.displayError("Based on your profile (age/marital status), you are not eligible for any available flat types in this specific project.");
            return;
        }

         FlatType selectedFlatType = applicantMenu.getFlatTypeSelection(applicableTypes);
         if (selectedFlatType == null) return;

        BTOApplication newApplication = applicantService.applyForProject(applicantNric, projectId, selectedFlatType);
        applicantMenu.displayApplicationResult(newApplication);
    }

    /** Helper to determine applicable flat types based on user profile and project offering */
    private List<FlatType> getApplicableFlatTypesForApplicant(User applicant, Project project) {
        List<FlatType> types = new ArrayList<>();
         if (applicant == null || project == null) return types;
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();
        boolean offersTwoRoom = project.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0) > 0;
        boolean offersThreeRoom = project.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0) > 0;

        if (status == MaritalStatus.SINGLE && age >= 35 && offersTwoRoom) {
            types.add(FlatType.TWO_ROOM);
        } else if (status == MaritalStatus.MARRIED && age >= 21) {
            if (offersTwoRoom) types.add(FlatType.TWO_ROOM);
            if (offersThreeRoom) types.add(FlatType.THREE_ROOM);
        }
        return types;
    }

    /** Handles viewing the applicant's current application status */
    private void viewMyApplication(String applicantNric) {
        BTOApplication application = applicantService.viewApplicationStatus(applicantNric);
        applicantMenu.displayApplicationStatus(application);
    }

    /** Handles requesting withdrawal of an application */
    private void requestWithdrawal(String applicantNric) {
         BTOApplication application = applicantService.viewApplicationStatus(applicantNric);
          if (application == null) {
             CommonView.displayWarning("No active application found to withdraw.");
             return;
         }
          if (application.isWithdrawalRequested()) {
             CommonView.displayWarning("A withdrawal request is already pending for this application.");
             applicantMenu.displayApplicationStatus(application);
             return;
         }

          applicantMenu.displayApplicationStatus(application);
         if (applicantMenu.confirmWithdrawal()) {
             boolean success = applicantService.requestWithdrawal(application.getApplicationId(), applicantNric);
             applicantMenu.displayWithdrawalResult(success);
         } else {
             CommonView.displayMessage("Withdrawal request cancelled.");
         }
     }

     /** Handles the sub-menu for managing enquiries */
     private void manageEnquiries(String applicantNric) {
         int enquiryChoice;
         do {
             enquiryChoice = applicantMenu.displayEnquiryMenu();
             try {
                 switch (enquiryChoice) {
                     case 1: submitNewEnquiry(applicantNric); break;
                     case 2: viewMyEnquiries(applicantNric); break;
                     case 3: editMyEnquiry(applicantNric); break;
                     case 4: deleteMyEnquiry(applicantNric); break;
                     case 0: break;
                     default: CommonView.displayInvalidChoice();
                 }
             } catch (Exception e) {
                  CommonView.displayError("An error occurred while managing enquiries.");
                  System.err.println("Error details: " + e.getMessage());
             }
              if (enquiryChoice != 0) CommonView.pressEnterToContinue();
         } while (enquiryChoice != 0);
     }

     /** Handles submitting a new enquiry */
     private void submitNewEnquiry(String submitterNric) {
         List<Project> allProjects = projectService.getAllProjects(); // Get projects to select from
         int projectId = applicantMenu.getEnquiryProjectIdInput(allProjects);
         if (projectId <= 0) { CommonView.displayMessage("Enquiry submission cancelled."); return; }

         Project targetProject = projectService.getProjectById(projectId);
         if (targetProject == null) { return; } // Service prints warning

         String content = applicantMenu.getEnquiryContentInput();
         Enquiry created = enquiryService.submitEnquiry(submitterNric, projectId, content);
         applicantMenu.displaySubmitEnquiryResult(created != null);
     }

     /** Handles viewing own enquiries */
     private void viewMyEnquiries(String submitterNric) {
         List<Enquiry> enquiries = enquiryService.viewMyEnquiries(submitterNric);
         applicantMenu.displayEnquiryList("My Enquiries", enquiries);
     }

     /** Handles editing an existing enquiry */
     private void editMyEnquiry(String editorNric) {
         List<Enquiry> myEnquiries = enquiryService.viewMyEnquiries(editorNric);
         applicantMenu.displayEnquiryList("My Enquiries for Editing", myEnquiries);
          if (myEnquiries.isEmpty()) return;

         int enquiryId = applicantMenu.getEnquiryIdToManage("edit");
         if (enquiryId == 0) { CommonView.displayMessage("Edit cancelled."); return; }

         boolean ownsEnquiry = myEnquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
         if (!ownsEnquiry) {
             CommonView.displayError("Invalid Enquiry ID selected from your list.");
             return;
         }

         String newContent = applicantMenu.getEditedEnquiryContentInput();
         boolean success = enquiryService.editEnquiry(enquiryId, newContent, editorNric);
         applicantMenu.displayEditEnquiryResult(success);
     }

     /** Handles deleting an existing enquiry */
     private void deleteMyEnquiry(String deleterNric) {
         List<Enquiry> myEnquiries = enquiryService.viewMyEnquiries(deleterNric);
         applicantMenu.displayEnquiryList("My Enquiries for Deletion", myEnquiries);
         if (myEnquiries.isEmpty()) return;

         int enquiryId = applicantMenu.getEnquiryIdToManage("delete");
         if (enquiryId == 0) { CommonView.displayMessage("Deletion cancelled."); return; }

          boolean ownsEnquiry = myEnquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
         if (!ownsEnquiry) {
             CommonView.displayError("Invalid Enquiry ID selected from your list.");
             return;
         }

         if (applicantMenu.confirmDeleteEnquiry()) {
            boolean success = enquiryService.deleteEnquiry(enquiryId, deleterNric);
            applicantMenu.displayDeleteEnquiryResult(success);
         } else {
             CommonView.displayMessage("Deletion cancelled.");
         }
     }

     // --- Password Change Implementation ---
     @Override public void displayPasswordChangePrompt() { applicantMenu.displayPasswordChangePrompt(); }
     @Override public String readOldPassword() { return applicantMenu.readOldPassword(); }
     @Override public String readNewPassword() { return applicantMenu.readNewPassword(); }
     @Override public String readConfirmNewPassword() { return applicantMenu.readConfirmNewPassword(); }
     @Override public void displayPasswordChangeSuccess() { applicantMenu.displayPasswordChangeSuccess(); }
     @Override public void displayPasswordChangeError(String message) { applicantMenu.displayPasswordChangeError(message); }
}