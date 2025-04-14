package views;

import utils.InputUtil;
import utils.TextFormatUtil;

public class LoginMenu {

    public String[] displayLoginPrompt() {
        System.out.println("\n--- User Login ---");
        String nric = InputUtil.readString("Enter NRIC: ");
        // In a real system, read password securely (e.g., using Console class)
        String password = InputUtil.readString("Enter Password (default: password): ");
        return new String[]{nric, password};
    }

    public void displayLoginSuccess(String userName, String role) {
        System.out.println(TextFormatUtil.success("\nLogin Successful!"));
        System.out.println("Welcome, " + userName + " (" + role + ")");
    }

    public void displayLoginError(String message) {
        System.out.println(TextFormatUtil.error("Login Failed: " + message));
    }

    public void displayLogoutMessage() {
        System.out.println("\nYou have been logged out.");
    }

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
         System.out.println(TextFormatUtil.success("Password changed successfully. Please log in again."));
     }

      public void displayPasswordChangeError(String message) {
        System.out.println(TextFormatUtil.error("Password change failed: " + message));
    }

    public int displayInitialAction() {
         System.out.println("\n--- Initial Action ---");
         System.out.println("1. Login");
         System.out.println("0. Exit System");
         return InputUtil.readIntInRange("Enter your choice: ", 0, 1);
    }

}