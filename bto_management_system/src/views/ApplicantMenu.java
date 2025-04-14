package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*; // Import necessary models like Project, BTOApplication, Enquiry
import enums.FlatType;
import stores.DataStore; // Added for simple project name lookup in enquiry list

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ApplicantMenu {

     public int displayApplicantMenu() {
        CommonView.displayNavigationBar("Applicant Menu"); // Use common navbar
        System.out.println("1. View Eligible BTO Projects (Apply Filters)");
        System.out.println("2. Apply for BTO Project");
        System.out.println("3. View My Application Status");
        System.out.println("4. Withdraw Application");
        System.out.println("5. Manage Enquiries");
        System.out.println("6. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 6);
    }

    // Method to get filter criteria from user
    public Map<String, String> getProjectFilters() {
        Map<String, String> filters = new HashMap<>();
        System.out.println("\n--- Filter Projects (Leave blank to skip a filter) ---");
        String location = InputUtil.readString("Enter Neighborhood/Location: ");
        if (!location.isEmpty()) filters.put("location", location);

        String flatTypeStr = InputUtil.readString("Enter Flat Type (e.g., 2-Room, 3-Room): ");
         if (!flatTypeStr.isEmpty()) {
             // Validate flat type input loosely here or let service handle strict validation
             filters.put("flatType", flatTypeStr);
         }
        // Add more filters here if needed
        return filters;
    }


     public void displayProjectList(List<Project> projects) {
         System.out.println("\n--- Eligible BTO Projects ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No eligible projects found matching your criteria.");
             return;
         }
         // Improved Table Formatting
         String format = "%-5s | %-25s | %-15s | %-10s | %-10s | %-10s | %-10s\n";
         System.out.printf(format, "ID", "Project Name", "Neighborhood", "Open Date", "Close Date", "2-Room", "3-Room");
         System.out.println("-".repeat(95));
         for (Project p : projects) {
              System.out.printf(format,
                     p.getProjectId(),
                     p.getProjectName(),
                     p.getNeighborhood(),
                     utils.DateUtils.formatDate(p.getApplicationOpeningDate()),
                     utils.DateUtils.formatDate(p.getApplicationClosingDate()),
                     p.getAvailableUnits().getOrDefault(FlatType.TWO_ROOM, 0) + "/" + p.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0) , // Show Available/Total
                     p.getAvailableUnits().getOrDefault(FlatType.THREE_ROOM, 0) + "/" + p.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0));
         }
          System.out.println("-".repeat(95));
     }

     public int getProjectSelection(String prompt) {
         return InputUtil.readInt(prompt + " (Enter 0 to cancel): ");
     }

      public FlatType getFlatTypeSelection(List<FlatType> availableTypes) {
         if (availableTypes == null || availableTypes.isEmpty()) {
             System.out.println(TextFormatUtil.warning("No applicable flat types for selection in this project."));
             return null;
         }

         System.out.println("Select Flat Type:");
         for (int i = 0; i < availableTypes.size(); i++) {
             System.out.printf("%d. %s\n", i + 1, availableTypes.get(i).getDisplayName());
         }
         System.out.println("0. Cancel");

         int choice = InputUtil.readIntInRange("Enter choice: ", 0, availableTypes.size());
         if (choice == 0) {
             return null; // Cancelled
         }
         return availableTypes.get(choice - 1);
     }


     public void displayApplicationResult(BTOApplication application) {
         if (application != null) {
             CommonView.displaySuccess("Application submitted successfully!");
             displayApplicationStatus(application);
         } else {
             CommonView.displayError("Failed to submit application."); // Service layer prints specific reason
         }
     }

      public void displayApplicationStatus(BTOApplication application) {
          System.out.println("\n--- Application Status ---");
          if (application == null) {
              System.out.println("You do not have an active BTO application.");
              return;
          }
          Project project = DataStore.getProjectById(application.getProjectId()); // Need DataStore access or pass project details
          CommonView.displayMessage("Application ID: " + application.getApplicationId());
          CommonView.displayMessage("Project Name  : " + (project != null ? project.getProjectName() : "N/A (Project may be deleted)"));
          CommonView.displayMessage("Neighborhood  : " + (project != null ? project.getNeighborhood() : "N/A"));
          CommonView.displayMessage("Applied For   : " + application.getAppliedFlatType().getDisplayName());
          CommonView.displayMessage("Status        : " + TextFormatUtil.bold(application.getStatus().name()));
          if (application.getStatus() == BTOApplicationStatus.BOOKED) {
              CommonView.displayMessage("Booked Flat   : " + (application.getBookedFlatType() != null ? application.getBookedFlatType().getDisplayName() : "N/A"));
              CommonView.displayMessage("Booking ID    : " + (application.getFlatBookingId() != null ? application.getFlatBookingId() : "N/A"));
          }
          if (application.isWithdrawalRequested()) {
               CommonView.displayWarning("Withdrawal Requested - Pending Manager Approval");
          }
          System.out.println("--------------------------");
      }

       public boolean confirmWithdrawal() {
         return InputUtil.readBooleanYN("Are you sure you want to request withdrawal for your application? (y/n): ");
     }

      public void displayWithdrawalResult(boolean success) {
         if (success) {
             CommonView.displaySuccess("Withdrawal request submitted successfully. Pending manager approval.");
         } else {
             CommonView.displayError("Failed to submit withdrawal request."); // Service prints reason
         }
     }

     // --- Enquiry Management Sub-Menu ---
      public int displayEnquiryMenu() {
         CommonView.displayNavigationBar("Manage Enquiries");
         System.out.println("1. Submit New Enquiry");
         System.out.println("2. View My Enquiries");
         System.out.println("3. Edit Enquiry");
         System.out.println("4. Delete Enquiry");
         System.out.println("0. Back to Main Menu");
         return InputUtil.readIntInRange("Enter choice: ", 0, 4);
     }

      public int getEnquiryProjectIdInput(List<Project> allProjects) {
          System.out.println("\n--- Available Projects for Enquiry ---");
          // Display a simple list of all projects for selection
          if (allProjects == null || allProjects.isEmpty()) {
             System.out.println("No projects available in the system.");
             return -1; // Indicate no projects
          }
          String format = "%-5s | %s\n";
           System.out.printf(format, "ID", "Project Name");
           System.out.println("-".repeat(30));
           allProjects.forEach(p -> System.out.printf(format, p.getProjectId(), p.getProjectName()));
           System.out.println("-".repeat(30));
           return InputUtil.readInt("Enter the Project ID for your enquiry (or 0 to cancel): ");
      }

      public String getEnquiryContentInput() {
          return InputUtil.readString("Enter your enquiry: ");
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
          String format = "%-5s | %-10s | %-15s | %-40s | Replies\n";
          System.out.printf(format, "ID", "Status", "Project", "Enquiry Content");
           System.out.println("-".repeat(90));
          for (Enquiry e : enquiries) {
              Project p = DataStore.getProjectById(e.getProjectId());
               System.out.printf("%-5d | %-10s | %-15s | %-40s | %d\n",
                      e.getEnquiryId(),
                      e.getStatus(),
                      (p != null ? p.getProjectName() : "N/A"),
                      e.getContent().substring(0, Math.min(e.getContent().length(), 40)) + (e.getContent().length() > 40 ? "..." : ""),
                      e.getReplies().size());

                // Optionally show replies immediately or require viewing details
                 if (!e.getReplies().isEmpty()) {
                    System.out.println("      Replies:");
                    e.getReplies().forEach(r -> System.out.println("        - " + r));
                 }
          }
          System.out.println("-".repeat(90));
      }

       public int getEnquiryIdToManage(String action) { // action = "edit", "delete", "reply", "view details"
           return InputUtil.readInt("Enter the Enquiry ID to " + action + " (or 0 to cancel): ");
       }

       public String getEditedEnquiryContentInput() {
           return InputUtil.readString("Enter the new enquiry text: ");
       }

        public void displayEditEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry updated successfully.");
         else CommonView.displayError("Failed to update enquiry."); // Service prints reason
     }

       public boolean confirmDeleteEnquiry() {
         return InputUtil.readBooleanYN("Are you sure you want to delete this enquiry? (y/n): ");
     }

        public void displayDeleteEnquiryResult(boolean success) {
         if (success) CommonView.displaySuccess("Enquiry deleted successfully.");
         else CommonView.displayError("Failed to delete enquiry."); // Service prints reason
     }

     // Methods for password change prompts (can be shared in CommonView or UserController view)
      public void displayPasswordChangePrompt() {
         System.out.println("\n--- Change Password ---");
     }
      public String readOldPassword() {
         return InputUtil.readString("Enter Old Password: ");
     }
      public String readNewPassword() {
         return InputUtil.readString("Enter New Password: ");
     }
      public String readConfirmNewPassword() {
         return InputUtil.readString("Confirm New Password: ");
     }
      public void displayPasswordChangeSuccess() {
         CommonView.displaySuccess("Password changed successfully. Please log in again.");
     }
      public void displayPasswordChangeError(String message) {
        CommonView.displayError("Password change failed: " + message);
    }
}