package controllers;

import interfaces.IUserService;
import services.UserServiceImpl;
import stores.AuthStore;
import views.CommonView; // For displaying results
import utils.InputUtil; // For reading passwords

// This can be abstract if it forces subclasses to implement 'start',
// but for now, just a concrete class providing shared functionality.
public class UserController {

    protected final IUserService userService;

    public UserController() {
        this.userService = new UserServiceImpl(); // Instantiate service
    }

    /**
     * Handles the password change process.
     * Assumes the necessary view methods (prompts, results) are available
     * either directly or via injection/passed parameter. For simplicity,
     * we might call view methods directly here or expect the subclass controller
     * to call appropriate view methods based on this logic.
     *
     * @param nric The NRIC of the user changing the password.
     * @param view An object capable of displaying prompts and results (e.g., ApplicantMenu, HDBOfficerMenu).
     *             This uses a generic placeholder 'PasswordChangeView' concept. In reality, you'd use
     *             the specific menu object (ApplicantMenu, etc.) passed to or held by the subclass controller.
     */
      protected void handleChangePassword(String nric, PasswordChangeView view) {
        view.displayPasswordChangePrompt();
        String oldPassword = view.readOldPassword();
        String newPassword = view.readNewPassword();
        String confirmPassword = view.readConfirmNewPassword();

        if (!newPassword.equals(confirmPassword)) {
            view.displayPasswordChangeError("New passwords do not match.");
            return;
        }
         if (newPassword.equals(oldPassword)) {
             view.displayPasswordChangeError("New password cannot be the same as the old password.");
            return;
        }
         // Add password complexity rules check here if desired

        boolean success = userService.changePassword(nric, oldPassword, newPassword);

        if (success) {
            view.displayPasswordChangeSuccess();
            AuthController.logout(); // Force re-login after password change
        } else {
            // Service layer might not distinguish between wrong old pwd and other errors easily
            view.displayPasswordChangeError("Incorrect old password or another error occurred.");
        }
    }

     // Define a simple interface for the view methods needed by handleChangePassword
     // Each specific Menu class (ApplicantMenu, OfficerMenu, ManagerMenu) will implement this.
     public interface PasswordChangeView {
         void displayPasswordChangePrompt();
         String readOldPassword();
         String readNewPassword();
         String readConfirmNewPassword();
         void displayPasswordChangeSuccess();
         void displayPasswordChangeError(String message);
     }

}