package main;

import controllers.ApplicantController; 
import controllers.AuthController;
import controllers.HDBManagerController;
import controllers.HDBOfficerController;
import models.User;
import stores.AuthStore;
import stores.DataStore;
import utils.FilePathConstants;
import utils.InputUtil;
import views.CommonView;
import views.LoginMenu;

public class BTOManagementApp {

    public static void main(String[] args) {
        try {
            DataStore.initialize(); 

            CommonView.displayWelcomeMessage();
            AuthController authController = new AuthController();

            while (true) {
                boolean loggedIn = authController.startLoginProcess();

                if (!loggedIn) {
                    break;
                }

                User currentUser = AuthStore.getCurrentUser();
                if (currentUser == null) {
                    CommonView.displayError("Critical Error: Login reported success, but no user found in session.");
                    continue;
                }

                switch (currentUser.getRole()) {
                    case APPLICANT:
                        ApplicantController applicantController = new ApplicantController(); // Ensure type is correct
                        applicantController.showApplicantMenu(); // 
                        break;
                    case OFFICER:
                        HDBOfficerController officerController = new HDBOfficerController();
                        officerController.showOfficerMenu();
                        break;
                    case MANAGER:
                        HDBManagerController managerController = new HDBManagerController();
                        managerController.showManagerMenu();
                        break;
                    default:
                        CommonView.displayError("Error: Unknown user role encountered (" + currentUser.getRole() + "). Logging out.");
                        AuthController.logout();
                }

                if (!AuthStore.isLoggedIn()) {
                }
            }

        } catch (Exception e) {
            System.err.println("\n!!! An unexpected error occurred: " + e.getMessage() + " !!!");
            e.printStackTrace();
            System.err.println("Attempting to save data before exiting...");
            DataStore.saveAllData();
            System.err.println("Application will now exit due to the error.");

        } finally {
            boolean gracefulExit = !AuthStore.isLoggedIn();

            if (gracefulExit) {
                 CommonView.displayGoodbyeMessage(); 
                 DataStore.saveAllData();
            }

           
             System.out.println("Application finished.");
        }
    }
}