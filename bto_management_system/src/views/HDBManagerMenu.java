package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import models.*;
import enums.*;
import stores.DataStore; // For lookups in display methods

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HDBManagerMenu {

    public int displayManagerMenu() {
        CommonView.displayNavigationBar("HDB Manager Menu");
        System.out.println("--- Project Management ---");
        System.out.println("1. Create New BTO Project");
        System.out.println("2. View/Edit/Delete My Managed Projects");
        System.out.println("3. View All Projects");
        System.out.println("4. Toggle Project Visibility");
        System.out.println("--- Approvals ---");
        System.out.println("5. Manage HDB Officer Registrations");
        System.out.println("6. Manage BTO Applications");
        System.out.println("7. Manage Application Withdrawals");
        System.out.println("--- Enquiries ---");
        System.out.println("8. View Enquiries (All / By Project)");
        System.out.println("9. Reply to Enquiry (For Handled Project)");
        System.out.println("--- Reporting ---");
        System.out.println("10. Generate Booking Report");
        System.out.println("--- Account ---");
        System.out.println("11. Change Password");
        System.out.println("0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 11);
    }

    // --- Project CRUD ---
     public Project getNewProjectDetails(String managerNric) {
         System.out.println("\n--- Create New BTO Project ---");
         String name = InputUtil.readString("Enter Project Name: ");
         String neighborhood = InputUtil.readString("Enter Neighborhood: ");
         Map<FlatType, Integer> units = new HashMap<>();
         units.put(FlatType.TWO_ROOM, InputUtil.readIntInRange("Enter total number of 2-Room units: ", 0, 9999));
         units.put(FlatType.THREE_ROOM, InputUtil.readIntInRange("Enter total number of 3-Room units: ", 0, 9999));
         // Add more flat types if system expands

         Date openDate = InputUtil.readDate("Enter Application Opening Date");
         Date closeDate = InputUtil.readDate("Enter Application Closing Date");
         // Basic validation in view, service does more thorough check
          if (openDate != null && closeDate != null && openDate.after(closeDate)) {
             CommonView.displayError("Closing date cannot be before opening date.");
             return null; // Indicate error to controller
         }

         int slots = InputUtil.readIntInRange("Enter Max HDB Officer Slots (1-10): ", 1, 10);

         // Return a temporary Project object or a Map of details for the controller/service
          // Using a temporary object allows using the constructor's logic potentially
          // but might require a different constructor or setting fields manually.
          // Let's return a Map for simplicity here. Or controller calls service directly with these args.
          // Returning null here signals we'll get details and pass to controller/service.
           return new Project(name, neighborhood, units, openDate, closeDate, managerNric, slots); // Use constructor, ID will be set by service
     }

     public void displayCreateProjectResult(Project project) {
         if (project != null) {
             CommonView.displaySuccess("Project '" + project.getProjectName() + "' created successfully (ID: " + project.getProjectId() + ").");
         } else {
             CommonView.displayError("Failed to create project."); // Service prints reason
         }
     }

     public void displayProjectList(String title, List<Project> projects) {
         System.out.println("\n--- " + title + " ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No projects found.");
             return;
         }
         // Similar format as Applicant view, maybe add Manager NRIC?
          String format = "%-5s | %-25s | %-15s | %-10s | %-10s | %-8s | %-10s | %s\n";
          System.out.printf(format, "ID", "Project Name", "Neighborhood", "Open Date", "Close Date", "Visible", "Slots", "Manager");
          System.out.println("-".repeat(110));
          for (Project p : projects) {
              System.out.printf(format,
                     p.getProjectId(),
                     p.getProjectName(),
                     p.getNeighborhood(),
                     utils.DateUtils.formatDate(p.getApplicationOpeningDate()),
                     utils.DateUtils.formatDate(p.getApplicationClosingDate()),
                     p.isVisible() ? "Yes" : "No",
                     p.getCurrentOfficerCount() + "/" + p.getMaxOfficerSlots(),
                     p.getAssignedHDBManagerNric());
         }
          System.out.println("-".repeat(110));
     }

      public int getProjectIdToManage(String action) { // action: "View Details", "Edit", "Delete", "Toggle Visibility", "Manage Officers", etc.
          return InputUtil.readInt("Enter the Project ID to " + action + " (or 0 to cancel): ");
      }

      // For Editing - get updated details (similar to create, but maybe pre-fill?)
      public Project getEditedProjectDetails(Project existingProject) {
           System.out.println("\n--- Editing Project: " + existingProject.getProjectName() + " (ID: " + existingProject.getProjectId() + ") ---");
           System.out.println("Enter new details (leave blank to keep current value where applicable):");

           String name = InputUtil.readString("Project Name [" + existingProject.getProjectName() + "]: ");
           String neighborhood = InputUtil.readString("Neighborhood [" + existingProject.getNeighborhood() + "]: ");

           // Editing units might be restricted
           boolean appsExist = DataStore.getApplications().values().stream().anyMatch(a -> a.getProjectId() == existingProject.getProjectId());
           Map<FlatType, Integer> units = existingProject.getTotalUnits(); // Default to existing
            if (!appsExist) {
                 System.out.println("Note: Applications have not started, unit counts can be changed.");
                 Map<FlatType, Integer> newUnits = new HashMap<>();
                 String twoRoomStr = InputUtil.readString("Total 2-Room units [" + units.getOrDefault(FlatType.TWO_ROOM, 0) + "]: ");
                 newUnits.put(FlatType.TWO_ROOM, twoRoomStr.isEmpty() ? units.getOrDefault(FlatType.TWO_ROOM, 0) : InputUtil.safeParseInt(twoRoomStr, -1)); // Use helper if needed

                 String threeRoomStr = InputUtil.readString("Total 3-Room units [" + units.getOrDefault(FlatType.THREE_ROOM, 0) + "]: ");
                 newUnits.put(FlatType.THREE_ROOM, threeRoomStr.isEmpty() ? units.getOrDefault(FlatType.THREE_ROOM, 0) : InputUtil.safeParseInt(threeRoomStr, -1));

                  // Validate non-negative
                  if (newUnits.values().stream().anyMatch(v -> v < 0)) {
                      CommonView.displayError("Unit counts cannot be negative. Aborting edit.");
                      return null;
                  }
                  units = newUnits; // Update if valid and no apps exist
            } else {
                System.out.println(TextFormatUtil.warning("Applications exist for this project. Total unit counts cannot be changed."));
            }


           String openDateStr = InputUtil.readString("App Opening Date (yyyy-MM-dd) [" + utils.DateUtils.formatDate(existingProject.getApplicationOpeningDate()) + "]: ");
           String closeDateStr = InputUtil.readString("App Closing Date (yyyy-MM-dd) [" + utils.DateUtils.formatDate(existingProject.getApplicationClosingDate()) + "]: ");
           String slotsStr = InputUtil.readString("Max Officer Slots (1-10) [" + existingProject.getMaxOfficerSlots() + "]: ");

           // Use existing values if input is blank
           Date openDate = openDateStr.isEmpty() ? existingProject.getApplicationOpeningDate() : utils.DateUtils.parseDate(openDateStr);
           Date closeDate = closeDateStr.isEmpty() ? existingProject.getApplicationClosingDate() : utils.DateUtils.parseDate(closeDateStr);
           int slots = slotsStr.isEmpty() ? existingProject.getMaxOfficerSlots() : InputUtil.safeParseInt(slotsStr, -1); // Use helper

           // Create a temporary object with updated values for the service layer validation
            Project updated = new Project(
                name.isEmpty() ? existingProject.getProjectName() : name,
                neighborhood.isEmpty() ? existingProject.getNeighborhood() : neighborhood,
                units, // Use potentially updated units
                openDate,
                closeDate,
                existingProject.getAssignedHDBManagerNric(), // Manager doesn't change
                slots);
             updated.setVisibility(existingProject.isVisible()); // Keep existing visibility

             // The service layer will handle actual validation and saving using this data
             return updated;
      }

      public void displayEditProjectResult(boolean success) {
           if(success) CommonView.displaySuccess("Project details updated successfully.");
           else CommonView.displayError("Failed to update project details."); // Service prints reason
      }

      public boolean confirmDeleteProject(String projectName) {
          return InputUtil.readBooleanYN("Are you absolutely sure you want to DELETE project '" + projectName + "' and all its associated data? (y/n): ");
      }

       public void displayDeleteProjectResult(boolean success) {
           if(success) CommonView.displaySuccess("Project deleted successfully.");
           else CommonView.displayError("Failed to delete project."); // Service prints reason
      }

       public boolean getVisibilityToggleChoice(boolean currentVisibility) {
            System.out.println("Current visibility is: " + (currentVisibility ? "ON" : "OFF"));
            return InputUtil.readBooleanYN("Set visibility to " + (currentVisibility ? "OFF" : "ON") + "? (y/n): ");
       }

        public void displayToggleVisibilityResult(boolean success, boolean newState) {
            if (success) CommonView.displaySuccess("Project visibility set to " + (newState ? "ON" : "OFF") + ".");
            else CommonView.displayError("Failed to toggle project visibility."); // Service prints reason
       }


     // --- Approval Menus ---
     public int displayOfficerRegistrationMenu() {
         CommonView.displayNavigationBar("Manage Officer Registrations");
         System.out.println("1. View Pending Registrations for My Project");
         System.out.println("2. Approve Registration");
         System.out.println("3. Reject Registration");
         System.out.println("0. Back to Main Menu");
         return InputUtil.readIntInRange("Enter choice: ", 0, 3);
     }

      public void displayOfficerRegistrationList(String title, List<HDBOfficerRegistration> registrations) {
         System.out.println("\n--- " + title + " ---");
         if (registrations == null || registrations.isEmpty()) {
             System.out.println("No registrations found.");
             return;
         }
          String format = "%-7s | %-15s | %-15s | %-10s | %s\n";
          System.out.printf(format, "Reg ID", "Officer Name", "Officer NRIC", "Status", "Request Date");
          System.out.println("-".repeat(75));
         for (HDBOfficerRegistration reg : registrations) {
             User officer = DataStore.getUserByNric(reg.getOfficerNric());
              System.out.printf(format,
                  reg.getRegistrationId(),
                  (officer != null ? officer.getName() : "N/A"),
                  reg.getOfficerNric(),
                  reg.getStatus(),
                  utils.DateUtils.formatDate(reg.getRequestDate()));
         }
          System.out.println("-".repeat(75));
     }

     public int getRegistrationIdToManage(String action) { // action: "Approve", "Reject"
         return InputUtil.readInt("Enter Registration ID to " + action + " (or 0 to cancel): ");
     }

      public void displayOfficerApprovalResult(boolean success, String action) { // action: "approved", "rejected"
         if (success) CommonView.displaySuccess("Officer registration " + action + " successfully.");
         else CommonView.displayError("Failed to " + action + " officer registration."); // Service prints reason
     }

      // --- BTO Application Approval ---
       public int displayBTOApplicationMenu() {
         CommonView.displayNavigationBar("Manage BTO Applications");
         System.out.println("1. View Pending Applications for My Project");
         System.out.println("2. Approve Application");
         System.out.println("3. Reject Application");
         // View Successful/Rejected/Booked? Maybe add later.
         System.out.println("0. Back to Main Menu");
         return InputUtil.readIntInRange("Enter choice: ", 0, 3);
     }

      public void displayBTOApplicationList(String title, List<BTOApplication> applications) {
           System.out.println("\n--- " + title + " ---");
           if (applications == null || applications.isEmpty()) {
               System.out.println("No applications found.");
               return;
           }
            String format = "%-7s | %-15s | %-9s | %-8s | %-10s | %s\n";
            System.out.printf(format, "App ID", "Applicant Name", "NRIC", "Age", "M.Status", "Applied Type");
            System.out.println("-".repeat(80));
           for (BTOApplication app : applications) {
               User applicant = DataStore.getUserByNric(app.getApplicantNric());
                System.out.printf(format,
                    app.getApplicationId(),
                    (applicant != null ? applicant.getName() : "N/A"),
                    app.getApplicantNric(),
                    (applicant != null ? applicant.getAge() : "N/A"),
                    (applicant != null ? applicant.getMaritalStatus() : "N/A"),
                    app.getAppliedFlatType().getDisplayName()
                );
           }
            System.out.println("-".repeat(80));
      }

       public int getApplicationIdToManage(String action) { // action: "Approve", "Reject"
           return InputUtil.readInt("Enter Application ID to " + action + " (or 0 to cancel): ");
       }

        public void displayBTOApprovalResult(boolean success, String action) { // action: "approved", "rejected"
           if (success) CommonView.displaySuccess("BTO Application " + action + " successfully.");
           else CommonView.displayError("Failed to " + action + " BTO application."); // Service prints reason
       }

        // --- Withdrawal Approval ---
        public int displayWithdrawalMenu() {
             CommonView.displayNavigationBar("Manage Application Withdrawals");
             System.out.println("1. View Pending Withdrawal Requests for My Project");
             System.out.println("2. Approve Withdrawal");
             System.out.println("3. Reject Withdrawal");
             System.out.println("0. Back to Main Menu");
             return InputUtil.readIntInRange("Enter choice: ", 0, 3);
         }

         // Can reuse displayBTOApplicationList, just filter for isWithdrawalRequested() == true in controller

         public void displayWithdrawalApprovalResult(boolean success, String action) { // action: "approved", "rejected"
            if (success) CommonView.displaySuccess("Application withdrawal request " + action + " successfully.");
            else CommonView.displayError("Failed to " + action + " withdrawal request."); // Service prints reason
        }


     // --- Enquiries ---
     // Can reuse ApplicantMenu's displayEnquiryList and getEnquiryIdToManage
     // Reuse HDBOfficerMenu's getReplyInput and displayReplyResult

      public int displayEnquiryViewChoice() {
          System.out.println("\n--- View Enquiries ---");
          System.out.println("1. View Enquiries for a Specific Project I Manage");
          System.out.println("2. View Enquiries for ALL Projects");
          System.out.println("0. Back");
          return InputUtil.readIntInRange("Enter choice: ", 0, 2);
      }


     // --- Reporting ---
     public Map<String, String> getReportFilters() {
         Map<String, String> filters = new HashMap<>();
         System.out.println("\n--- Generate Booking Report (Leave blank to skip filter) ---");

         String projName = InputUtil.readString("Filter by Project Name: ");
         if (!projName.isEmpty()) filters.put("projectName", projName);

          String flatTypeStr = InputUtil.readString("Filter by Flat Type (e.g., 2-Room): ");
         if (!flatTypeStr.isEmpty()) filters.put("flatType", flatTypeStr);

         String maritalStatusStr = InputUtil.readString("Filter by Marital Status (SINGLE/MARRIED): ");
         if (!maritalStatusStr.isEmpty()) filters.put("maritalStatus", maritalStatusStr);

         // Add age range filters if needed
         String minAgeStr = InputUtil.readString("Filter by Minimum Age: ");
         if (!minAgeStr.isEmpty()) filters.put("minAge", minAgeStr);
         String maxAgeStr = InputUtil.readString("Filter by Maximum Age: ");
         if (!maxAgeStr.isEmpty()) filters.put("maxAge", maxAgeStr);

         return filters;
     }

      public void displayReport(Report report) {
         if (report == null) {
             CommonView.displayError("Failed to generate report.");
         } else {
             report.display(); // Report model has its own display logic
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