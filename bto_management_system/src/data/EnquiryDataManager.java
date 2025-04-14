package data;

import models.Enquiry;
import enums.EnquiryStatus;
import utils.DateUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EnquiryDataManager extends AbstractCsvDataManager<Integer, Enquiry> {

    @Override
    protected String getHeaderLine() {
        // Using a different delimiter for replies to avoid CSV comma conflict
        return "EnquiryID,SubmitterNRIC,ProjectID,Status,SubmissionDate,LastUpdateDate,Content,Replies(" + LIST_DELIMITER + " separated)";
    }

    @Override
    protected Enquiry parseCsvRow(String[] values) {
        if (values.length < 8) {
             System.err.println("Skipping malformed enquiry row: Not enough columns.");
             return null;
         }

        int enquiryId = safeParseInt(values[0], -1);
        String submitterNric = safeParseString(values[1]);
        int projectId = safeParseInt(values[2], -1);
        EnquiryStatus status = EnquiryStatus.valueOf(safeParseString(values[3]).toUpperCase());
        Date submissionDate = DateUtils.parseDate(safeParseString(values[4]));
        Date lastUpdateDate = DateUtils.parseDate(safeParseString(values[5]));
        String content = safeParseString(values[6]);
        List<String> replies = parseStringList(values[7]); // Use helper from ProjectDataManager or similar

        if (enquiryId <= 0 || submitterNric.isEmpty() || projectId <= 0 || submissionDate == null || lastUpdateDate == null || content.isEmpty()) {
             System.err.println("Skipping enquiry row: Invalid critical data for row starting with ID " + values[0]);
             return null;
        }


        return new Enquiry(enquiryId, submitterNric, projectId, content, replies, status, submissionDate, lastUpdateDate);
    }

    @Override
    protected String formatCsvRow(Enquiry enquiry) {
        return String.join(CSV_DELIMITER,
                String.valueOf(enquiry.getEnquiryId()),
                enquiry.getSubmitterNric(),
                String.valueOf(enquiry.getProjectId()),
                enquiry.getStatus().name(),
                DateUtils.formatDate(enquiry.getSubmissionDate()),
                DateUtils.formatDate(enquiry.getLastUpdateDate()),
                // Handle potential commas in content by quoting (basic CSV handling)
                "\"" + enquiry.getContent().replace("\"", "\"\"") + "\"", // Basic quoting
                formatStringList(enquiry.getReplies()) // Use helper
        );
    }

    @Override
    protected Integer getKey(Enquiry enquiry) {
        return enquiry.getEnquiryId();
    }

      // Helper to parse lists like "reply1;reply2"
    private List<String> parseStringList(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(data.split(LIST_DELIMITER))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toList());
    }

    // Helper to format lists
    private String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        // Ensure replies themselves don't contain the list delimiter, or handle quoting if they might
        return list.stream()
                  // .map(reply -> "\"" + reply.replace("\"", "\"\"") + "\"") // Quote replies if they might have delimiters/commas
                   .collect(Collectors.joining(LIST_DELIMITER));
    }
}