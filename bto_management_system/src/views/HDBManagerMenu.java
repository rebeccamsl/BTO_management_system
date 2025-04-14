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

/**
 * Handles the display and input for the HDB Manager user interface.
 * Implements PasswordChangeView for the password change functionality.
 */
public class HDBManagerMenu implements controllers.UserController.PasswordChangeView {

    /**
     * Displays the main menu for HDB Managers and gets their choice.
     * @return The integer choice selected by the user.
     */
    public int displayManagerMenu() {
        CommonView.displayNavigationBar("HDB Manager Menu");
        System.out.println("--- Project Management ---");
        System.out.println(" 1. Create New BTO Project");
        System.out.println(" 2. View My Managed Projects");
        System.out.println(" 3. Edit My Managed Project");
        System.out.println(" 4. Delete My Managed Project");
        System.out.println(" 5. View All Projects (Apply Filters)");
        System.out.println(" 6. Toggle Project Visibility");
        System.out.println("--- Approvals ---");
        System.out.println(" 7. Manage HDB Officer Registrations");
        System.out.println(" 8. Manage BTO Applications");
        System.out.println(" 9. Manage Application Withdrawals");
        System.out.println("--- Enquiries ---");
        System.out.println("10. View Enquiries (All / By Project)");
        System.out.println("11. Reply to Enquiry (For Handled Project)");
        System.out.println("--- Reporting ---");
        System.out.println("12. Generate Booking Report");
        System.out.println("--- Account ---");
        System.out.println("13. Change Password");
        System.out.println(" 0. Logout");
        return InputUtil.readIntInRange("Enter your choice: ", 0, 13);
    }

    // --- Project CRUD ---
     /**
      * Prompts for and collects details for creating a new project.
      * @param managerNric The NRIC of the manager creating the project.
      * @return A temporary Project object populated with entered details, or null if cancelled/invalid.
      */
     public Project getNewProjectDetails(String managerNric) {
         CommonView.displayNavigationBar("Create New BTO Project");
         String name = InputUtil.readString("Enter Project Name: ");
         if (name.isEmpty()) { CommonView.displayMessage("Creation cancelled."); return null; }
         String neighborhood = InputUtil.readString("Enter Neighborhood: ");
         if (neighborhood.isEmpty()) { CommonView.displayMessage("Creation cancelled."); return null; }

         Map<FlatType, Integer> units = new HashMap<>();
         int twoRoomUnits = InputUtil.readIntInRange("Enter total number of 2-Room units: ", 0, 9999);
         int threeRoomUnits = InputUtil.readIntInRange("Enter total number of 3-Room units: ", 0, 9999);
         if (twoRoomUnits > 0) units.put(FlatType.TWO_ROOM, twoRoomUnits);
         if (threeRoomUnits > 0) units.put(FlatType.THREE_ROOM, threeRoomUnits);

          if (units.isEmpty()) {
              if (!InputUtil.readBooleanYN(TextFormatUtil.warning("Warning: No units specified. Create project anyway? (y/n): "))) {
                   CommonView.displayMessage("Creation cancelled."); return null;
              }
          }

         Date openDate = InputUtil.readDate("Enter Application Opening Date");
         if (openDate == null) { CommonView.displayMessage("Creation cancelled."); return null; } // Date parsing failed
         Date closeDate = InputUtil.readDate("Enter Application Closing Date");
         if (closeDate == null) { CommonView.displayMessage("Creation cancelled."); return null; }

          if (openDate.after(closeDate)) {
             CommonView.displayError("Closing date cannot be before opening date.");
             return null;
         }

         int slots = InputUtil.readIntInRange("Enter Max HDB Officer Slots (1-10): ", 1, 10);

         // Return temporary object holding data for the service
         return new Project(name.trim(), neighborhood.trim(), units, openDate, closeDate, managerNric, slots);
     }

     /**
      * Displays the result of a project creation attempt.
      * @param project The created Project object, or null if failed.
      */
     public void displayCreateProjectResult(Project project) {
         if (project != null) {
             CommonView.displaySuccess("Project '" + project.getProjectName() + "' created successfully (ID: " + project.getProjectId() + "). Visibility is OFF by default.");
         } else {
             CommonView.displayError("Failed to create project.");
         }
     }

     /**
      * Displays a list of projects in a table format suitable for managers.
      * @param title The title for the list.
      * @param projects The list of Project objects to display.
      */
     public void displayProjectList(String title, List<Project> projects) {
         System.out.println("\n--- " + title + " ---");
         if (projects == null || projects.isEmpty()) {
             System.out.println("No projects found.");
             return;
         }
          String headerFormat = "%-5s | %-25s | %-15s | %-10s | %-10s | %-8s | %-10s | %s\n";
          String rowFormat    = "%-5d | %-25s | %-15s | %-10s | %-10s | %-8s | %-10s | %s\n";
          CommonView.displayTableHeader(headerFormat, "ID", "Project Name", "Neighborhood", "Open Date", "Close Date", "Visible", "Slots", "Manager");
          for (Project p : projects) {
              String visibilityStr = p.isVisible() ? TextFormatUtil.success("Yes") : TextFormatUtil.warning("No");
              String slotsInfo = p.getCurrentOfficerCount() + "/" + p.getMaxOfficerSlots();
              CommonView.displayTableRow(rowFormat,
                     p.getProjectId(),
                     p.getProjectName(),
                     p.getNeighborhood(),
                     utils.DateUtils.formatDate(p.getApplicationOpeningDate()),
                     utils.DateUtils.formatDate(p.getApplicationClosingDate()),
                     visibilityStr,
                     slotsInfo,
                     p.getAssignedHDBManagerNric());
         }
     }

      /**
       * Prompts the user for a Project ID for a specific management action.
       * @param action Description of the action.
       * @return The Project ID entered, or 0 to cancel.
       */
      public int getProjectIdToManage(String action) {
          return InputUtil.readInt("Enter the Project ID to " + action + " (or 0 to cancel): ");
      }

      /**
       * Prompts for updated project details during editing.
       * @param existingProject The current Project object being edited.
       * @return A temporary Project object with updated details, or null if cancelled/invalid.
       */
      public Project getEditedProjectDetails(Project existingProject) {
           if (existingProject == null) return null;

           CommonView.displayNavigationBar("Editing Project: " + existingProject.getProjectName() + " (ID: " + existingProject.getProjectId() + ")");
           System.out.println("Enter new details (Press Enter to keep current value):");

           String name = InputUtil.readString("Project Name [" + existingProject.getProjectName() + "]: ");
           String neighborhood = InputUtil.readString("Neighborhood [" + existingProject.getNeighborhood() + "]: ");

           // Unit editing restriction check
           boolean appsExist = DataStore.getApplications().values().stream().anyMatch(a -> a.getProjectId() == existingProject.getProjectId());
           Map<FlatType, Integer> units = new HashMap<>(existingProject.getTotalUnits());
            if (!appsExist) {
                 CommonView.displayMessage("Note: Applications have not been received; total unit counts can be changed.");
                 Map<FlatType, Integer> newUnits = new HashMap<>();
                 for (FlatType type : FlatType.values()) {
                     int currentCount = units.getOrDefault(type, 0);
                     String countStr = InputUtil.readString("Total " + type.getDisplayName() + " units [" + currentCount + "]: ");
                     if (!countStr.isEmpty()) {
                         int newCount = InputUtil.safeParseInt(countStr, -1);
                         if (newCount < 0) { CommonView.displayError("Unit count cannot be negative. Aborting edit."); return null; }
                         newUnits.put(type, newCount);
                     } else {
                         newUnits.put(type, currentCount);
                     }
                 }
                  units = newUnits;
            } else {
                CommonView.displayWarning("Applications exist for this project. Total unit counts cannot be changed.");
            }

           String openDateStr = InputUtil.readString("App Opening Date (yyyy-MM-dd) [" + utils.DateUtils.formatDate(existingProject.getApplicationOpeningDate()) + "]: ");
           String closeDateStr = InputUtil.readString("App Closing Date (yyyy-MM-dd) [" + utils.DateUtils.formatDate(existingProject.getApplicationClosingDate()) + "]: ");
           String slotsStr = InputUtil.readString("Max Officer Slots (1-10) [" + existingProject.getMaxOfficerSlots() + "]: ");

           Date openDate = openDateStr.isEmpty() ? existingProject.getApplicationOpeningDate() : utils.DateUtils.parseDate(openDateStr);
           Date closeDate = closeDateStr.isEmpty() ? existingProject.getApplicationClosingDate() : utils.DateUtils.parseDate(closeDateStr);
           int slots = slotsStr.isEmpty() ? existingProject.getMaxOfficerSlots() : InputUtil.safeParseInt(slotsStr, -1);

           // View-level validation
            if (openDate == null || closeDate == null) { CommonView.displayError("Invalid date format entered. Aborting edit."); return null; }
            if (openDate.after(closeDate)) { CommonView.displayError("Closing date cannot be before opening date. Aborting edit."); return null; }
            if (slots == -1 || slots < 1 || slots > 10) { CommonView.displayError("Invalid officer slots value (1-10). Aborting edit."); return null; }

           // Create temporary object for service layer
            Project updated = new Project(
                name.isEmpty() ? existingProject.getProjectName() : name.trim(),
                neighborhood.isEmpty() ? existingProject.getNeighborhood() : neighborhood.trim(),
                units, openDate, closeDate,
                existingProject.getAssignedHDBManagerNric(), slots);
             updated.setVisibility(existingProject.isVisible()); // Maintain current visibility during edit
             updated.setAvailableUnits(existingProject.getAvailableUnits()); // Available units not directly edited

             return updated;
      }

      /** Displays edit result. */
      public void displayEditProjectResult(boolean success) {
           if(success) CommonView.displaySuccess("Project details updated successfully.");
           else CommonView.displayError("Failed to update project details.");
      }

      /** Confirms project deletion. */
      public boolean confirmDeleteProject(String projectName) {
          CommonView.displayWarning("WARNING: Deleting a project is irreversible and will remove all associated applications, enquiries, registrations, and bookings.");
          return InputUtil.readBooleanYN("Are you absolutely sure you want to DELETE project '" + projectName + "'? (y/n): ");
      }

       /** Displays delete result. */
       public void displayDeleteProjectResult(boolean success) {
           if(success) CommonView.displaySuccess("Project deleted successfully.");
           else CommonView.displayError("Failed to delete project.");
      }

       /** Prompts for visibility toggle confirmation. */
       public boolean getVisibilityToggleChoice(boolean currentVisibility) {
            System.out.println("Current project visibility is: " + TextFormatUtil.bold(currentVisibility ? "ON" : "OFF"));
            return InputUtil.readBooleanYN("Do you want to set visibility to " + TextFormatUtil.bold(currentVisibility ? "OFF" : "ON") + "? (y/n): ");
       }

        /** Displays visibility toggle result. */
        public void displayToggleVisibilityResult(boolean success, boolean newState) {
            if (success) CommonView.displaySuccess("Project visibility successfully set to " + TextFormatUtil.bold(newState ? "ON" : "OFF") + ".");
            else CommonView.displayError("Failed to toggle project visibility.");
       }


     // --- Approval Menus ---
     /** Displays menu for managing officer registrations. */
     public int displayOfficerRegistrationMenu() {
         CommonView.displayNavigationBar("Manage Officer Registrations for Selected Project");
         System.out.println("1. Approve Registration");
         System.out.println("2. Reject Registration");
         System.out.println("0. Back");
         return InputUtil.readIntInRange("Enter choice: ", 0, 2);
     }

      /** Displays list of officer registrations. */
      public void displayOfficerRegistrationList(String title, List<HDBOfficerRegistration> registrations) {
         System.out.println("\n--- " + title + " ---");
         if (registrations == null || registrations.isEmpty()) {
             System.out.println("No registrations found matching criteria.");
             return;
         }
          String headerFormat = "%-7s | %-20s | %-9s | %-10s | %s\n";
          String rowFormat    = "%-7d | %-20s | %-9s | %-10s | %s\n";
          CommonView.displayTableHeader(headerFormat, "Reg ID", "Officer Name", "NRIC", "Status", "Request Date");
         for (HDBOfficerRegistration reg : registrations) {
             User officer = DataStore.getUserByNric(reg.getOfficerNric());
             CommonView.displayTableRow(rowFormat,
                  reg.getRegistrationId(),
                  (officer != null ? officer.getName() : "N/A"),
                  reg.getOfficerNric(),
                  reg.getStatus(), // Should be PENDING when shown here
                  utils.DateUtils.formatDate(reg.getRequestDate()));
         }
     }

     /** Prompts for Registration ID to manage. */
     public int getRegistrationIdToManage(String action) {
         return InputUtil.readInt("Enter Registration ID to " + action + " (or 0 to cancel): ");
     }

      /** Displays officer registration approval/rejection result. */
      public void displayOfficerApprovalResult(boolean success, String action) {
         if (success) CommonView.displaySuccess("Officer registration " + action + " successfully.");
         else CommonView.displayError("Failed to " + action + " officer registration.");
     }

      // --- BTO Application Approval ---
       /** Displays menu for managing BTO applications. */
       public int displayBTOApplicationMenu() {
         CommonView.displayNavigationBar("Manage BTO Applications for Selected Project");
         System.out.println("1. Approve Application");
         System.out.println("2. Reject Application");
         System.out.println("0. Back");
         return InputUtil.readIntInRange("Enter choice: ", 0, 2);
     }

      /** Displays list of BTO applications. */
      public void displayBTOApplicationList(String title, List<BTOApplication> applications) {
           System.out.println("\n--- " + title + " ---");
           if (applications == null || applications.isEmpty()) {
               System.out.println("No applications found matching criteria.");
               return;
           }
            String headerFormat = "%-7s | %-20s | %-9s | %-4s | %-8s | %-10s | %s\n";
            String rowFormat    = "%-7d | %-20s | %-9s | %-4s | %-8s | %-10s | %s\n";
            CommonView.displayTableHeader(headerFormat, "App ID", "Applicant Name", "NRIC", "Age", "M.Status", "Applied", "Withdrawal?");
           for (BTOApplication app : applications) {
               User applicant = DataStore.getUserByNric(app.getApplicantNric());
               String ageStr = (applicant != null) ? String.valueOf(applicant.getAge()) : "N/A";
               String maritalStr = (applicant != null) ? applicant.getMaritalStatus().name() : "N/A";
               String withdrawalStatus = app.isWithdrawalRequested() ? TextFormatUtil.warning("YES") : "No";
               CommonView.displayTableRow(rowFormat,
                    app.getApplicationId(),
                    (applicant != null ? applicant.getName() : "N/A"),
                    app.getApplicantNric(),
                    ageStr,
                    maritalStr,
                    app.getAppliedFlatType().getDisplayName(),
                    withdrawalStatus
                );
           }
      }

       /** Prompts for Application ID to manage. */
       public int getApplicationIdToManage(String action) {
           return InputUtil.readInt("Enter Application ID to " + action + " (or 0 to cancel): ");
       }

        /** Displays BTO application approval/rejection result. */
        public void displayBTOApprovalResult(boolean success, String action) {
           if (success) CommonView.displaySuccess("BTO Application " + action + " successfully.");
           else CommonView.displayError("Failed to " + action + " BTO application.");
       }

        // --- Withdrawal Approval ---
        /** Displays menu for managing withdrawal requests. */
        public int displayWithdrawalMenu() {
             CommonView.displayNavigationBar("Manage Application Withdrawals for Selected Project");
             System.out.println("1. Approve Withdrawal Request");
             System.out.println("2. Reject Withdrawal Request");
             System.out.println("0. Back");
             return InputUtil.readIntInRange("Enter choice: ", 0, 2);
         }

         /** Displays withdrawal approval/rejection result. */
         public void displayWithdrawalApprovalResult(boolean success, String action) {
            if (success) CommonView.displaySuccess("Application withdrawal request " + action + " successfully.");
            else CommonView.displayError("Failed to " + action + " withdrawal request.");
        }

     // --- Enquiries ---
     /** Displays choice for viewing enquiries. */
      public int displayEnquiryViewChoice() {
          System.out.println("\n--- View Enquiries ---");
          System.out.println("1. View Enquiries for a Specific Project I Manage");
          System.out.println("2. View Enquiries for ALL Projects");
          System.out.println("0. Back");
          return InputUtil.readIntInRange("Enter choice: ", 0, 2);
      }
      // Re-use ApplicantMenu.displayEnquiryList for display
      // Re-use ApplicantMenu.getEnquiryIdToManage for ID input
      // Re-use HDBOfficerMenu.getReplyInput for reply text
      // Re-use HDBOfficerMenu.displayReplyResult for result

     // --- Reporting ---
     /** Prompts for report filter criteria. */
     public Map<String, String> getReportFilters() {
         Map<String, String> filters = new HashMap<>();
         CommonView.displayNavigationBar("Generate Booking Report");
         System.out.println("Enter filter criteria (Leave blank to skip filter):");

         String projName = InputUtil.readString("Filter by Project Name: ");
         if (!projName.isEmpty()) filters.put("projectName", projName.trim());

          String flatTypeStr = InputUtil.readString("Filter by Flat Type (e.g., 2-Room): ");
         if (!flatTypeStr.isEmpty()) {
             if (FlatType.fromDisplayName(flatTypeStr.trim()) != null) {
                 filters.put("flatType", flatTypeStr.trim());
              } else {
                  CommonView.displayWarning("Invalid flat type filter entered, ignoring.");
              }
         }

         String maritalStatusStr = InputUtil.readString("Filter by Marital Status (SINGLE/MARRIED): ");
         if (!maritalStatusStr.isEmpty()) {
             try {
                  MaritalStatus.valueOf(maritalStatusStr.trim().toUpperCase());
                  filters.put("maritalStatus", maritalStatusStr.trim().toUpperCase());
              } catch (IllegalArgumentException e) {
                  CommonView.displayWarning("Invalid marital status filter entered, ignoring.");
              }
         }

         String minAgeStr = InputUtil.readString("Filter by Minimum Age: ");
         if (!minAgeStr.isEmpty()) filters.put("minAge", minAgeStr.trim());
         String maxAgeStr = InputUtil.readString("Filter by Maximum Age: ");
         if (!maxAgeStr.isEmpty()) filters.put("maxAge", maxAgeStr.trim());

         return filters;
     }

      /** Displays the generated report. */
      public void displayReport(Report report) {
         if (report == null) {
             CommonView.displayError("Failed to generate report.");
         } else {
             report.display();
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