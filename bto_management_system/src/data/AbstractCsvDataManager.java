package data;

import utils.TextFormatUtil; // Assuming you have this for errors

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class for DataManagers that use CSV files.
 * Provides common CSV reading and writing functionalities.
 * @param <K> Key type for the data map
 * @param <V> Value type (Model object) for the data map
 */
public abstract class AbstractCsvDataManager<K, V> implements DataManager<K, V> {

    // Ensure these delimiters are correctly defined and accessible
    protected static final String CSV_DELIMITER = ",";
    protected static final String LIST_DELIMITER = ";"; // Delimiter for lists within a cell

    @Override
    public Map<K, V> load(String filePath) throws IOException {
        Map<K, V> dataMap = new ConcurrentHashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
             System.out.println("Data file not found, creating new file: " + filePath);
             File parentDir = file.getParentFile();
             if (parentDir != null && !parentDir.exists()) {
                 parentDir.mkdirs(); // Ensure directory exists
             }
             if (file.createNewFile()) {
                 // Write header to the new file only if creation succeeded
                 try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                     writer.write(getHeaderLine());
                     writer.newLine();
                 } catch (IOException writeEx) {
                     System.err.println(TextFormatUtil.error("Failed to write header to new file: " + filePath + " - " + writeEx.getMessage()));
                 }
             } else {
                 System.err.println(TextFormatUtil.error("Failed to create new file: " + filePath));
             }
             return dataMap; // Return empty map for new or uncreatable file
        }


        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            boolean isHeaderSkipped = false;

            while ((line = reader.readLine()) != null) {
                 lineNumber++;
                 if (!isHeaderSkipped) {
                    isHeaderSkipped = true;
                    // Optionally validate header here against getHeaderLine()
                    continue; // Skip header line
                }
                 if (line.trim().isEmpty()) {
                      // System.out.println("Skipping empty line " + lineNumber + " in " + filePath); // Optional info
                     continue; // Skip empty lines
                 }

                try {
                    // Use the CSV delimiter to split the main line
                    String[] values = line.split(CSV_DELIMITER, -1); // -1 keeps trailing empty strings
                    V object = parseCsvRow(values); // Delegate parsing to subclass
                    if (object != null) {
                        dataMap.put(getKey(object), object); // Add to map using key from object
                    } else {
                        // parseCsvRow should print specific errors, but log general failure too
                        System.err.println("Failed to parse line " + lineNumber + " in " + filePath + ". Skipping row. Content: " + Arrays.toString(values));
                    }
                } catch (Exception e) {
                     // Catch any unexpected exception during row processing
                     System.err.println(TextFormatUtil.error("Unexpected error processing line " + lineNumber + " in file " + filePath + ": " + line));
                     e.printStackTrace(); // Log parsing error but try to continue loading other lines
                }
            }
        } catch (FileNotFoundException e) {
            // This case should be handled by the file.exists() check, but included for completeness
            System.err.println(TextFormatUtil.error("Data file not found during load attempt (should have been created): " + filePath));
            throw e; // Re-throw as it indicates a setup issue
        }
        System.out.println("Loaded " + dataMap.size() + " records from: " + filePath);
        return dataMap;
    }

    @Override
    public void save(String filePath, Map<K, V> dataMap) throws IOException {
        File file = new File(filePath);
         File parentDir = file.getParentFile();
         if (parentDir != null && !parentDir.exists()) {
             parentDir.mkdirs(); // Ensure directory exists before writing
         }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write header
            writer.write(getHeaderLine());
            writer.newLine();

            // Write data rows
            if (dataMap != null) { // Check if map is null
                for (V object : dataMap.values()) {
                    if (object == null) continue; // Skip null objects in the map
                    try {
                        writer.write(formatCsvRow(object)); // Delegate formatting to subclass
                        writer.newLine();
                    } catch (Exception e) {
                        System.err.println(TextFormatUtil.error("Error formatting object for file " + filePath + ": " + object));
                        e.printStackTrace(); // Log formatting error but continue saving other objects
                    }
                }
                System.out.println("Saving " + dataMap.size() + " records to: " + filePath);
            } else {
                System.out.println("Warning: Data map provided for saving to " + filePath + " is null. Saving empty file.");
            }
        } catch (IOException e) {
             System.err.println(TextFormatUtil.error("Error writing to file " + filePath + ": " + e.getMessage()));
             throw e; // Re-throw to indicate save failure
        }
    }

    /** Defines the header line for the CSV file. Must be implemented by subclasses. */
    protected abstract String getHeaderLine();

    /** Parses a single row (array of string values) from the CSV into a model object. Must be implemented by subclasses. */
    protected abstract V parseCsvRow(String[] values);

    /** Formats a model object into a CSV string row. Must be implemented by subclasses. */
    protected abstract String formatCsvRow(V object);

    /** Extracts the key from a model object for map storage. Must be implemented by subclasses. */
    protected abstract K getKey(V object);

     //Helper Methods for Subclasses

     /** Safely trims string, returns empty string if input is null. */
    protected String safeParseString(String value) {
        return (value == null) ? "" : value.trim();
    }

    /** Safely parses integer, returns default on error/empty. */
    protected int safeParseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
             System.err.println(TextFormatUtil.warning("Could not parse integer '" + value + "', using default " + defaultValue));
            return defaultValue;
        }
    }

     /** Safely parses boolean ("true" case-insensitive), returns default otherwise. */
     protected boolean safeParseBoolean(String value, boolean defaultValue) {
         if (value == null || value.trim().isEmpty()) return defaultValue;
         return "true".equalsIgnoreCase(value.trim());
     }
}