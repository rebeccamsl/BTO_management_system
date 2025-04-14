package data;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays; // Import Arrays
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors; // Import Collectors
import utils.TextFormatUtil; // Import for error formatting

/**
 * Abstract base class for DataManagers that use CSV files.
 * Provides common CSV reading and writing functionalities.
 */
public abstract class AbstractCsvDataManager<K, V> implements DataManager<K, V> {

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
                parentDir.mkdirs();
            }
            // Write header to the new file immediately after creation
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                 writer.write(getHeaderLine());
                 writer.newLine();
             } catch (IOException e) {
                 System.err.println(TextFormatUtil.error("Failed to write header to new file: " + filePath));
                 throw e; // Rethrow exception as initialization failed
             }
             return dataMap; // Return empty map for the newly created file
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean firstLine = true; // Flag to identify the header line

            while ((line = reader.readLine()) != null) {
                // Skip the first line (header) explicitly
                if (firstLine) {
                    firstLine = false; // Set flag to false after reading the first line
                    // Optional: Validate header structure if needed here
                    // String expectedHeader = getHeaderLine();
                    // if (!line.trim().equals(expectedHeader)) {
                    //    System.err.println(TextFormatUtil.warning("Warning: Header mismatch in " + filePath + ". Expected: '" + expectedHeader + "', Found: '" + line.trim() + "'"));
                    // }
                    continue; // Skip processing this line further
                }

                 if (line.trim().isEmpty()) continue; // Skip empty lines

                try {
                    String[] values = line.split(CSV_DELIMITER, -1);
                    V object = parseCsvRow(values); // Call the subclass implementation
                    if (object != null) {
                        // Ensure key extraction doesn't fail
                        K key = getKey(object);
                        if (key != null) {
                             dataMap.put(key, object);
                        } else {
                             System.err.println(TextFormatUtil.error("Error loading data: Could not extract key for object parsed from line: " + line));
                        }
                    }
                    // parseCsvRow handles its own detailed error logging if parsing fails
                } catch (Exception e) {
                     // Catch broader exceptions during parsing/key extraction for a specific line
                     System.err.println(TextFormatUtil.error("Critical error parsing line in file " + filePath + ": " + line));
                     e.printStackTrace(); // Log detailed stack trace but continue loading other lines
                }
            }
        } catch (FileNotFoundException e) {
            // This should not happen due to the file existence check above but jic
            System.err.println(TextFormatUtil.error("Error: Data file not found during read operation: " + filePath));
            throw e;
        } catch (IOException e) {
            System.err.println(TextFormatUtil.error("Error reading data file: " + filePath));
            throw e;
        }
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
            for (V object : dataMap.values()) {
                 try {
                     writer.write(formatCsvRow(object)); // Call subclass implementation
                     writer.newLine();
                 } catch (Exception e) {
                     System.err.println(TextFormatUtil.error("Error formatting object for file " + filePath + ". Object: " + object));
                     e.printStackTrace(); // Log formatting error but continue saving other objects
                 }
            }
        } catch (IOException e) {
            System.err.println(TextFormatUtil.error("Error writing data to file: " + filePath));
            throw e; // Rethrow to signal failure
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

    // --- Helper Methods ---
    protected String safeParseString(String value) {
        return (value == null) ? "" : value.trim(); // Ensure trim happens, return empty string if null
    }

    protected int safeParseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
             System.err.println(TextFormatUtil.warning("Warning: Could not parse integer '" + value + "', using default " + defaultValue));
            return defaultValue;
        }
    }

     protected boolean safeParseBoolean(String value, boolean defaultValue) {
         String trimmedVal = safeParseString(value).toLowerCase();
         if (trimmedVal.isEmpty()) return defaultValue;
         // Handle common boolean strings explicitly
         if ("true".equals(trimmedVal) || "yes".equals(trimmedVal) || "1".equals(trimmedVal)) return true;
         if ("false".equals(trimmedVal) || "no".equals(trimmedVal) || "0".equals(trimmedVal)) return false;
         // Fallback to Boolean.parseBoolean for just "true" (case-insensitive)
         return Boolean.parseBoolean(trimmedVal); // Will be false for anything else
     }

     // Helper method to parse lists like "item1;item2" - Copied from ProjectDataManager
     protected List<String> parseStringList(String data) {
         if (data == null || data.trim().isEmpty()) {
             return new ArrayList<>();
         }
         // Split by delimiter and filter out empty strings that might result from trailing delimiters
         return Arrays.stream(data.split(LIST_DELIMITER))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .collect(Collectors.toList());
     }

     // Helper method to format lists - Copied from ProjectDataManager
     protected String formatStringList(List<String> list) {
         if (list == null || list.isEmpty()) return "";
         // Ensure items themselves don't contain the list delimiter, or handle quoting if they might
         return list.stream()
                   // Basic check for delimiter within item - replace if found? Or quote? Replacing is simpler.
                   .map(item -> item.replace(LIST_DELIMITER, "")) // Prevent accidental splitting
                   .collect(Collectors.joining(LIST_DELIMITER));
     }
}