package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.*;
import stores.DataStore;
import services.FlatBookingServiceImpl; // Only used temporarily for receipt display convenience

import java.util.List;
import java.util.Map;

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
        // Applicant actions like withdrawal, enquiry management accessible via Applicant logic flow
        System.out.println("--- Officer Duties ---");
        System.out.println("7. View/Reply Enquiries for Handling Project");
        System.out.println("8. Assist Applicant Flat Booking");
        System.out.println("9. Generate Booking Receipt");
        System.out.println("--- Account ---");
        System.out.println("10. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 10);
    }

    // --- Project Display ---
     /**
      * Displays detailed information about a specific project.
      * @param project The Project object to display.
      */
     public void displayProjectDetails(Project project) {
         if (project == null) {
             CommonView.displayWarning("No project details to display (Project might not exist or is not assigned).");
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
         for (FlatType type : FlatType.values()) {
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

     // --- Registration Display ---
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
        for (HDBOfficerRegistration reg : registrations) {
            Project p = DataStore.getProjectById(reg.getProjectId());
            CommonView.displayTableRow(rowFormat,
                 reg.getRegistrationId(),
                 (p != null ? p.getProjectName() : "N/A"),
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

     // --- Enquiry Management (Officer View) ---
     /**
      * Displays enquiries for the project the officer is handling.
      * @param enquiries List of Enquiry objects.
      */
     public void displayProjectEnquiryList(List<Enquiry> enquiries) {
        // Re-use ApplicantMenu display logic, provide appropriate title
        new ApplicantMenu().displayEnquiryList("Enquiries for Handling Project", enquiries);
    }

     /**
      * Prompts for and reads the officer's reply to an enquiry.
      * @return The reply text.
      */
     public String getReplyInput() {
         return InputUtil.readString("Enter your reply to the enquiry: ");
     }

      /**
       * Displays the result of submitting a reply.
       * @param success true if successful, false otherwise.
       */
      public void displayReplyResult(boolean success) {
         if (success) CommonView.displaySuccess("Reply submitted successfully.");
         else CommonView.displayError("Failed to submit reply.");
     }

     // --- Flat Booking Assistance ---
     /**
      * Prompts for the NRIC of the applicant being assisted.
      * @return The applicant's NRIC.
      */
     public String getApplicantNricForBooking() {
         return InputUtil.readString("Enter Applicant's NRIC to assist with booking: ");
     }

      /**
       * Displays the details of the application found for booking.
       * @param application The BTOApplication object (should be status SUCCESSFUL).
       */
      public void displayApplicationForBooking(BTOApplication application) {
           System.out.println("\n--- Application Found for Booking ---");
           if (application == null) {
               CommonView.displayError("No application with status SUCCESSFUL found for the given NRIC.");
               return;
           }
           // Reuse applicant view to show the details
           new ApplicantMenu().displayApplicationStatus(application);
      }

      /**
       * Prompts the officer to select/confirm the flat type for booking.
       * @param availableTypes List of FlatType enums that can be booked (usually just the applied type).
       * @return The selected FlatType, or null if cancelled.
       */
      public FlatType getFlatTypeForBooking(List<FlatType> availableTypes) {
           CommonView.displayInfo("\n" + TextFormatUtil.bold("Officer:") +" Assisting applicant with flat selection.");
           // Reuse applicant selection logic
           return new ApplicantMenu().getFlatTypeSelection(availableTypes);
      }

      /**
       * Prompts the officer to confirm the booking action.
       * @param type The FlatType being booked.
       * @return true if confirmed (y), false otherwise (n).
       */
      public boolean confirmBooking(FlatType type) {
          return InputUtil.readBooleanYN("Confirm booking for one unit of " + type.getDisplayName() + "? (y/n): ");
      }

       /**
        * Displays the result of the booking attempt. If successful, shows booking ID and optionally the receipt.
        * @param booking The created FlatBooking object if successful, null otherwise.
        */
       public void displayBookingResult(FlatBooking booking) {
          if (booking != null) {
               CommonView.displaySuccess("Flat booked successfully!");
               CommonView.displayMessage("Booking ID: " + booking.getBookingId());
               // Display receipt immediately after successful booking
               System.out.println("\nGenerating Receipt...");
               // Generate and print receipt (using service temporarily here for simplicity)
               // In a cleaner design, controller would call service then pass receipt string here.
               System.out.println(new FlatBookingServiceImpl().generateBookingReceipt(booking.getBookingId()));
          } else {
              CommonView.displayError("Failed to book flat.");
          }
      }

      /**
       * Prompts the officer for a Booking ID to generate a receipt.
       * @return The entered Booking ID.
       */
      public int getBookingIdForReceipt() {
           return InputUtil.readInt("Enter the Booking ID to generate receipt (or 0 to cancel): ");
      }

      /**
       * Displays the generated booking receipt or an error message.
       * @param receipt The formatted receipt string, or an error string.
       */
      public void displayReceipt(String receipt) {
           if (receipt == null || receipt.startsWith(TextFormatUtil.error(""))) {
               CommonView.displayError(receipt != null ? receipt : "Receipt could not be generated.");
           } else {
               // Add extra newline before receipt for spacing
               System.out.println(receipt); // Receipt string already has newlines and formatting
           }
      }

    // --- Password Change Methods (Implementation of PasswordChangeView) ---
     @Override public void displayPasswordChangePrompt() { System.out.println("\n--- Change Password ---"); CommonView.displayMessage("Note: Default password is 'password'."); }
     @Override public String readOldPassword() { return InputUtil.readString("Enter Old Password: "); }
     @Override public String readNewPassword() { return InputUtil.readString("Enter New Password: "); }
     @Override public String readConfirmNewPassword() { return InputUtil.readString("Confirm New Password: "); }
     @Override public void displayPasswordChangeSuccess() { CommonView.displaySuccess("Password changed successfully. Please log in again."); }
     @Override public void displayPasswordChangeError(String message) { CommonView.displayError("Password change failed: " + message); }
}