package data;

import models.Project;
import enums.FlatType;
import utils.DateUtils;
import utils.TextFormatUtil;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern; 
import java.util.stream.Collectors;

/**
 * DataManager implementation for Project using CSV files.
 */
public class ProjectDataManager extends AbstractCsvDataManager<Integer, Project> {

    @Override
    protected String getHeaderLine() {
        return "ProjectID,ProjectName,Neighborhood,TotalUnits,AvailableUnits,OpeningDate,ClosingDate,ManagerNRIC,MaxOfficerSlots,Visibility,AssignedOfficerNRICs";
    }

    @Override
    protected Project parseCsvRow(String[] values) {
        int EXPECTED_COLUMNS = 11;
        if (values.length < EXPECTED_COLUMNS) {
            System.err.println(TextFormatUtil.error("Skipping malformed project row: Expected at least " + EXPECTED_COLUMNS + " columns, found " + values.length + ". Line: " + Arrays.toString(values)));
            return null;
        }

        // Parse main fields 
        int projectId = safeParseInt(values[0], -1);
        String projectName = safeParseString(values[1]);
        String neighborhood = safeParseString(values[2]);
        
        // Remove potential surrounding quotes if CSV writer added them unnecessarily
        if (projectName.startsWith("\"") && projectName.endsWith("\"")) {
            projectName = projectName.substring(1, projectName.length() - 1).replace("\"\"", "\"");
        }
        if (neighborhood.startsWith("\"") && neighborhood.endsWith("\"")) {
            neighborhood = neighborhood.substring(1, neighborhood.length() - 1).replace("\"\"", "\"");
        }

        // Parse complex fields using helpers 
        Map<FlatType, Integer> totalUnits = parseFlatTypeMap(values[3]);
        Map<FlatType, Integer> availableUnits = parseFlatTypeMap(values[4]);

        Date openingDate = DateUtils.parseDate(safeParseString(values[5]));
        Date closingDate = DateUtils.parseDate(safeParseString(values[6]));
        String managerNric = safeParseString(values[7]);
        int maxSlots = safeParseInt(values[8], 0);
        boolean visibility = safeParseBoolean(values[9], false); // Use helper

        String officersStr = (values.length > 10) ? safeParseString(values[10]) : ""; // Handle potential missing last column if empty
        List<String> officerNrics = parseStringList(officersStr);

        // Critical Data Validation 
        if (projectId <= 0 || projectName.isEmpty() || managerNric.isEmpty() || maxSlots < 0 ) {
            System.err.println(TextFormatUtil.error("Skipping project row: Invalid critical data (ID<=0, Name empty, Manager empty, or Slots<0) for row starting with ID " + values[0]));
            return null;
        }
        if (maxSlots > 10) {
             System.err.println(TextFormatUtil.warning("Project row with ID " + values[0] + " has MaxOfficerSlots > 10 ("+maxSlots+"). Setting to 10."));
             maxSlots = 10;
        }
         if (maxSlots < officerNrics.size()) {
            System.err.println(TextFormatUtil.error("Skipping project row: MaxOfficerSlots ("+maxSlots+") is less than the number of assigned officers ("+officerNrics.size()+") derived from ["+officersStr+"] for project ID " + values[0]));
            return null;
         }


        // Post-parsing Unit Consistency Validation
        if (totalUnits.values().stream().anyMatch(v -> v < 0) || availableUnits.values().stream().anyMatch(v -> v < 0)) {
             System.err.println(TextFormatUtil.error("Skipping project row: Negative unit count found for project " + projectId));
             return null;
        }
        for (Map.Entry<FlatType, Integer> availEntry : availableUnits.entrySet()) {
            FlatType type = availEntry.getKey();
            int available = availEntry.getValue();
            int total = totalUnits.getOrDefault(type, 0);
            if (available > total) {
                System.err.println(TextFormatUtil.warning("Data inconsistency: Available units (" + available + ") exceed total units (" + total + ") for " + type + " in project " + projectId + ". Correcting available to total."));
                availableUnits.put(type, total);
            }
        }
        for (Map.Entry<FlatType, Integer> totalEntry : totalUnits.entrySet()) {
            FlatType type = totalEntry.getKey();
            int total = totalEntry.getValue();
            if (total > 0 && !availableUnits.containsKey(type)) {
                 System.err.println(TextFormatUtil.warning("Data inconsistency: Total units exist for " + type + " in project " + projectId + " but no entry in available units map. Initializing available to 0."));
                availableUnits.put(type, 0);
            }
        }

        // Construct and return the Project object
        return new Project(projectId, projectName, neighborhood, totalUnits, availableUnits,
                           openingDate, closingDate, managerNric, officerNrics, maxSlots, visibility);
    }

    /**
     * Formats a Project object into a CSV string row.
     * @param project The Project object to format.
     * @return The formatted CSV string row.
     */
    @Override
    protected String formatCsvRow(Project project) {
        return String.join(CSV_DELIMITER,
                String.valueOf(project.getProjectId()),
                // Quote fields that might contain the delimiter or quotes
                "\"" + project.getProjectName().replace("\"", "\"\"") + "\"",
                "\"" + project.getNeighborhood().replace("\"", "\"\"") + "\"",
                formatFlatTypeMap(project.getTotalUnits()),
                formatFlatTypeMap(project.getAvailableUnits()),
                DateUtils.formatDate(project.getApplicationOpeningDate()), // Handles null
                DateUtils.formatDate(project.getApplicationClosingDate()), // Handles null
                project.getAssignedHDBManagerNric(),
                String.valueOf(project.getMaxOfficerSlots()),
                String.valueOf(project.isVisible()).toLowerCase(), // "true" or "false"
                formatStringList(project.getAssignedHDBOfficerNrics())
        );
    }

    /**
     * Gets the key (Project ID) for the Project object.
     * @param project The Project object.
     * @return The Project ID.
     */
    @Override
    protected Integer getKey(Project project) {
        return project.getProjectId();
    }

    // Helper Methods

    /**
     * Parses strings like "TWO_ROOM:50;THREE_ROOM:30" into a Map<FlatType, Integer>.
     * Uses Pattern.quote for safe splitting.
     */
    private Map<FlatType, Integer> parseFlatTypeMap(String data) {
        Map<FlatType, Integer> map = new HashMap<>();
        if (data == null || data.trim().isEmpty()) return map;

        // Split by the defined list delimiter
        String[] pairs = data.split(Pattern.quote(LIST_DELIMITER));

        for (String pair : pairs) {
            if (pair == null || pair.trim().isEmpty()) continue;

            // Split each pair by the first colon only
            String[] kv = pair.trim().split(":", 2);

            if (kv.length == 2) {
                String typeStr = kv[0].trim().toUpperCase(); // Key part
                String countStr = kv[1].trim(); // Value part

                try {
                    FlatType type = FlatType.valueOf(typeStr); // Parse enum key
                    int count = Integer.parseInt(countStr);    // Parse integer value

                    if (count >= 0) {
                        map.put(type, count);
                    } else {
                        System.err.println(TextFormatUtil.warning("Ignoring negative unit count '" + countStr + "' for type '" + typeStr + "' in project data part: [" + pair + "]"));
                    }
                } catch (NumberFormatException e) {
                    System.err.println(TextFormatUtil.warning("Ignoring invalid unit count (not a number) '" + countStr + "' for type '" + typeStr + "' in project data part: [" + pair + "]"));
                } catch (IllegalArgumentException e) {
                    System.err.println(TextFormatUtil.warning("Ignoring invalid FlatType name '" + typeStr + "' in project data part: [" + pair + "]"));
                }
            } else {
                // This warning triggers if a segment split by ';' doesn't contain a ':'
                System.err.println(TextFormatUtil.warning("Ignoring malformed pair (missing colon?) '" + pair + "' in project data string: [" + data + "]"));
            }
        }
        return map;
    }

    /** Formats a Map<FlatType, Integer> back to a CSV string fragment. */
    private String formatFlatTypeMap(Map<FlatType, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() >= 0)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().name() + ":" + entry.getValue())
                .collect(Collectors.joining(LIST_DELIMITER));
    }

    /** Parses strings like "S123;T456" into a List<String>. */
    private List<String> parseStringList(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Split by the defined list delimiter, escaping it, and filter empty results
        return Arrays.stream(data.split(Pattern.quote(LIST_DELIMITER)))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    /** Formats a List<String> back into a CSV string fragment. */
    private String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(LIST_DELIMITER, list);
    }
}