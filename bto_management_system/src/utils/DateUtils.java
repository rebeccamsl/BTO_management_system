package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    // Define a consistent date format for storage and display
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private DateUtils() {}

    /**
     * Formats a Date object into a string (yyyy-MM-dd).
     * Returns an empty string if the date is null.
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_FORMAT.format(date);
    }

    /**
     * Parses a string (yyyy-MM-dd) into a Date object.
     * Returns null if the string is null, empty, or cannot be parsed.
     */
    public static Date parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return DATE_FORMAT.parse(dateString.trim());
        } catch (ParseException e) {
            System.err.println("Error parsing date string: " + dateString + ". Expected format: yyyy-MM-dd");
            return null; // Or re-throw a custom exception
        }
    }

     /**
     * Checks if a date string is in the valid format yyyy-MM-dd.
     */
    public static boolean isValidDateFormat(String dateString) {
         if (dateString == null || dateString.trim().isEmpty()) {
            return false;
        }
        try {
            // Set lenient to false to enforce strict format matching
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(dateString.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}