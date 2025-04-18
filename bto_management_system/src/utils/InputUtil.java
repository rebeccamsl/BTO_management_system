package utils;

import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Date;

public class InputUtil {

    private static final Scanner scanner = new Scanner(System.in);

    private InputUtil() {}

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

    public static String readStringAllowEmpty(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                     System.out.println(TextFormatUtil.error("Input cannot be empty. Please enter a whole number."));
                     continue;
                }
                int value = Integer.parseInt(line);
                return value;
            } catch (NumberFormatException e) {
                System.out.println(TextFormatUtil.error("Invalid input. Please enter a whole number."));
            }
        }
    }

    public static int readIntInRange(String prompt, int min, int max) {
        int value;
        while (true) {
            value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            } else {
                System.out.println(TextFormatUtil.error(String.format("Input must be between %d and %d (inclusive). Please try again.", min, max)));
            }
        }
    }

     public static int safeParseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
             System.err.println("Warning: Could not parse integer '" + value + "', using default " + defaultValue);
            return defaultValue;
        }
    }

    public static int readPositiveInt(String prompt) {
        int value;
        while (true) {
             value = readInt(prompt);
             if (value > 0) {
                 return value;
             } else {
                 System.out.println(TextFormatUtil.error("Input must be a positive integer (greater than 0). Please try again."));
             }
        }
    }

    public static Date readDate(String prompt) {
        while (true) {
            String dateString = readStringAllowEmpty(prompt + " (yyyy-MM-dd or 0 to cancel): ");
            if ("0".equals(dateString)) {
                 return null;
            }
            if (dateString.isEmpty()){
                 System.out.println(TextFormatUtil.error("Date cannot be empty. Please use yyyy-MM-dd or enter 0 to cancel."));
                 continue;
            }
            if (DateUtils.isValidDateFormat(dateString)) {
                 Date parsedDate = DateUtils.parseDate(dateString);
                 if (parsedDate != null) {
                     return parsedDate;
                 }
            } else {
                System.out.println(TextFormatUtil.error("Invalid date format. Please use yyyy-MM-dd or enter 0 to cancel."));
            }
        }
    }

    public static boolean readBooleanYN(String prompt) {
        while (true) {
            String input = readString(prompt).toLowerCase();
            if ("y".equals(input)) {
                return true;
            } else if ("n".equals(input)) {
                return false;
            } else {
                System.out.println(TextFormatUtil.error("Invalid input. Please enter 'y' or 'n'."));
            }
        }
    }

    public static void closeScanner() {
        try {
            if (scanner != null) {
                scanner.close();
                System.out.println("Debug: Input scanner closed.");
            }
        } catch (IllegalStateException e) {
            System.err.println("Warning: Scanner already closed or in invalid state.");
        }
    }
}