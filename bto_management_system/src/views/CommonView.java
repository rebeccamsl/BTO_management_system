package views;

import utils.InputUtil; // Needed for pressEnterToContinue
import utils.TextFormatUtil;

/**
 * Provides common utility methods for displaying UI components
 * consistently across the application.
 */
public class CommonView {

    // ANSI Reset code length - used for approximate length calculations with formatting
    private static final int RESET_CODE_LENGTH = TextFormatUtil.RESET.length();

    // Prevent instantiation of this utility class
    private CommonView() {}

    /**
     * Displays the application's welcome message.
     */
    public static void displayWelcomeMessage() {
        String border = "\u250F" + "\u2501".repeat(68) + "\u2513";
        String middle = "\u2503" + " ".repeat(15) + "Welcome to the BTO Management System!" + " ".repeat(16) + "\u2503";
        String bottom = "\u2517" + "\u2501".repeat(68) + "\u251B";
        System.out.println(border);
        System.out.println(middle);
        System.out.println(bottom);
    }

     /**
      * Displays the application's goodbye message.
      */
     public static void displayGoodbyeMessage() {
        System.out.println("\nThank you for using the BTO Management System. Goodbye!");
    }

     /**
      * Displays a formatted navigation bar or section header. Handles long titles by truncating.
      * @param title The title to display in the header.
      */
     public static void displayNavigationBar(String title) {
         int totalWidth = 70; // Adjust width as needed
         String borderLine = "\u2501".repeat(Math.max(0, totalWidth - 2));
         String topBorder = "\u250F" + borderLine + "\u2513";
         String bottomBorder = "\u2517" + borderLine + "\u251B";

         String formattedTitle = " " + TextFormatUtil.bold(title) + " ";
         // Approximate length calculation
         int approxTitleDisplayLength = title.length() + 2;

         String contentLine;
         if (approxTitleDisplayLength > totalWidth - 2) {
             int maxTitleLength = totalWidth - 7;
             String truncatedTitle = title.substring(0, Math.max(0, maxTitleLength)) + "...";
             formattedTitle = " " + TextFormatUtil.bold(truncatedTitle) + " ";
             // Use String.format for padding to ensure correct length
             contentLine = String.format("\u2503%-"+ (totalWidth - 2) +"s\u2503", formattedTitle);

         } else {
             int titleDisplayLength = title.length() + 2;
             int sidePadding = Math.max(0, (totalWidth - titleDisplayLength - 2) / 2);
             String leftPad = " ".repeat(sidePadding);
             int rightPaddingLength = Math.max(0, totalWidth - titleDisplayLength - sidePadding - 2);
             String rightPad = " ".repeat(rightPaddingLength);
             contentLine = "\u2503" + leftPad + formattedTitle + rightPad + "\u2503";
             // Adjust padding if bolding changed effective length (can be tricky)
             // A simpler fixed padding approach might be more reliable if ANSI codes cause issues
             // Example: contentLine = String.format("\u2503 %-" + (totalWidth - 4) + "s \u2503", TextFormatUtil.bold(title));
         }

         System.out.println("\n" + topBorder);
         System.out.println(contentLine);
         System.out.println(bottomBorder);
    }


    /**
     * Displays a standard informational message.
     * @param message The message string.
     */
    public static void displayMessage(String message) {
        System.out.println(message);
    }

    /**
     * Displays a success message, typically formatted in green.
     * @param message The success message string.
     */
    public static void displaySuccess(String message) {
        System.out.println(TextFormatUtil.success(message));
    }

    /**
     * Displays an error message, typically formatted in red and sent to System.err.
     * @param message The error message string.
     */
    public static void displayError(String message) {
        System.err.println(TextFormatUtil.error(message));
    }

    /**
     * Displays a warning message, typically formatted in yellow.
     * @param message The warning message string.
     */
    public static void displayWarning(String message) {
        System.out.println(TextFormatUtil.warning(message));
    }

    /**
     * Displays a generic "Invalid choice" error message.
     */
    public static void displayInvalidChoice() {
        displayError("Invalid choice. Please select a valid option.");
    }

    /**
     * Pauses execution and waits for the user to press Enter.
     */
    public static void pressEnterToContinue() {
        // *** FIX: Use an existing InputUtil method to wait for Enter ***
        InputUtil.readStringAllowEmpty("Press Enter to continue...");
        // *** End Fix ***
    }

     /**
      * Displays a message indicating a feature is not implemented.
      */
     public static void displayNotImplemented() {
         displayWarning("Sorry, this feature is not yet implemented.");
     }

     /**
      * Displays a formatted table header. Requires manual format string.
      * @param formatString The printf format string for the header.
      * @param headers Column headers.
      */
     public static void displayTableHeader(String formatString, String... headers) {
         System.out.println();
         System.out.printf(formatString, (Object[]) headers);
         int width = String.format(formatString, (Object[]) headers).length();
         // Crude way to estimate length without ANSI codes for separator
         int estimatedContentWidth = 0;
         for(String h : headers) estimatedContentWidth += h.length();
         width = Math.max(width - (headers.length * (TextFormatUtil.BOLD.length() + TextFormatUtil.RESET.length())), estimatedContentWidth + headers.length*3);

         System.out.println("-".repeat(Math.max(width, 40)));
     }

     /**
      * Displays a formatted table row using printf.
      * @param formatString The printf format string for the row.
      * @param columns Data for each column in the row.
      */
     public static void displayTableRow(String formatString, Object... columns) {
         System.out.printf(formatString, columns);
     }
}