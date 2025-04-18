package views;

import utils.InputUtil;
import utils.TextFormatUtil;

/**
 * Handles the display and input for the user login process.
 */
public class LoginMenu {

    /**
     * Displays prompts and reads NRIC and password from the user.
     * @return A String array containing {nric, password}.
     */
    public String[] displayLoginPrompt() {
        System.out.println("\n--- User Login ---");
        String nric = InputUtil.readString("Enter NRIC: ");
        String password = InputUtil.readString("Enter Password: "); 
        return new String[]{nric, password};
    }

    /**
     * Displays a success message upon successful login.
     * @param userName The name of the logged-in user.
     * @param role The role of the logged-in user.
     */
    public void displayLoginSuccess(String userName, String role) {
        System.out.println(TextFormatUtil.success("\nLogin Successful!"));
        System.out.println("Welcome, " + userName + " (" + role + ")");
    }

    /**
     * Displays an error message upon failed login.
     * @param message The specific error message.
     */
    public void displayLoginError(String message) {
        CommonView.displayError("Login Failed: " + message);
    }

    /**
     * Displays a message confirming logout.
     */
    public void displayLogoutMessage() {
        CommonView.displayMessage("\nYou have been logged out.");
    }

     /**
      * Displays the header for the password change section.
      */
     public void displayPasswordChangePrompt() {
         System.out.println("\n--- Change Password ---");
         CommonView.displayMessage("Note: Default password is 'password'.");
     }

     /**
      * Prompts for and reads the user's old password.
      * @return The entered old password.
      */
     public String readOldPassword() {
         return InputUtil.readString("Enter Old Password: ");
     }

      /**
       * Prompts for and reads the user's new password.
       * @return The entered new password.
       */
      public String readNewPassword() {
         return InputUtil.readString("Enter New Password: ");
     }

      /**
       * Prompts for and reads the confirmation of the new password.
       * @return The entered confirmation password.
       */
      public String readConfirmNewPassword() {
         return InputUtil.readString("Confirm New Password: ");
     }

     /**
      * Displays a success message after password change.
      */
     public void displayPasswordChangeSuccess() {
         CommonView.displaySuccess("Password changed successfully. Please log in again.");
     }

      /**
       * Displays an error message if password change fails.
       * @param message The specific error message.
       */
      public void displayPasswordChangeError(String message) {
        CommonView.displayError("Password change failed: " + message);
    }

    /**
     * Displays the initial choice between logging in or exiting.
     * @return The user's choice (1 for Login, 0 for Exit).
     */
    public int displayInitialAction() {
         System.out.println("\n--- BTO System ---");
         System.out.println("1. Login");
         System.out.println("0. Exit System");
         return InputUtil.readIntInRange("Enter your choice: ", 0, 1);
    }
}