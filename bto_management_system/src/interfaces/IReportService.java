package interfaces;

import models.Report;
import java.util.Map;

public interface IReportService {
    /**
     * Generates a report of applicants with successful flat bookings based on specified filters.
     * @param filters A map containing filter criteria (e.g., "maritalStatus": "MARRIED", "flatType": "THREE_ROOM", "projectName": "SkyView"). Keys match potential filterable fields.
     * @return A Report object containing the filtered data.
     */
    Report generateBookingReport(Map<String, String> filters);
}