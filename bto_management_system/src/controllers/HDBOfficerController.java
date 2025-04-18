package controllers;

import views.HDBOfficerMenu;
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
import java.util.stream.Collectors;

/**
 * Controller for handling HDB Officer user interactions and logic flow.
 */
public class HDBOfficerController extends UserController implements UserController.PasswordChangeView {

    private final HDBOfficerMenu officerMenu;
    private final ApplicantMenu applicantView;
    private final IHDBOfficerService officerService;
    private final IProjectService projectService;
    private final IApplicantService applicantService;
    private final IEnquiryService enquiryService;
    private final IFlatBookingService bookingService;
    

    private Map<String, String> lastProjectFilters = new HashMap<>();

    public HDBOfficerController() {
        super();
        this.officerMenu = new HDBOfficerMenu();
        this.applicantView = new ApplicantMenu();
        this.officerService = new HDBOfficerServiceImpl();
        this.projectService = new ProjectServiceImpl();
        this.applicantService = new ApplicantServiceImpl();
        this.enquiryService = new EnquiryServiceImpl();
        this.bookingService = new FlatBookingServiceImpl();
    }

    /**
     * Main loop for the Officer menu.
     */
    public void showOfficerMenu() {
        int choice;
        String currentNric = AuthStore.getCurrentUserNric();
        if (currentNric == null) {
            CommonView.displayError("Critical Error: Cannot show Officer menu - no user logged in.");
            return;
        }

        do {
            Project handlingProject = projectService.getHandlingProjectForOfficer(currentNric);
            String handlingProjectName = (handlingProject != null) ? handlingProject.getProjectName() : null;
            choice = officerMenu.displayOfficerMenu(handlingProjectName);

            try {
                switch (choice) {
                    case 1: viewHandlingProjectDetails(currentNric); break;
                    case 2: registerToHandleProject(currentNric); break;
                    case 3: viewMyRegistrationStatuses(currentNric); break;
                    case 4: viewEligibleProjects(currentNric); break;
                    case 5: applyForProject(currentNric); break;
                    case 6: viewMyApplication(currentNric); break;
                    case 7: manageHandlingProjectEnquiries(currentNric); break;
                    case 8: assistFlatBooking(currentNric); break;
                    case 9: generateBookingReceipt(); break;
                    case 10: handleChangePassword(currentNric, this); break;
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
    private void viewHandlingProjectDetails(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
        // Display details using the officer menu's method
        officerMenu.displayProjectDetails(handlingProject); 
        // Handles null case internally
    }

    private void registerToHandleProject(String officerNric) {
         List<Project> allOpenProjects = projectService.getAllProjects().stream()
                 .filter(p -> p.isWithinApplicationPeriod(new Date()))
                 .sorted(Comparator.comparing(Project::getProjectName))
                 .collect(Collectors.toList());

         if (allOpenProjects.isEmpty()) {
             CommonView.displayWarning("No projects are currently open for officer registration.");
             return;
         }

      
         officerMenu.displayProjectsForRegistration(allOpenProjects);

         int projectId = applicantView.getProjectSelection("Enter Project ID to register for"); // Reuse prompt is ok
         if (projectId == 0) { CommonView.displayMessage("Registration cancelled."); return; }

         // Verify selection was in the list shown
         boolean isValidChoice = allOpenProjects.stream().anyMatch(p -> p.getProjectId() == projectId);
         if (!isValidChoice) {
              CommonView.displayError("Invalid Project ID selected from the list.");
              return;
         }

         HDBOfficerRegistration registration = officerService.registerForProject(officerNric, projectId);
         officerMenu.displayRegistrationResult(registration != null, "submitted");
     }

    private void viewMyRegistrationStatuses(String officerNric) {
        List<HDBOfficerRegistration> registrations = officerService.getOfficerRegistrations(officerNric);
        officerMenu.displayRegistrationList(registrations);
    }

    // --- Applicant Actions ---
     private void viewEligibleProjects(String officerNric) {
         this.lastProjectFilters = applicantView.getProjectFilters();
         List<Project> projects = projectService.getVisibleProjectsForApplicant(officerNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         applicantView.displayProjectList(projects); 
     }

     private void applyForProject(String officerNric) {
         BTOApplication existingApp = applicantService.viewApplicationStatus(officerNric);
          if (existingApp != null) {
             CommonView.displayWarning("You already have an active application (Status: " + existingApp.getStatus() + "). Cannot apply for another project.");
             applicantView.displayApplicationStatus(existingApp);
             return;
         }

        List<Project> projects = projectService.getVisibleProjectsForApplicant(officerNric);
         projects = applicantService.filterProjects(projects, lastProjectFilters);
         if (projects == null || projects.isEmpty()) {
             CommonView.displayWarning("No eligible projects available to apply for (based on current filters). Try changing filters via Option 4.");
             return;
         }
        applicantView.displayProjectList(projects);

        int projectId = applicantView.getProjectSelection("Enter the Project ID you wish to apply for");
        if (projectId == 0) { CommonView.displayMessage("Application cancelled."); return; }

        Project selectedProject = projectService.getProjectById(projectId);
          final int finalProjectId = projectId;
         boolean wasDisplayed = projects.stream().anyMatch(p -> p.getProjectId() == finalProjectId);
         if (selectedProject == null || !wasDisplayed) {
            CommonView.displayError("Invalid Project ID selected or not eligible/visible based on current filters.");
            return;
        }

         User officerAsApplicant = DataStore.getUserByNric(officerNric);
         List<FlatType> applicableTypes = getApplicableFlatTypesForApplicant(officerAsApplicant, selectedProject);

        if (applicableTypes.isEmpty()) {
            CommonView.displayError("Based on your profile, you are not eligible for any available flat types in this specific project.");
            return;
        }

         FlatType selectedFlatType = applicantView.getFlatTypeSelection(applicableTypes);
         if (selectedFlatType == null) return; // View already displayed cancel message

        // handles final checks (incl. officer not handling this project)
        BTOApplication newApplication = applicantService.applyForProject(officerNric, projectId, selectedFlatType);
        applicantView.displayApplicationResult(newApplication); // Reuse applicant view
     }

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

     private void viewMyApplication(String officerNric) {
        BTOApplication application = applicantService.viewApplicationStatus(officerNric);
        applicantView.displayApplicationStatus(application); // Reuse applicant view
    }

    // --- Officer Duties ---
    private void manageHandlingProjectEnquiries(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
        if (handlingProject == null) {
            CommonView.displayWarning("You are not currently assigned to handle any project's enquiries.");
            return;
        }

        List<Enquiry> enquiries = enquiryService.viewProjectEnquiries(handlingProject.getProjectId()).stream()
                                    .filter(e -> e.getStatus() != EnquiryStatus.CLOSED) // Only show open/answered
                                    .collect(Collectors.toList());
        officerMenu.displayProjectEnquiryList(enquiries);

        if (enquiries.isEmpty()) {
            CommonView.displayMessage("No open or answered enquiries for this project.");
            return;
        }

         int enquiryId = applicantView.getEnquiryIdToManage("reply to"); // Reuse prompt
         if (enquiryId == 0) return;

          boolean isValidId = enquiries.stream().anyMatch(e -> e.getEnquiryId() == enquiryId);
          if (!isValidId) { CommonView.displayError("Invalid Enquiry ID selected from the list."); return; }

         String replyText = officerMenu.getReplyInput();
         boolean success = enquiryService.replyToEnquiry(enquiryId, officerNric, replyText); // Service checks permission
         officerMenu.displayReplyResult(success);
    }

    private void assistFlatBooking(String officerNric) {
        Project handlingProject = projectService.getHandlingProjectForOfficer(officerNric);
         if (handlingProject == null) {
             CommonView.displayWarning("You must be assigned to a project to assist with bookings for it.");
             return;
         }

        String applicantNric = officerMenu.getApplicantNricForBooking();
        BTOApplication application = officerService.retrieveApplicationForBooking(applicantNric);

        officerMenu.displayApplicationForBooking(application);
        if (application == null) return;

        // Ensure the application belongs to the project the officer is handling
        if (application.getProjectId() != handlingProject.getProjectId()) {
            CommonView.displayError("This application (ID: " + application.getApplicationId() + ") is for project '"
                                    + DataStore.getProjectById(application.getProjectId()).getProjectName() // Safe lookup
                                    + "', which you are not handling (" + handlingProject.getProjectName() + ").");
            return;
        }

        // Assume booking the applied type if available
        FlatType typeToBook = application.getAppliedFlatType();
        if (handlingProject.getAvailableUnits(typeToBook) <= 0) {
            CommonView.displayError("No units of the applied type (" + typeToBook.getDisplayName() + ") are currently available for booking in project " + handlingProject.getProjectId() + ".");
            return;
        }
        List<FlatType> bookingOptions = List.of(typeToBook); // Only offer the applied type

        // Confirm booking (no need to re-select type if only one option)
        if (officerMenu.confirmBooking(typeToBook)) {
            FlatBooking booking = bookingService.createBooking(application.getApplicationId(), typeToBook, officerNric);
            officerMenu.displayBookingResult(booking);
        } else {
            CommonView.displayMessage("Booking cancelled by officer.");
        }
    }

    private void generateBookingReceipt() {
         int bookingId = officerMenu.getBookingIdForReceipt();
         if (bookingId == 0) return; // Cancelled

         
         FlatBooking booking = bookingService.getBookingById(bookingId);
         Project handlingProject = projectService.getHandlingProjectForOfficer(AuthStore.getCurrentUserNric());

         if (booking != null && handlingProject != null && booking.getProjectId() != handlingProject.getProjectId()) {
             CommonView.displayWarning("Warning: Generating receipt for a booking not related to your currently handled project.");
             
         } else if (booking != null && handlingProject == null) {
              CommonView.displayWarning("Warning: You are not currently handling a project, but proceeding to generate receipt.");
         }


         String receipt = bookingService.generateBookingReceipt(bookingId);
         officerMenu.displayReceipt(receipt); 
         // View handles error display if receipt is null/error string
     }

     // --- Password Change Implementation ---
     @Override public void displayPasswordChangePrompt() { officerMenu.displayPasswordChangePrompt(); }
     @Override public String readOldPassword() { return officerMenu.readOldPassword(); }
     @Override public String readNewPassword() { return officerMenu.readNewPassword(); }
     @Override public String readConfirmNewPassword() { return officerMenu.readConfirmNewPassword(); }
     @Override public void displayPasswordChangeSuccess() { officerMenu.displayPasswordChangeSuccess(); }
     @Override public void displayPasswordChangeError(String message) { officerMenu.displayPasswordChangeError(message); }
}