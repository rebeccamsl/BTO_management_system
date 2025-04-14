package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.*;
import stores.DataStore; // For simple lookups needed in display

import java.util.List;
import java.util.Map;

public class HDBOfficerMenu {

     public int displayOfficerMenu(String handlingProjectName) {
        String title = "Officer Menu" + (handlingProjectName != null ? " (Handling: " + handlingProjectName + ")" : "");
        CommonView.displayNavigationBar(title);
        System.out.println("--- Project Management ---");
        System.out.println("1. View Handling Project Details");
        System.out.println("2. Register to Handle a Project");
        System.out.println("3. View My Registration Statuses");
        System.out.println("--- Applicant Actions ---");
        System.out.println("4. View Eligible BTO Projects (Apply Filters)");
        System.out.println("5. Apply for BTO Project");
        System.out.println("6. View My BTO Application Status");
        // Withdrawal is same flow as applicant
        System.out.println("--- Enquiry Management ---");
        System.out.println("7. View/Reply Enquiries for Handling Project");
        // Enquiry submission/edit/delete is same flow as applicant
        System.out.println("--- Flat Booking ---");
        System.out.println("8. Assist Flat Booking");
        System.out.println("9. Generate Booking Receipt");
        System.out.println("--- Account ---");
        System.out.println("10. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 10);
    }

    // --- Project Display ---
     public void displayProjectDetails(Project project) {
         if (project == null) {
             CommonView.displayWarning("No project details to display.");
             return;
         }
         System.out.println("\n--- Project Details ---");
         System.out.println("Project ID      : " + project.getProjectId());
         System.out.println("Project Name    : " + project.getProjectName());
         System.out.println("Neighborhood    : " + project.getNeighborhood());
         System.out.println("Visibility      : " + (project.isVisible() ? "ON" : "OFF"));
         System.out.println("Application Open: " + utils.DateUtils.formatDate(project.getApplicationOpeningDate()));
         System.out.println("Application Close: " + utils.DateUtils.formatDate(project.getApplicationClosingDate()));
         System.out.println("Manager In Charge: " + project.getAssignedHDBManagerNric()); // Show NRIC
         System.out.println("Officer Slots   : " + project.getCurrentOfficerCount() + "/" + project.getMaxOfficerSlots());
         System.out.println("Assigned Officers: " + String.join(", ", project.getAssignedHDBOfficerNrics()));
         System.out.println("\n--- Unit Availability ---");
          String unitFormat = "%-10s : %d / %d\n";
          System.out.printf(unitFormat, "Flat Type", "Available", "Total");
          System.out.println("-".repeat(30));
         for (Map.Entry<FlatType, Integer> entry : project.getTotalUnits().entrySet()) {
             FlatType type = entry.getKey();
             int total = entry.getValue();
             int available = project.getAvailableUnits().getOrDefault(type, 0);
              System.out.printf(unitFormat, type.getDisplayName(), available, total);
         }
          System.out.println("-----------------------");
     }

     // --- Registration Display ---
     public void displayRegistrationList(List<HDBOfficerRegistration> registrations) {
        System.out.println("\n--- My Registration Requests ---");
        if (registrations == null || registrations.isEmpty()) {
            System.out.println("You have no registration requests.");
            return;
        }
         String format = "%-7s | %-15s | %-10s | %s\n";
         System.out.printf(format, "Reg ID", "Project Name", "Status", "Request Date");
         System.out.println("-".repeat(60));
        for (HDBOfficerRegistration reg : registrations) {
            Project p = DataStore.getProjectById(reg.getProjectId());
             System.out.printf(format,
                 reg.getRegistrationId(),
                 (p != null ? p.getProjectName() : "N/A"),
                 reg.getStatus(),
                 utils.DateUtils.formatDate(reg.getRequestDate()));
        }
         System.out.println("-".repeat(60));
    }

     public void displayRegistrationResult(boolean success, String action) {
         if (success) CommonView.displaySuccess("Registration request " + action + " successfully.");
         else CommonView.displayError("Failed to " + action + " registration request."); // Service prints reason
     }


     // --- Enquiry Display/Input (Officer View) ---
     public void displayProjectEnquiryList(List<Enquiry> enquiries) {
        // Re-use ApplicantMenu display or customize if needed
        new ApplicantMenu().displayEnquiryList("Enquiries for Handling Project", enquiries);
    }

     public String getReplyInput() {
         return InputUtil.readString("Enter your reply: ");
     }

      public void displayReplyResult(boolean success) {
         if (success) CommonView.displaySuccess("Reply submitted successfully.");
         else CommonView.displayError("Failed to submit reply."); // Service prints reason
     }


     // --- Flat Booking ---
     public String getApplicantNricForBooking() {
         return InputUtil.readString("Enter Applicant's NRIC to assist with booking: ");
     }

      public void displayApplicationForBooking(BTOApplication application) {
           System.out.println("\n--- Application Found for Booking ---");
           if (application == null) {
               System.out.println(TextFormatUtil.error("No application with status SUCCESSFUL found for the given NRIC."));
               return;
           }
           // Reuse applicant view, or show specific booking-relevant info
           new ApplicantMenu().displayApplicationStatus(application);
      }

      public FlatType getFlatTypeForBooking(List<FlatType> availableTypes) {
           System.out.println(TextFormatUtil.info("\nApplicant '" + DataStore.getUserByNric(AuthStore.getCurrentUserNric()).getName() +"' is assisting with booking.")); // Clarify context
           // Reuse applicant selection logic
           return new ApplicantMenu().getFlatTypeSelection(availableTypes);
      }

      public boolean confirmBooking(FlatType type) {
          return InputUtil.readBooleanYN("Confirm booking for " + type.getDisplayName() + "? (y/n): ");
      }

       public void displayBookingResult(FlatBooking booking) {
          if (booking != null) {
               CommonView.displaySuccess("Flat booked successfully!");
               CommonView.displayMessage("Booking ID: " + booking.getBookingId());
               // Optionally display receipt immediately
               System.out.println("\nGenerating Receipt...");
               System.out.println(new FlatBookingServiceImpl().generateBookingReceipt(booking.getBookingId())); // Generate and print
          } else {
              CommonView.displayError("Failed to book flat."); // Service prints reason
          }
      }

      public int getBookingIdForReceipt() {
           return InputUtil.readInt("Enter the Booking ID to generate receipt: ");
      }

      public void displayReceipt(String receipt) {
           if (receipt.startsWith(TextFormatUtil.error(""))) { // Check if it's an error message
               CommonView.displayError(receipt);
           } else {
               System.out.println("\n--- Booking Receipt ---");
               System.out.println(receipt);
           }
      }

    // Reuse password change prompts from ApplicantMenu or CommonView
     public void displayPasswordChangePrompt() { new ApplicantMenu().displayPasswordChangePrompt(); }
     public String readOldPassword() { return new ApplicantMenu().readOldPassword(); }
     public String readNewPassword() { return new ApplicantMenu().readNewPassword(); }
     public String readConfirmNewPassword() { return new ApplicantMenu().readConfirmNewPassword(); }
     public void displayPasswordChangeSuccess() { new ApplicantMenu().displayPasswordChangeSuccess(); }
     public void displayPasswordChangeError(String message) { new ApplicantMenu().displayPasswordChangeError(message); }
}