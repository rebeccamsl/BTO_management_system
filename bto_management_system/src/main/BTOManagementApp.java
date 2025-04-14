package main; // Root package for the main application class


// Import necessary classes from other packages
import controllers.ApplicantController;
import controllers.AuthController;
import controllers.HDBManagerController;
import controllers.HDBOfficerController;
import models.User;
import stores.AuthStore;
import stores.DataStore;
// FilePathConstants is used *inside* DataStore.initialize now, no longer needed here for the call itself
import utils.InputUtil; // If needed for Scanner closing
import views.CommonView;
import views.LoginMenu; // Although AuthController uses it mostly

/**
 * Main application class for the BTO Management System.
 * Initializes data, handles the main login/logout loop, and dispatches
 * control to the appropriate role-based controller.
 */
public class BTOManagementApp {

    public static void main(String[] args) {
        try {
            // 1. Initialize DataStore (Load data from files)
            // CORRECTED: Call initialize() without arguments, as defined in DataStore.java
            DataStore.initialize();

            CommonView.displayWelcomeMessage();
            AuthController authController = new AuthController(); // Handles login logic

            while (true) {
                // 2. Authentication Loop (Login or Exit)
                boolean loggedIn = authController.startLoginProcess();

                if (!loggedIn) {
                    // User chose to exit from the initial login screen
                    break; // Exit the main application loop
                }

                // 3. User is Logged In - Dispatch to Role-Specific Controller
                User currentUser = AuthStore.getCurrentUser();
                if (currentUser == null) {
                    // This should ideally not happen if loggedIn is true, but a safety check
                    CommonView.displayError("Critical Error: Login reported success, but no user found in session. Please contact support.");
                    continue; // Go back to login prompt
                }

                // Instantiate the appropriate controller based on user role
                switch (currentUser.getRole()) {
                    case APPLICANT:
                        ApplicantController applicantController = new ApplicantController();
                        applicantController.showApplicantMenu(); // Enters the applicant's menu loop
                        break;
                    case OFFICER:
                        HDBOfficerController officerController = new HDBOfficerController();
                        officerController.showOfficerMenu(); // Enters the officer's menu loop
                        break;
                    case MANAGER:
                        HDBManagerController managerController = new HDBManagerController();
                        managerController.showManagerMenu(); // Enters the manager's menu loop
                        break;
                    default:
                        CommonView.displayError("Error: Unknown user role encountered (" + currentUser.getRole() + "). Logging out.");
                        AuthController.logout(); // Log out user with unknown role
                }

                // After the role-specific menu loop ends (due to logout choice),
                // the main loop continues, effectively going back to the login prompt.
                if (!AuthStore.isLoggedIn()) {
                    // Optional message
                    // CommonView.displayMessage("Returning to login screen...");
                }
            } // End of main application loop (while true)

        } catch (Exception e) {
            // Catch unexpected errors during runtime
            System.err.println("\n!!! An unexpected error occurred: " + e.getMessage() + " !!!");
            e.printStackTrace(); // Print stack trace for debugging
            System.err.println("Attempting to save data before exiting...");
            DataStore.saveAllData(); // Attempt to save data on crash
            System.err.println("Application will now exit due to the error.");

        } finally {
            // 4. Final Cleanup (Save data and close resources)
            // Ensure data is saved if the loop exits normally (e.g., user chose Exit)
            // Check if the exit was graceful (not due to error and user chose to exit)
            boolean gracefulExit = !AuthStore.isLoggedIn(); // Simple check, assumes logout means intended exit

            if (gracefulExit) {
                 CommonView.displayGoodbyeMessage();
                 DataStore.saveAllData(); // Save data on clean exit too
            } else if (!AuthStore.isLoggedIn()) {
                // If logged out but loop didn't break (e.g., password change), don't show goodbye yet
            }

            // Close the scanner if InputUtil manages a global one
             // InputUtil.closeScanner();
             System.out.println("Application finished.");
        }
    }
}