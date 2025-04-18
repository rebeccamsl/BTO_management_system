package views;

import utils.InputUtil;
import utils.TextFormatUtil;
import java.util.regex.Pattern; // Import Pattern for removing ANSI codes

/**
 * Provides common utility methods for displaying UI components
 * consistently across the application.
 */
public class CommonView {

    // Pattern to remove ANSI escape codes for length calculation
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");

    // Private constructor to prevent instantiation
    private CommonView() {}

    // ... displayWelcomeMessage, displayGoodbyeMessage ... (no changes)
     public static void displayWelcomeMessage() {
        String border = "\u250F" + "\u2501".repeat(68) + "\u2513";
        String middle = "\u2503" + " ".repeat(15) + "Welcome to the BTO Management System!" + " ".repeat(16) + "\u2503";
        String bottom = "\u2517" + "\u2501".repeat(68) + "\u251B";
        System.out.println(border);
        System.out.println(middle);
        System.out.println(bottom);
    }
     public static void displayGoodbyeMessage() {
        System.out.println("\nThank you for using the BTO Management System. Goodbye!");
    }


     /**
      * Displays a formatted navigation bar or section header.
      * Calculates padding based on visible length, ignoring ANSI codes.
      * @param title The title string, potentially containing ANSI formatting.
      */
     public static void displayNavigationBar(String title) {
         int totalWidth = 70; // Target width
         String borderLine = "\u2501".repeat(Math.max(0, totalWidth - 2));
         String topBorder = "\u250F" + borderLine + "\u2513";
         String bottomBorder = "\u2517" + borderLine + "\u251B";

         // Title with desired formatting (e.g., bold) but without extra spaces yet
         String formattedTitleContent = TextFormatUtil.bold(title);
         // Calculate VISIBLE length by removing ANSI codes
         String visibleTitle = removeAnsiCodes(formattedTitleContent);
         int visibleTitleLength = visibleTitle.length();

         // Add spaces around the visible title for centering calculation
         int titleLengthWithSpaces = visibleTitleLength + 2; // +2 for spaces " Title "

         String contentLine;
         // Check if visible title (+ spaces) exceeds available width
         if (titleLengthWithSpaces > totalWidth - 2) {
             // Truncate the *original* title if needed, then reformat
             int maxVisibleLength = totalWidth - 7; // Space for bars, spaces, "..."
             String truncatedVisibleTitle = title.substring(0, Math.min(title.length(), maxVisibleLength)) + "...";
             formattedTitleContent = " " + TextFormatUtil.bold(truncatedVisibleTitle) + " ";
             // Format the line, assuming truncation handles length
             contentLine = String.format("\u2503%-"+ (totalWidth - 2) +"s\u2503", formattedTitleContent); // Left align padded

         } else {
             // Center the title based on visible length
             int sidePadding = Math.max(0, (totalWidth - titleLengthWithSpaces - 2) / 2);
             String leftPad = " ".repeat(sidePadding);
             // Calculate right padding needed
             int rightPaddingLength = Math.max(0, totalWidth - sidePadding - titleLengthWithSpaces - 2);
             String rightPad = " ".repeat(rightPaddingLength);
             // Construct the line with actual formatted title
             contentLine = "\u2503" + leftPad + " " + formattedTitleContent + " " + rightPad + "\u2503";
         }

         System.out.println("\n" + topBorder);
         System.out.println(contentLine);
         System.out.println(bottomBorder);
    }

    /** Helper method to remove ANSI escape codes for length calculation */
    private static String removeAnsiCodes(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return ANSI_ESCAPE_PATTERN.matcher(text).replaceAll("");
    }


    // ... displayMessage, displaySuccess, displayError, etc. ... (no changes)
    public static void displayMessage(String message) {
        System.out.println(message);
    }
    public static void displaySuccess(String message) {
        System.out.println(TextFormatUtil.success(message));
    }
    public static void displayError(String message) {
        System.err.println(TextFormatUtil.error(message));
    }
    public static void displayWarning(String message) {
        System.out.println(TextFormatUtil.warning(message));
    }
    public static void displayInvalidChoice() {
        displayError("Invalid choice. Please select a valid option.");
    }
    public static void pressEnterToContinue() {
        InputUtil.readStringAllowEmpty("Press Enter to continue...");
    }
     public static void displayNotImplemented() {
         displayWarning("Sorry, this feature is not yet implemented.");
     }
     public static void displayTableHeader(String formatString, String... headers) {
         System.out.println();
         System.out.printf(formatString, (Object[]) headers);
         int width = calculateVisibleWidth(formatString, headers);
         System.out.println("-".repeat(Math.max(width, 40)));
     }
     public static void displayTableRow(String formatString, Object... columns) {
         System.out.printf(formatString, columns);
     }
     // Helper to estimate visible width for table separator
     private static int calculateVisibleWidth(String formatString, String... headers) {
         String formattedHeader = String.format(formatString, (Object[]) headers);
         String visibleHeader = removeAnsiCodes(formattedHeader);
         // This is still approximate as printf alignment affects final visual width
         return Math.max(visibleHeader.length(), 40);
     }
}