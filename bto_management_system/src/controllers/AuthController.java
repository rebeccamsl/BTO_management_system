package controllers;

import interfaces.IAuthService;
import models.User;
import services.AuthServiceImpl;
import stores.AuthStore;
import views.LoginMenu;
import views.CommonView; // For messages
import utils.TextFormatUtil; // For formatting

public class AuthController {

    private final LoginMenu loginMenu;
    private final IAuthService authService;

    public AuthController() {
        this.loginMenu = new LoginMenu();
        this.authService = new AuthServiceImpl();
    }

    /**
     * Manages the overall login process.
     * @return true if login is successful, false if user chooses to exit.
     */
    public boolean startLoginProcess() {
        while (true) {
             int action = loginMenu.displayInitialAction();
             if (action == 0) {
                 return false; // User chose to exit
             }

            String[] credentials = loginMenu.displayLoginPrompt();
            String nric = credentials[0];
            String password = credentials[1];

            User loggedInUser = authService.login(nric, password);

            if (loggedInUser != null) {
                loginMenu.displayLoginSuccess(loggedInUser.getName(), loggedInUser.getRole().name());
                return true; // Login successful
            } else {
                // Login failed, authService might have printed details, or we print a generic one
                loginMenu.displayLoginError("Invalid NRIC or Password."); // Generic error shown to user
                // Optionally add retry limit logic here
            }
             // Loop back to ask Login or Exit again after failed attempt
        }
    }

    /**
     * Static method for logging out. Can be called from other controllers.
     */
    public static void logout() {
        IAuthService authService = new AuthServiceImpl(); // Instantiate locally or use shared instance
        authService.logout();
        // LoginMenu view can display the logout message if called from there,
        // or CommonView can be used if logout happens elsewhere.
        // loginMenu.displayLogoutMessage(); // Redundant if BTOManagementApp handles it
    }
}