package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.FlatType;
import stores.DataStore; // Used for simple lookups in display methods

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the display and input for the Applicant user interface.
 * Implements PasswordChangeView for the password change functionality.
 */
public class ApplicantMenu implements controllers.UserController.PasswordChangeView {

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
     * Prompts the user for project filter criteria.
     * @return A map containing the entered filters.
     */
    public Map<String, String> getProjectFilters() {
        Map<String, String> filters = new HashMap<>();
        System.out.println("\n--- Filter Projects (Leave blank to skip a filter) ---");
        String location = InputUtil.readString("Enter Neighborhood/Location: ");
        if (!location.isEmpty()) filters.put("location", location.trim());

        String flatTypeStr = InputUtil.readString("Enter Flat Type (e.g., 2-Room, 3-Room): ");
         if (!flatTypeStr.isEmpty()) {
             if (FlatType.fromDisplayName(flatTypeStr.trim()) != null) {
                filters.put("flatType", flatTypeStr.trim());
             } else {
                 CommonView.displayWarning("Invalid flat type entered for filter, ignoring.");
             }
         }
        return filters;
    }

    /**
     * Displays a list of BTO projects in a formatted table.
     * @param projects The list of Project objects to display.
     */
     public void displayProjectList(List<Project> projects) {
         System.out.println("\n--- Eligible BTO Projects ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No eligible projects found matching your criteria.");
             return;
         }
         String headerFormat = "%-5s | %-25s | %-15s | %-10s | %-10s | %-10s | %-10s\n";
         String rowFormat    = "%-5d | %-25s | %-15s | %-10s | %-10s | %-10s | %-10s\n";
         CommonView.displayTableHeader(headerFormat,"ID", "Project Name", "Neighborhood", "Open Date", "Close Date", "2-Room", "3-Room");
         for (Project p : projects) {
             String twoRoomInfo = p.getAvailableUnits().getOrDefault(FlatType.TWO_ROOM, 0) + "/" + p.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0);
             String threeRoomInfo = p.getAvailableUnits().getOrDefault(FlatType.THREE_ROOM, 0) + "/" + p.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0);
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
       * @param availableTypes List of FlatType enums applicable for selection.
       * @return The selected FlatType, or null if cancelled.
       */
      public FlatType getFlatTypeSelection(List<FlatType> availableTypes) {
         if (availableTypes == null || availableTypes.isEmpty()) {
             CommonView.displayWarning("No applicable flat types for selection in this project.");
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
          String projectName = (project != null ? project.getProjectName() : "N/A (Project data unavailable)");
          String neighborhood = (project != null ? project.getNeighborhood() : "N/A");

          CommonView.displayMessage(String.format("%-15s: %d", "Application ID", application.getApplicationId()));
          CommonView.displayMessage(String.format("%-15s: %s", "Project Name", projectName));
          CommonView.displayMessage(String.format("%-15s: %s", "Neighborhood", neighborhood));
          CommonView.displayMessage(String.format("%-15s: %s", "Applied For", application.getAppliedFlatType().getDisplayName()));
          CommonView.displayMessage(String.format("%-15s: %s", "Submit Date", utils.DateUtils.formatDate(application.getSubmissionDate())));
          CommonView.displayMessage(String.format("%-15s: %s", "Status", TextFormatUtil.bold(application.getStatus().name())));

          if (application.getStatus() == BTOApplicationStatus.BOOKED) {
              CommonView.displayMessage(String.format("%-15s: %s", "Booked Flat", (application.getBookedFlatType() != null ? application.getBookedFlatType().getDisplayName() : "N/A")));
              CommonView.displayMessage(String.format("%-15s: %s", "Booking ID", (application.getFlatBookingId() != null ? application.getFlatBookingId() : "N/A")));
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
      /**
       * Displays the menu for managing enquiries.
       * @return The user's menu choice.
       */
      public int displayEnquiryMenu() {
         CommonView.displayNavigationBar("Manage Enquiries");
         System.out.println("1. Submit New Enquiry");
         System.out.println("2. View My Enquiries");
         System.out.println("3. Edit My Enquiry");
         System.out.println("4. Delete My Enquiry");
         System.out.println("0. Back to Main Menu");
         return InputUtil.readIntInRange("Enter choice: ", 0, 4);
     }

      /**
       * Displays available projects and prompts for selection for submitting an enquiry.
       * @param allProjects List of all projects in the system.
       * @return Project ID selected, or <= 0 if cancelled or none available.
       */
      public int getEnquiryProjectIdInput(List<Project> allProjects) {
          System.out.println("\n--- Available Projects for Enquiry ---");
          if (allProjects == null || allProjects.isEmpty()) {
             System.out.println("No projects currently in the system.");
             return -1;
          }
          String format = "%-5s | %s\n";
          CommonView.displayTableHeader(format, "ID", "Project Name");
          allProjects.forEach(p -> CommonView.displayTableRow(format, p.getProjectId(), p.getProjectName()));
          return InputUtil.readInt("Enter the Project ID for your enquiry (or 0 to cancel): ");
      }

      /**
       * Prompts for and reads the content of a new enquiry.
       * @return The enquiry text entered by the user.
       */
      public String getEnquiryContentInput() {
          return InputUtil.readString("Enter your enquiry details: ");
      }

       /**
        * Displays the result of submitting an enquiry.
        * @param success true if submission was successful, false otherwise.
        */
       public void displaySubmitEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry submitted successfully.");
         else CommonView.displayError("Failed to submit enquiry.");
     }

      /**
       * Displays a list of enquiries in a formatted table, including replies.
       * @param title The title for the enquiry list section.
       * @param enquiries The list of Enquiry objects to display.
       */
      public void displayEnquiryList(String title, List<Enquiry> enquiries) {
          System.out.println("\n--- " + title + " ---");
          if (enquiries == null || enquiries.isEmpty()) {
              System.out.println("No enquiries found.");
              return;
          }
          String headerFormat = "%-5s | %-10s | %-15s | %-40s | %s\n";
          String rowFormat    = "%-5d | %-10s | %-15s | %-40s | %d\n";
          CommonView.displayTableHeader(headerFormat, "ID", "Status", "Project", "Enquiry Content (Preview)", "Replies");

          for (Enquiry e : enquiries) {
              Project p = DataStore.getProjectById(e.getProjectId());
              String projectName = (p != null ? p.getProjectName() : "N/A");
              String contentPreview = e.getContent().substring(0, Math.min(e.getContent().length(), 40)) + (e.getContent().length() > 40 ? "..." : "");
              CommonView.displayTableRow(rowFormat,
                      e.getEnquiryId(),
                      e.getStatus(),
                      projectName,
                      contentPreview,
                      e.getReplies().size());

                 if (!e.getReplies().isEmpty()) {
                    for (String reply : e.getReplies()) {
                        // Indent replies for better readability
                        System.out.println("      \u21B3 " + TextFormatUtil.info(reply)); // Use info color for replies
                    }
                    System.out.println(); // Add a blank line after replies for separation
                 } else {
                    // Add a blank line even if no replies to maintain spacing
                    System.out.println();
                 }
          }
      }

       /**
        * Prompts the user for an Enquiry ID for a specific management action.
        * @param action The action being performed (e.g., "edit", "delete", "reply").
        * @return The Enquiry ID entered, or 0 to cancel.
        */
       public int getEnquiryIdToManage(String action) {
           return InputUtil.readInt("Enter the Enquiry ID to " + action + " (or 0 to cancel): ");
       }

       /**
        * Prompts for and reads the updated content for an enquiry being edited.
        * @return The new enquiry text.
        */
       public String getEditedEnquiryContentInput() {
           return InputUtil.readString("Enter the new enquiry text: ");
       }

        /**
         * Displays the result of an attempt to edit an enquiry.
         * @param success true if editing was successful, false otherwise.
         */
        public void displayEditEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry updated successfully.");
         else CommonView.displayError("Failed to update enquiry.");
     }

       /**
        * Prompts the user to confirm deleting an enquiry.
        * @return true if the user confirms (y), false otherwise (n).
        */
       public boolean confirmDeleteEnquiry() {
         return InputUtil.readBooleanYN("Are you sure you want to delete this enquiry permanently? (y/n): ");
     }

        /**
         * Displays the result of an attempt to delete an enquiry.
         * @param success true if deletion was successful, false otherwise.
         */
        public void displayDeleteEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry deleted successfully.");
         else CommonView.displayError("Failed to delete enquiry.");
     }

     // --- Password Change Methods (Implementation of PasswordChangeView) ---
     @Override public void displayPasswordChangePrompt() { System.out.println("\n--- Change Password ---"); CommonView.displayMessage("Note: Default password is 'password'."); }
     @Override public String readOldPassword() { return InputUtil.readString("Enter Old Password: "); }
     @Override public String readNewPassword() { return InputUtil.readString("Enter New Password: "); }
     @Override public String readConfirmNewPassword() { return InputUtil.readString("Confirm New Password: "); }
     @Override public void displayPasswordChangeSuccess() { CommonView.displaySuccess("Password changed successfully. Please log in again."); }
     @Override public void displayPasswordChangeError(String message) { CommonView.displayError("Password change failed: " + message); }
}