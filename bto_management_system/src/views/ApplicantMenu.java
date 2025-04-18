package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.FlatType;
import enums.BTOApplicationStatus;
import stores.DataStore; // Used for simple lookups in display methods

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator; // Import comparator for sorting

/**
 * Handles the display and input for the Applicant user interface.
 * Implements PasswordChangeView for the password change functionality.
 */
public class ApplicantMenu implements controllers.UserController.PasswordChangeView { // Implement interface

     /**
      * Displays the main menu for Applicants and gets their choice.
      * @return The integer choice selected by the user.
      */
     public int displayApplicantMenu() {
        CommonView.displayNavigationBar("Applicant Menu");
        System.out.println("1. View Eligible BTO Projects (Apply Filters)");
        System.out.println("2. Apply for BTO Project");
        System.out.println("3. View My Application Status");
        System.out.println("4. Request Application Withdrawal");
        System.out.println("5. Manage Enquiries");
        System.out.println("6. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 6);
    }

    /**
     * Prompts the user for project filter criteria. Uses readStringAllowEmpty.
     * @return A map containing the entered filters.
     */
     public Map<String, String> getProjectFilters() {
         Map<String, String> filters = new HashMap<>();
         System.out.println("\n--- Filter Projects (Press Enter to skip a filter) ---");

         String locationInput = InputUtil.readStringAllowEmpty("Enter Neighborhood/Location: ");
         String location = locationInput.trim();
         if (!location.isEmpty()) {
             filters.put("location", location);
         }

         String flatTypeInput = InputUtil.readStringAllowEmpty("Enter Flat Type (e.g., 2-Room, 3-Room): ");
         String flatTypeStr = flatTypeInput.trim();
          if (!flatTypeStr.isEmpty()) {
              FlatType validatedType = FlatType.fromDisplayName(flatTypeStr); // Uses helper to check validity
              if (validatedType != null) {
                  // Store the VALIDATED display name
                  filters.put("flatType", validatedType.getDisplayName());
              } else {
                  if (!flatTypeInput.isEmpty()) {
                     CommonView.displayWarning("Invalid flat type '" + flatTypeStr + "' entered for filter, ignoring this filter.");
                  }
              }
          }
         return filters;
     }

    /**
     * Displays a list of BTO projects in a formatted table.
     * @param projects The list of Project objects to display.
     */
     public void displayProjectList(List<Project> projects) {
         System.out.println("\n--- BTO Projects Available for Application ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No projects found matching your criteria.");
             return;
         }
         String headerFormat = "%-5s | %-25s | %-15s | %-10s | %-10s | %-11s | %-11s\n";
         String rowFormat    = "%-5d | %-25s | %-15s | %-10s | %-10s | %-11s | %-11s\n";
         CommonView.displayTableHeader(headerFormat,"ID", "Project Name", "Neighborhood", "Open Date", "Close Date", "2-Room(A/T)", "3-Room(A/T)");
         for (Project p : projects) {
             String twoRoomInfo = p.getAvailableUnits(FlatType.TWO_ROOM) + "/" + p.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0);
             String threeRoomInfo = p.getAvailableUnits(FlatType.THREE_ROOM) + "/" + p.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0);
              CommonView.displayTableRow(rowFormat,
                     p.getProjectId(),
                     p.getProjectName(),
                     p.getNeighborhood(),
                     utils.DateUtils.formatDate(p.getApplicationOpeningDate()),
                     utils.DateUtils.formatDate(p.getApplicationClosingDate()),
                     twoRoomInfo,
                     threeRoomInfo);
         }
     }

     /**
      * Prompts the user to select a Project ID for a specific action.
      * @param prompt The action being performed.
      * @return The selected Project ID, or 0 if cancelled.
      */
     public int getProjectSelection(String prompt) {
         return InputUtil.readInt(prompt + " (Enter Project ID, or 0 to cancel): ");
     }

      /**
       * Prompts the user to select a flat type from a list of available options.
       * @param availableTypes List of FlatType enums applicable for selection based on eligibility.
       * @return The selected FlatType, or null if cancelled.
       */
      public FlatType getFlatTypeSelection(List<FlatType> availableTypes) {
         if (availableTypes == null || availableTypes.isEmpty()) {
             CommonView.displayWarning("No applicable flat types for selection in this project based on your eligibility.");
             return null;
         }

         System.out.println("\nSelect Applicable Flat Type:");
         for (int i = 0; i < availableTypes.size(); i++) {
             System.out.printf("%d. %s\n", i + 1, availableTypes.get(i).getDisplayName());
         }
         System.out.println("0. Cancel Application");

         int choice = InputUtil.readIntInRange("Enter choice: ", 0, availableTypes.size());
         if (choice == 0) {
             System.out.println("Application cancelled.");
             return null;
         }
         return availableTypes.get(choice - 1);
     }

     /**
      * Displays the result of a BTO application attempt.
      * @param application The BTOApplication object if successful, null otherwise.
      */
     public void displayApplicationResult(BTOApplication application) {
         if (application != null) {
             CommonView.displaySuccess("Application submitted successfully!");
             displayApplicationStatus(application);
         } else {
             CommonView.displayError("Failed to submit application.");
         }
     }

      /**
       * Displays the detailed status of a specific BTO application.
       * @param application The BTOApplication object to display.
       */
      public void displayApplicationStatus(BTOApplication application) {
          System.out.println("\n--- Your BTO Application Status ---");
          if (application == null) {
              System.out.println("You do not have an active BTO application.");
              return;
          }
          Project project = DataStore.getProjectById(application.getProjectId());
          String projectName = (project != null ? project.getProjectName() : "N/A (Project data missing)");
          String neighborhood = (project != null ? project.getNeighborhood() : "N/A");

          String format = "%-15s: %s\n";
          System.out.printf(format, "Application ID", application.getApplicationId());
          System.out.printf(format, "Project Name", projectName);
          System.out.printf(format, "Neighborhood", neighborhood);
          System.out.printf(format, "Applied For", application.getAppliedFlatType().getDisplayName());
          System.out.printf(format, "Submit Date", utils.DateUtils.formatDate(application.getSubmissionDate()));
          System.out.printf(format, "Status", TextFormatUtil.bold(application.getStatus().name()));

          if (application.getStatus() == BTOApplicationStatus.BOOKED) {
              System.out.printf(format, "Booked Flat", (application.getBookedFlatType() != null ? application.getBookedFlatType().getDisplayName() : "N/A"));
              System.out.printf(format, "Booking ID", (application.getFlatBookingId() != null ? application.getFlatBookingId() : "N/A"));
          }
          if (application.isWithdrawalRequested()) {
               CommonView.displayWarning("NOTE: Withdrawal Requested - Pending Manager Approval");
          }
          System.out.println("------------------------------------");
      }

       /**
        * Prompts the user to confirm application withdrawal.
        * @return true if the user confirms (y), false otherwise (n).
        */
       public boolean confirmWithdrawal() {
         return InputUtil.readBooleanYN("Are you sure you want to request withdrawal for this application? (y/n): ");
     }

      /**
       * Displays the result of submitting a withdrawal request.
       * @param success true if the request was submitted, false otherwise.
       */
      public void displayWithdrawalResult(boolean success) {
         if (success) {
             CommonView.displaySuccess("Withdrawal request submitted successfully. This is pending manager approval.");
         } else {
             CommonView.displayError("Failed to submit withdrawal request.");
         }
     }

     // --- Enquiry Management ---
      public int displayEnquiryMenu() {
         CommonView.displayNavigationBar("Manage Enquiries");
         System.out.println("1. Submit New Enquiry");
         System.out.println("2. View My Enquiries");
         System.out.println("3. Edit My Enquiry");
         System.out.println("4. Delete My Enquiry");
         System.out.println("0. Back to Main Menu");
         return InputUtil.readIntInRange("Enter choice: ", 0, 4);
     }

      public int getEnquiryProjectIdInput(List<Project> allProjects) {
          System.out.println("\n--- Available Projects for Enquiry ---");
          if (allProjects == null || allProjects.isEmpty()) {
             System.out.println("No projects currently in the system.");
             return -1;
          }
          String headerFormat = "%-5s | %s\n";
          String rowFormat    = "%-5d | %s\n";
          CommonView.displayTableHeader(headerFormat,"ID", "Project Name");
          allProjects.sort(Comparator.comparingInt(Project::getProjectId));
          allProjects.forEach(p -> CommonView.displayTableRow(rowFormat, p.getProjectId(), p.getProjectName()));
          return InputUtil.readInt("Enter the Project ID for your enquiry (or 0 to cancel): ");
      }

      public String getEnquiryContentInput() {
          return InputUtil.readString("Enter your enquiry details: ");
      }

       public void displaySubmitEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry submitted successfully.");
         else CommonView.displayError("Failed to submit enquiry.");
     }

      public void displayEnquiryList(String title, List<Enquiry> enquiries) {
          System.out.println("\n--- " + title + " ---");
          if (enquiries == null || enquiries.isEmpty()) {
              System.out.println("No enquiries found.");
              return;
          }
          String headerFormat = "%-5s | %-10s | %-15s | %-40s | %s\n";
          String rowFormat    = "%-5d | %-10s | %-15s | %-40s | %d\n";
          CommonView.displayTableHeader(headerFormat, "ID", "Status", "Project", "Enquiry Content (Preview)", "Replies");
          enquiries.sort(Comparator.comparingInt(Enquiry::getEnquiryId));
          for (Enquiry e : enquiries) {
              Project p = DataStore.getProjectById(e.getProjectId());
              String projectName = (p != null ? p.getProjectName() : "N/A");
              int previewLength = Math.min(e.getContent().length(), 40);
              String contentPreview = e.getContent().substring(0, previewLength) + (e.getContent().length() > 40 ? "..." : "");
              CommonView.displayTableRow(rowFormat,
                      e.getEnquiryId(),
                      e.getStatus(),
                      projectName,
                      contentPreview,
                      e.getReplies().size());
                 if (!e.getReplies().isEmpty()) {
                    for (String reply : e.getReplies()) {
                        System.out.println("      \u21B3 " + TextFormatUtil.info(reply));
                    }
                    System.out.println();
                 } else {
                     System.out.println();
                 }
          }
      }

       public int getEnquiryIdToManage(String action) {
           return InputUtil.readInt("Enter the Enquiry ID to " + action + " (or 0 to cancel): ");
       }

       public String getEditedEnquiryContentInput() {
           return InputUtil.readString("Enter the new enquiry text: ");
       }

        public void displayEditEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry updated successfully.");
         else CommonView.displayError("Failed to update enquiry.");
     }

       public boolean confirmDeleteEnquiry() {
         return InputUtil.readBooleanYN("Are you sure you want to delete this enquiry permanently? (y/n): ");
     }

        public void displayDeleteEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry deleted successfully.");
         else CommonView.displayError("Failed to delete enquiry.");
     }

     // --- Password Change Methods ---
     @Override public void displayPasswordChangePrompt() { System.out.println("\n--- Change Password ---"); CommonView.displayMessage("Note: Default password is 'password'."); }
     @Override public String readOldPassword() { return InputUtil.readString("Enter Old Password: "); }
     @Override public String readNewPassword() { return InputUtil.readString("Enter New Password: "); }
     @Override public String readConfirmNewPassword() { return InputUtil.readString("Confirm New Password: "); }
     @Override public void displayPasswordChangeSuccess() { CommonView.displaySuccess("Password changed successfully. Please log in again."); }
     @Override public void displayPasswordChangeError(String message) { CommonView.displayError("Password change failed: " + message); }
}