package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.*;
import stores.DataStore;
import services.FlatBookingServiceImpl; 

import java.util.List;
import java.util.Map;
import java.util.Comparator;

/**
 * Handles the display and input for the HDB Officer user interface.
 * Implements PasswordChangeView for the password change functionality.
 */
public class HDBOfficerMenu implements controllers.UserController.PasswordChangeView {

     /**
      * Displays the main menu for HDB Officers and gets their choice.
      * @param handlingProjectName The name of the project currently handled by the officer, or null.
      * @return The integer choice selected by the user.
      */
     public int displayOfficerMenu(String handlingProjectName) {
        String title = "Officer Menu" + (handlingProjectName != null ? " (Handling: " + TextFormatUtil.bold(handlingProjectName) + ")" : "");
        CommonView.displayNavigationBar(title);
        System.out.println("--- Project Assignment ---");
        System.out.println("1. View My Handling Project Details");
        System.out.println("2. Register to Handle a Different Project");
        System.out.println("3. View My Registration Statuses");
        System.out.println("--- Applicant Actions (As Applicant) ---");
        System.out.println("4. View Eligible BTO Projects (Apply Filters)");
        System.out.println("5. Apply for BTO Project");
        System.out.println("6. View My BTO Application Status");
        System.out.println("--- Officer Duties ---");
        System.out.println("7. View/Reply Enquiries for Handling Project");
        System.out.println("8. Assist Applicant Flat Booking");
        System.out.println("9. Generate Booking Receipt");
        System.out.println("--- Account ---");
        System.out.println("10. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 10);
    }



    /**
     * Displays a list of projects specifically for Officer Registration selection.
     * @param projects List of projects open for registration.
     */
     public void displayProjectsForRegistration(List<Project> projects) {
         System.out.println("\n--- Projects Open for Officer Registration ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No projects are currently open for officer registration.");
             return;
         }
         String headerFormat = "%-5s | %-25s | %-15s | %-10s | %-10s\n";
         String rowFormat    = "%-5d | %-25s | %-15s | %-10s | %-10s\n";
         CommonView.displayTableHeader(headerFormat, "ID", "Project Name", "Neighborhood", "Open Date", "Close Date");
         // Sort projects by ID for consistent display before showing
         projects.sort(Comparator.comparingInt(Project::getProjectId));
         for (Project p : projects) {
             CommonView.displayTableRow(rowFormat,
                    p.getProjectId(),
                    p.getProjectName(),
                    p.getNeighborhood(),
                    utils.DateUtils.formatDate(p.getApplicationOpeningDate()),
                    utils.DateUtils.formatDate(p.getApplicationClosingDate()));
        }
     }

    /**
     * Displays detailed information about a specific project.
     * @param project The Project object to display.
     */
    public void displayProjectDetails(Project project) {
        if (project == null) {
            CommonView.displayWarning("No project details to display (You might not be handling a project or it doesn't exist).");
            return;
        }
        System.out.println("\n--- Project Details ("+ TextFormatUtil.bold(project.getProjectName()) +") ---");
        CommonView.displayMessage(String.format("%-18s: %d", "Project ID", project.getProjectId()));
        CommonView.displayMessage(String.format("%-18s: %s", "Project Name", project.getProjectName()));
        CommonView.displayMessage(String.format("%-18s: %s", "Neighborhood", project.getNeighborhood()));
        CommonView.displayMessage(String.format("%-18s: %s", "Visibility", (project.isVisible() ? TextFormatUtil.success("ON") : TextFormatUtil.warning("OFF"))));
        CommonView.displayMessage(String.format("%-18s: %s", "Application Open", utils.DateUtils.formatDate(project.getApplicationOpeningDate())));
        CommonView.displayMessage(String.format("%-18s: %s", "Application Close", utils.DateUtils.formatDate(project.getApplicationClosingDate())));
        CommonView.displayMessage(String.format("%-18s: %s", "Manager In Charge", project.getAssignedHDBManagerNric()));
        CommonView.displayMessage(String.format("%-18s: %d / %d", "Officer Slots", project.getCurrentOfficerCount(), project.getMaxOfficerSlots()));
        CommonView.displayMessage(String.format("%-18s: %s", "Assigned Officers", (project.getAssignedHDBOfficerNrics().isEmpty() ? "None" : String.join(", ", project.getAssignedHDBOfficerNrics()))));

        System.out.println("\n--- Unit Availability (Available / Total) ---");
        String headerFormat = "%-10s : %-10s\n";
        String rowFormat    = "%-10s : %d / %d\n";
        System.out.printf(headerFormat, "Flat Type", "Avail/Total");
        System.out.println("-".repeat(25));
        boolean unitsDisplayed = false;
        for (FlatType type : List.of(FlatType.TWO_ROOM, FlatType.THREE_ROOM)) {
            int total = project.getTotalUnits().getOrDefault(type, 0);
            int available = project.getAvailableUnits().getOrDefault(type, 0);
             if (total > 0) {
                 System.out.printf(rowFormat, type.getDisplayName(), available, total);
                 unitsDisplayed = true;
             }
        }
         if (!unitsDisplayed) { System.out.println("No units defined for this project."); }
         System.out.println("-------------------------");
    }

     /**
      * Displays a list of the officer's registration requests.
      * @param registrations List of HDBOfficerRegistration objects.
      */
     public void displayRegistrationList(List<HDBOfficerRegistration> registrations) {
        System.out.println("\n--- My Registration Requests ---");
        if (registrations == null || registrations.isEmpty()) {
            System.out.println("You have no registration requests.");
            return;
        }
         String headerFormat = "%-7s | %-20s | %-10s | %s\n";
         String rowFormat    = "%-7d | %-20s | %-10s | %s\n";
         CommonView.displayTableHeader(headerFormat, "Reg ID", "Project Name", "Status", "Request Date");
         registrations.sort(Comparator.comparing(HDBOfficerRegistration::getRequestDate).reversed());
        for (HDBOfficerRegistration reg : registrations) {
            Project p = DataStore.getProjectById(reg.getProjectId());
            CommonView.displayTableRow(rowFormat,
                 reg.getRegistrationId(),
                 (p != null ? p.getProjectName() : "N/A (Project ID: " + reg.getProjectId() + ")"),
                 reg.getStatus(),
                 utils.DateUtils.formatDate(reg.getRequestDate()));
        }
    }

     /**
      * Displays the result of a registration request submission.
      * @param success true if successful, false otherwise.
      * @param action The action performed (e.g., "submitted").
      */
     public void displayRegistrationResult(boolean success, String action) {
         if (success) CommonView.displaySuccess("Registration request " + action + " successfully.");
         else CommonView.displayError("Failed to " + action + " registration request.");
     }

     //Enquiry Management
     public void displayProjectEnquiryList(List<Enquiry> enquiries) {
        new ApplicantMenu().displayEnquiryList("Enquiries for Handling Project", enquiries);
    }
     public String getReplyInput() {
         return InputUtil.readString("Enter your reply to the enquiry: ");
     }
     public void displayReplyResult(boolean success) {
         if (success) CommonView.displaySuccess("Reply submitted successfully.");
         else CommonView.displayError("Failed to submit reply.");
     }

     //Flat Booking help
     public String getApplicantNricForBooking() {
         return InputUtil.readString("Enter Applicant's NRIC to assist with booking: ");
     }
     public void displayApplicationForBooking(BTOApplication application) {
           System.out.println("\n--- Application Found for Booking ---");
           if (application == null) {
               CommonView.displayError("No application with status SUCCESSFUL found for the given NRIC.");
               return;
           }
           new ApplicantMenu().displayApplicationStatus(application);
      }
      public FlatType getFlatTypeForBooking(List<FlatType> availableTypes) {
           CommonView.displayMessage("\n" + TextFormatUtil.bold("Officer:") +" Assisting applicant with flat selection.");
           return new ApplicantMenu().getFlatTypeSelection(availableTypes);
      }
      public boolean confirmBooking(FlatType type) {
          return InputUtil.readBooleanYN("Confirm booking for one unit of " + type.getDisplayName() + "? (y/n): ");
      }
       public void displayBookingResult(FlatBooking booking) {
          if (booking != null) {
               CommonView.displaySuccess("Flat booked successfully!");
               CommonView.displayMessage("Booking ID: " + booking.getBookingId());
               System.out.println("\nGenerating Receipt...");
               System.out.println(new FlatBookingServiceImpl().generateBookingReceipt(booking.getBookingId()));
          } else {
              CommonView.displayError("Failed to book flat.");
          }
      }
      public int getBookingIdForReceipt() {
           return InputUtil.readInt("Enter the Booking ID to generate receipt (or 0 to cancel): ");
      }
      public void displayReceipt(String receipt) {
           if (receipt == null || receipt.startsWith(TextFormatUtil.error(""))) {
               CommonView.displayError(receipt != null ? receipt : "Receipt could not be generated.");
           } else {
               System.out.println(receipt);
           }
      }

    // Password Change Methods
     @Override public void displayPasswordChangePrompt() { System.out.println("\n--- Change Password ---"); CommonView.displayMessage("Note: Default password is 'password'."); }
     @Override public String readOldPassword() { return InputUtil.readString("Enter Old Password: "); }
     @Override public String readNewPassword() { return InputUtil.readString("Enter New Password: "); }
     @Override public String readConfirmNewPassword() { return InputUtil.readString("Confirm New Password: "); }
     @Override public void displayPasswordChangeSuccess() { CommonView.displaySuccess("Password changed successfully. Please log in again."); }
     @Override public void displayPasswordChangeError(String message) { CommonView.displayError("Password change failed: " + message); }
}