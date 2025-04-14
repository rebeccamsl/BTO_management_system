package controllers;

import views.HDBOfficerMenu;
import views.ApplicantMenu; // Re-use some applicant views
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


public class HDBOfficerController extends UserController implements UserController.PasswordChangeView {

    private final HDBOfficerMenu officerMenu;
    private final ApplicantMenu applicantView; // For reusing applicant action views
    private final IHDBOfficerService officerService;
    private final IProjectService projectService;
    private final IApplicantService applicantService; // For applicant actions
    private final IEnquiryService enquiryService;
    private final IFlatBookingService bookingService;
    // userService is inherited

    // Store filters like applicant controller
    private Map<String, String> lastProjectFilters = new HashMap<>();

    public HDBOfficerController() {
        super();
        this.officerMenu = new HDBOfficerMenu();
        this.applicantView = new ApplicantMenu(); // Instantiate to reuse its methods
        this.officerService = new HDBOfficerServiceImpl();
        this.projectService = new ProjectServiceImpl();
        this.applicantService = new ApplicantServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
        this.bookingService = new FlatBookingServiceImpl();
    }

    public void showOfficerMenu() {
        int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            System.err.println("Error: No logged-in user found for Officer menu.");
            return;
        }

        do {
            // Get handling project name for display
            Project handlingProject = projectService.getHandlingProjectForOfficer(currentNric);
            String handlingProjectName = (handlingProject != null) ? handlingProject.getProjectName() : null;

            choice = officerMenu.displayOfficerMenu(handlingProjectName);

            switch (choice) {
                // Project Management
                case 1: viewHandlingProjectDetails(currentNric); break;
                case 2: registerToHandleProject(currentNric); break;
                case 3: viewMyRegistrationStatuses(currentNric); break;
                // Applicant Actions (Delegate or Re-implement using shared services)
                case 4: viewEligibleProjects(currentNric); break; // Reuses ApplicantController's logic pattern
                case 5: applyForProject(currentNric); break; // Reuses ApplicantController's logic pattern
                case 6: viewMyApplication(currentNric); break; // Reuses ApplicantController's logic pattern
                // Enquiry Management
                case 7: manageHandlingProjectEnquiries(currentNric); break;
                // Flat Booking
                case 8: assistFlatBooking(currentNric); break;
                case 9: generateBookingReceipt(); break;
                // Account
                case 10: handleChangePassword(currentNric, this); break; // Use self as view
                // Exit
                case 0: AuthController.logout(); break;
                default: CommonView.displayInvalidChoice();
            }
             if (choice != 0 && AuthStore.isLoggedIn()) {
                 CommonView.pressEnterToContinue();
             }

        } while (choice != 0 && AuthStore.isLoggedIn());
    }

    // --- Project Management Methods ---
    private void viewHandlingProjectDetails(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
         if (handlingProject == null) {
             CommonView.displayWarning("You are not currently assigned to handle any project.");
             return;
         }
         // Check project details (even if visibility is off - service/model handles this access implicitly)
         // Attempting to edit is blocked by lack of menu option / controller logic.
        officerMenu.displayProjectDetails(handlingProject);
    }

    private void registerToHandleProject(String officerNric) {
         // Display list of projects currently open for application (potential candidates)
         List<Project> allOpenProjects = projectService.getAllProjects().stream()
                 .filter(p -> p.isWithinApplicationPeriod(new Date()))
                 .collect(Collectors.toList());

         if (allOpenProjects.isEmpty()) {
             CommonView.displayWarning("No projects are currently open for application/registration.");
             return;
         }

         System.out.println("\n--- Projects Open for Officer Registration ---");
         applicantView.displayProjectList(allOpenProjects); // Reuse applicant list view

         int projectId = applicantView.getProjectSelection("Enter Project ID to register for");
         if (projectId == 0) return; // Cancelled

         Project selectedProject = projectService.getProjectById(projectId);
         if (selectedProject == null || !allOpenProjects.contains(selectedProject)) {
              CommonView.displayError("Invalid Project ID selected.");
              return;
         }

         // Service layer performs eligibility checks
         HDBOfficerRegistration registration = officerService.registerForProject(officerNric, projectId);
         officerMenu.displayRegistrationResult(registration != null, "submitted");
     }

    private void viewMyRegistrationStatuses(String officerNric) {
        List<HDBOfficerRegistration> registrations = officerService.getOfficerRegistrations(officerNric);
        officerMenu.displayRegistrationList(registrations);
    }

    // --- Applicant Action Methods (Reusing ApplicantController patterns) ---
     private void viewEligibleProjects(String officerNric) {
         this.lastProjectFilters = applicantView.getProjectFilters(); // Get filters
         List<Project> projects = projectService.getVisibleProjectsForApplicant(officerNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         applicantView.displayProjectList(projects); // Reuse applicant view
     }

     private void applyForProject(String officerNric) {
         // Service layer checks if officer is handling the project they try to apply for (should be blocked if so)
         // Service layer checks general eligibility (age, marital status)
         BTOApplication existingApp = applicantService.viewApplicationStatus(officerNric);
          if (existingApp != null) {
             System.out.println(TextFormatUtil.warning("You already have an active application (Status: " + existingApp.getStatus() + "). Cannot apply for another project."));
             applicantView.displayApplicationStatus(existingApp);
             return;
         }

        List<Project> projects = projectService.getVisibleProjectsForApplicant(officerNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         if (projects == null || projects.isEmpty()) {
             System.out.println("No eligible projects available to apply for (based on current filters).");
             return;
         }
        applicantView.displayProjectList(projects);

        int projectId = applicantView.getProjectSelection("Enter the Project ID you wish to apply for");
        if (projectId == 0) return;

        Project selectedProject = projectService.getProjectById(projectId);
          final int finalProjectId = projectId;
         boolean wasDisplayed = projects.stream().anyMatch(p -> p.getProjectId() == finalProjectId);
         if (selectedProject == null || !wasDisplayed) {
            System.out.println(TextFormatUtil.error("Invalid Project ID or not eligible/visible based on current filters."));
            return;
        }
         // Check if officer tries to apply for the project they handle (if any)
          Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
          if (handlingProject != null && handlingProject.getProjectId() == projectId) {
               CommonView.displayError("You cannot apply for the project you are assigned to handle.");
               return;
          }


         User officerAsApplicant = DataStore.getUserByNric(officerNric); // Get user details
          List<FlatType> applicableTypes = getApplicableFlatTypesForApplicant(officerAsApplicant, selectedProject); // Reuse helper

        if (applicableTypes.isEmpty()) {
            System.out.println(TextFormatUtil.error("No suitable flat types available for you in this specific project."));
            return;
        }

         FlatType selectedFlatType = applicantView.getFlatTypeSelection(applicableTypes);
         if (selectedFlatType == null) return;

        BTOApplication newApplication = applicantService.applyForProject(officerNric, projectId, selectedFlatType);
        applicantView.displayApplicationResult(newApplication); // Reuse applicant view
     }

      // Helper copied from ApplicantController for consistency
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


     private void viewMyApplication(String officerNric) {
        BTOApplication application = applicantService.viewApplicationStatus(officerNric);
        applicantView.displayApplicationStatus(application); // Reuse applicant view
    }

    // --- Enquiry Methods ---
    private void manageHandlingProjectEnquiries(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
        if (handlingProject == null) {
            CommonView.displayWarning("You are not currently assigned to handle any project's enquiries.");
            return;
        }

        List<Enquiry> enquiries = enquiryService.viewProjectEnquiries(handlingProject.getProjectId());
        officerMenu.displayProjectEnquiryList(enquiries); // Use officer menu's display

        if (enquiries.isEmpty()) return; // Nothing to reply to

         int enquiryId = applicantView.getEnquiryIdToManage("reply to"); // Reuse applicant view prompt
         if (enquiryId == 0) return; // Cancelled

         // Verify the ID is from the list shown
          boolean isValidId = enquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
          if (!isValidId) {
              CommonView.displayError("Invalid Enquiry ID selected from the list.");
              return;
          }


         String replyText = officerMenu.getReplyInput();
         boolean success = enquiryService.replyToEnquiry(enquiryId, officerNric, replyText);
         officerMenu.displayReplyResult(success);
    }

    // --- Booking Methods ---
    private void assistFlatBooking(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
         if (handlingProject == null) {
             CommonView.displayWarning("You must be assigned to a project to assist with bookings.");
             return;
         }

        String applicantNric = officerMenu.getApplicantNricForBooking();
        BTOApplication application = officerService.retrieveApplicationForBooking(applicantNric); // Finds SUCCESSFUL app

        officerMenu.displayApplicationForBooking(application);
        if (application == null) return; // No suitable application found

        // Verify the application is for the project the officer is handling
        if (application.getProjectId() != handlingProject.getProjectId()) {
            CommonView.displayError("This application (ID: " + application.getApplicationId() + ") is for project '"
                                    + DataStore.getProjectById(application.getProjectId()).getProjectName()
                                    + "', which you are not handling.");
            return;
        }

        // Determine available flat types for booking (based on *applied* type and project availability)
         // Applicant already passed eligibility for applied type. Officer just confirms the choice if available.
         FlatType appliedType = application.getAppliedFlatType();
         if (handlingProject.getAvailableUnits(appliedType) <= 0) {
             CommonView.displayError("No units of the applied type (" + appliedType.getDisplayName() + ") are currently available for booking in this project.");
              // This scenario might indicate a race condition or issue with manager approvals vs supply.
              // Officer cannot proceed.
             return;
         }

         // For simplicity, assume applicant books the type they applied for if available.
         // If they could choose other available types, the logic would need adjustment.
         FlatType typeToBook = appliedType;
         // List<FlatType> bookingOptions = List.of(appliedType); // Only allow booking applied type
         // FlatType typeToBook = officerMenu.getFlatTypeForBooking(bookingOptions);
         // if (typeToBook == null) return; // Cancelled


         // Confirm booking
         if (officerMenu.confirmBooking(typeToBook)) {
             // Service handles decrementing units, updating app status, creating booking record
             FlatBooking booking = bookingService.createBooking(application.getApplicationId(), typeToBook, officerNric);
             officerMenu.displayBookingResult(booking); // Displays success/failure and receipt if successful
         } else {
             System.out.println("Booking cancelled.");
         }
    }

    private void generateBookingReceipt() {
         int bookingId = officerMenu.getBookingIdForReceipt();
         if (bookingId <= 0) return; // Allow 0 or negative as cancel/invalid maybe?

         // Check if officer is handling the project associated with this booking? Optional security check.
         FlatBooking booking = bookingService.getBookingById(bookingId);
         Project handlingProject = projectService.getHandlingProjectForOfficer(AuthStore.getCurrentUserNric());

         if (booking != null && handlingProject != null && booking.getProjectId() != handlingProject.getProjectId()) {
             CommonView.displayError("You can only generate receipts for bookings related to the project you are handling ("+handlingProject.getProjectName()+").");
             return;
         }
          // Alternatively, allow any officer to generate any receipt? Brief isn't explicit. Let's allow it for now.


         String receipt = bookingService.generateBookingReceipt(bookingId);
         officerMenu.displayReceipt(receipt);
     }

     // --- Password Change ---
      @Override public void displayPasswordChangePrompt() { officerMenu.displayPasswordChangePrompt(); }
     @Override public String readOldPassword() { return officerMenu.readOldPassword(); }
     @Override public String readNewPassword() { return officerMenu.readNewPassword(); }
     @Override public String readConfirmNewPassword() { return officerMenu.readConfirmNewPassword(); }
     @Override public void displayPasswordChangeSuccess() { officerMenu.displayPasswordChangeSuccess(); }
     @Override public void displayPasswordChangeError(String message) { officerMenu.displayPasswordChangeError(message); }
}
