package data;

import models.Project;
import enums.FlatType;
import utils.DateUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDataManager extends AbstractCsvDataManager<Integer, Project> {

    @Override
    protected String getHeaderLine() {
        // Example: ProjectID,Name,Neighborhood,TotalUnits(2R;3R),AvailUnits(2R;3R),OpenDate,CloseDate,ManagerNRIC,OfficerSlots,Visibility,AssignedOfficers(NRIC1;NRIC2)
        return "ProjectID,ProjectName,Neighborhood,TotalUnits,AvailableUnits,OpeningDate,ClosingDate,ManagerNRIC,MaxOfficerSlots,Visibility,AssignedOfficerNRICs";
    }

    @Override
    protected Project parseCsvRow(String[] values) {
         if (values.length < 11) {
             System.err.println("Skipping malformed project row: Not enough columns.");
             return null;
         }

        int projectId = safeParseInt(values[0], -1);
        String projectName = safeParseString(values[1]);
        String neighborhood = safeParseString(values[2]);
        Map<FlatType, Integer> totalUnits = parseFlatTypeMap(values[3]);
        Map<FlatType, Integer> availableUnits = parseFlatTypeMap(values[4]);
        Date openingDate = DateUtils.parseDate(safeParseString(values[5]));
        Date closingDate = DateUtils.parseDate(safeParseString(values[6]));
        String managerNric = safeParseString(values[7]);
        int maxSlots = safeParseInt(values[8], 0);
        boolean visibility = safeParseBoolean(values[9], false);
        List<String> officerNrics = parseStringList(values[10]);

         if (projectId <= 0 || projectName.isEmpty() || managerNric.isEmpty() || maxSlots < 0) {
             System.err.println("Skipping project row: Invalid critical data (ID, Name, Manager, Slots) for row starting with ID " + values[0]);
             return null;
         }

         // Simple validation: available units shouldn't exceed total units
         for (FlatType type : availableUnits.keySet()) {
             if (availableUnits.get(type) > totalUnits.getOrDefault(type, 0)) {
                 System.err.println("Warning: Available units exceed total units for " + type + " in project " + projectId + ". Correcting available to total.");
                 availableUnits.put(type, totalUnits.getOrDefault(type, 0));
             }
         }


        return new Project(projectId, projectName, neighborhood, totalUnits, availableUnits,
                           openingDate, closingDate, managerNric, officerNrics, maxSlots, visibility);
    }

    @Override
    protected String formatCsvRow(Project project) {
        return String.join(CSV_DELIMITER,
                String.valueOf(project.getProjectId()),
                project.getProjectName(),
                project.getNeighborhood(),
                formatFlatTypeMap(project.getTotalUnits()),
                formatFlatTypeMap(project.getAvailableUnits()),
                DateUtils.formatDate(project.getApplicationOpeningDate()),
                DateUtils.formatDate(project.getApplicationClosingDate()),
                project.getAssignedHDBManagerNric(),
                String.valueOf(project.getMaxOfficerSlots()),
                String.valueOf(project.isVisible()),
                formatStringList(project.getAssignedHDBOfficerNrics())
        );
    }

    @Override
    protected Integer getKey(Project project) {
        return project.getProjectId();
    }

    // Helper to parse maps like "TWO_ROOM:50;THREE_ROOM:30"
    private Map<FlatType, Integer> parseFlatTypeMap(String data) {
        Map<FlatType, Integer> map = new HashMap<>();
         if (data == null || data.trim().isEmpty()) return map;

        String[] pairs = data.split(LIST_DELIMITER);
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                try {
                    FlatType type = FlatType.valueOf(safeParseString(kv[0]).toUpperCase());
                    int count = safeParseInt(kv[1], 0);
                    map.put(type, count);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Ignoring invalid FlatType '" + kv[0] + "' in map data.");
                }
            }
        }
        return map;
    }

    // Helper to format maps
    private String formatFlatTypeMap(Map<FlatType, Integer> map) {
        if (map == null || map.isEmpty()) return "";
        return map.entrySet().stream()
                .map(entry -> entry.getKey().name() + ":" + entry.getValue())
                .collect(Collectors.joining(LIST_DELIMITER));
    }

     // Helper to parse lists like "S1234567A;T9876543Z"
    private List<String> parseStringList(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Split by delimiter and filter out empty strings that might result from trailing delimiters
        return Arrays.stream(data.split(LIST_DELIMITER))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    // Helper to format lists
    private String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return String.join(LIST_DELIMITER, list);
    }
}