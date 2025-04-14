package utils;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Date;

public class InputUtil {

    private static final Scanner scanner = new Scanner(System.in);

    // Prevent instantiation
    private InputUtil() {}

    /**
     * Gets a non-empty string input from the user.
     *
     * @param prompt The message to display to the user.
     * @return The non-empty string entered by the user.
     */
    public static String readString(String prompt) {
        String input;
        while (true) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            } else {
                System.out.println(TextFormatUtil.error("Input cannot be empty. Please try again."));
            }
        }
    }

    /**
     * Gets an integer input from the user.
     *
     * @param prompt The message to display to the user.
     * @return The integer entered by the user.
     */
    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                return value;
            } catch (InputMismatchException e) {
                System.out.println(TextFormatUtil.error("Invalid input. Please enter an integer."));
                scanner.nextLine(); // Consume invalid input
            }
        }
    }

     /**
     * Gets an integer input from the user within a specified range (inclusive).
     *
     * @param prompt The message to display to the user.
     * @param min The minimum allowed value.
     * @param max The maximum allowed value.
     * @return The integer entered by the user within the range.
     */
    public static int readIntInRange(String prompt, int min, int max) {
        int value;
        while (true) {
            value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println(TextFormatUtil.error(String.format("Input must be between %d and %d. Please try again.", min, max)));
            }
        }
    }


    /**
     * Gets a positive integer input from the user.
     *
     * @param prompt The message to display to the user.
     * @return The positive integer entered by the user.
     */
    public static int readPositiveInt(String prompt) {
        int value;
        while (true) {
             value = readInt(prompt);
             if (value > 0) {
                 return value;
             } else {
                 System.out.println(TextFormatUtil.error("Input must be a positive integer. Please try again."));
             }
        }
    }

     /**
     * Gets a date input from the user in yyyy-MM-dd format.
     *
     * @param prompt The message to display to the user.
     * @return The Date object parsed from the user input.
     */
    public static Date readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String dateString = scanner.nextLine().trim();
            if (DateUtils.isValidDateFormat(dateString)) {
                 Date parsedDate = DateUtils.parseDate(dateString);
                 if (parsedDate != null) {
                     return parsedDate;
                 }
                 // else parseDate already printed an error
            } else {
                System.out.println(TextFormatUtil.error("Invalid date format. Please use yyyy-MM-dd."));
            }
        }
    }

     /**
     * Gets a boolean input from the user (y/n).
     *
     * @param prompt The message to display to the user (should end with "(y/n): ").
     * @return true if the user enters 'y' (case-insensitive), false otherwise ('n').
     */
    public static boolean readBooleanYN(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(input)) {
                return true;
            } else if ("n".equals(input)) {
                return false;
            } else {
                System.out.println(TextFormatUtil.error("Invalid input. Please enter 'y' or 'n'."));
            }
        }
    }


    /**
     * Closes the underlying scanner. Should be called only once when the application exits.
     */
    public static void closeScanner() {
        scanner.close();
    }
}