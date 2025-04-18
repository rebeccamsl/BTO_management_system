package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NRICValidator {

    // pattern for NRIC: Starts with S or T, followed by 7 digits, ends with a letter.
    private static final Pattern NRIC_PATTERN = Pattern.compile("^[STst]\\d{7}[a-zA-Z]$");

    // Prevent instantiation
    private NRICValidator() {}

    /**
     * Validates if the given string matches the NRIC format.
     * Case-insensitive for the starting and ending letters.
     *
     * @param nric The NRIC string to validate.
     * @return true if the format is valid, false otherwise.
     */
    public static boolean isValidFormat(String nric) {
        if (nric == null || nric.trim().isEmpty()) {
            return false;
        }
        Matcher matcher = NRIC_PATTERN.matcher(nric.trim());
        return matcher.matches();
    }

    /**
     * (Optional) Basic checksum validation for NRIC/FIN.
     * Note: This is a simplified example and might not cover all edge cases or future formats.
     * Real-world validation might be more complex.
     *
     * @param nric The NRIC string (assumed valid format).
     * @return true if the checksum is potentially valid, false otherwise.
     */
    public static boolean isValidChecksum(String nric) {
         if (!isValidFormat(nric)) {
            return false; // Format must be valid
         }

        nric = nric.toUpperCase(); // uppercase
        char firstChar = nric.charAt(0);
        char lastChar = nric.charAt(nric.length() - 1);
        String digits = nric.substring(1, 8);

        int totalWeight = 0;
        int[] weights = {2, 7, 6, 5, 4, 3, 2};

        for (int i = 0; i < digits.length(); i++) {
            totalWeight += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }

        // Add offset for 'T' or 'G' prefixes (FIN numbers starting from 2000)
        if (firstChar == 'T' || firstChar == 'G') {
            totalWeight += 4;
        }

        int remainder = totalWeight % 11;

        char expectedLastChar;
        if (firstChar == 'S' || firstChar == 'T') { // NRIC Checksum
            char[] nricChecksumChars = {'J', 'Z', 'I', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A'};
            expectedLastChar = nricChecksumChars[remainder];
        } else if (firstChar == 'F' || firstChar == 'G') { // FIN Checksum
            char[] finChecksumChars = {'X', 'W', 'U', 'T', 'R', 'Q', 'P', 'N', 'M', 'L', 'K'};
             expectedLastChar = finChecksumChars[remainder];
        }

         else {
            return false; 
         }

        return lastChar == expectedLastChar;
    }
}