package data;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        // Create file and directory if they don't exist
        if (!file.exists()) {
             System.out.println("Data file not found, creating new file: " + filePath);
            file.getParentFile().mkdirs(); // Ensure directory exists
            file.createNewFile();
            // Write header to the new file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                 writer.write(getHeaderLine());
                 writer.newLine();
             }
             return dataMap; // Return empty map for new file
        }


        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isHeader = true; // Skip header line

            while ((line = reader.readLine()) != null) {
                 if (isHeader) {
                    isHeader = false;
                     // Optionally validate header here against getHeaderLine()
                    continue;
                }
                 if (line.trim().isEmpty()) continue; // Skip empty lines

                try {
                    String[] values = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
                    V object = parseCsvRow(values);
                    if (object != null) {
                        dataMap.put(getKey(object), object);
                    }
                } catch (Exception e) {
                     System.err.println("Error parsing line in file " + filePath + ": " + line);
                     e.printStackTrace(); // Log parsing error but continue loading other lines
                }
            }
        }
        return dataMap;
    }

    @Override
    public void save(String filePath, Map<K, V> dataMap) throws IOException {
        File file = new File(filePath);
        // Ensure directory exists
        file.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Write header
            writer.write(getHeaderLine());
            writer.newLine();

            // Write data rows
            for (V object : dataMap.values()) {
                 try {
                     writer.write(formatCsvRow(object));
                     writer.newLine();
                 } catch (Exception e) {
                     System.err.println("Error formatting object for file " + filePath + ": " + object);
                     e.printStackTrace(); // Log formatting error but continue saving other objects
                 }
            }
        }
    }

    /**
     * Defines the header line for the CSV file.
     * Must be implemented by subclasses.
     * @return The CSV header string.
     */
    protected abstract String getHeaderLine();

    /**
     * Parses a single row (array of string values) from the CSV into a model object.
     * Must be implemented by subclasses.
     * @param values The array of string values from a CSV row.
     * @return The parsed model object, or null if parsing fails.
     */
    protected abstract V parseCsvRow(String[] values);

    /**
     * Formats a model object into a CSV string row.
     * Must be implemented by subclasses.
     * @param object The model object to format.
     * @return The formatted CSV string row.
     */
    protected abstract String formatCsvRow(V object);

    /**
     * Extracts the key from a model object for map storage.
     * Must be implemented by subclasses.
     * @param object The model object.
     * @return The key for the map.
     */
    protected abstract K getKey(V object);

     // Helper method to safely parse strings, returning default if empty/null
    protected String safeParseString(String value) {
        return (value == null || value.trim().isEmpty()) ? "" : value.trim();
    }

    // Helper method to safely parse integers, returning a default value on error
    protected int safeParseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
             System.err.println("Warning: Could not parse integer '" + value + "', using default " + defaultValue);
            return defaultValue;
        }
    }

    // Helper method to safely parse booleans
     protected boolean safeParseBoolean(String value, boolean defaultValue) {
         if (value == null || value.trim().isEmpty()) return defaultValue;
         return Boolean.parseBoolean(value.trim());
     }
}