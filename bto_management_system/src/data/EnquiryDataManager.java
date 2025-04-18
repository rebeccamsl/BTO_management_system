package data;

import models.Enquiry;
import enums.EnquiryStatus;
import utils.DateUtils;
import utils.TextFormatUtil; // Added

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EnquiryDataManager extends AbstractCsvDataManager<Integer, Enquiry> {

    @Override
    protected String getHeaderLine() {
        // Use LIST_DELIMITER '|' for replies
        return "EnquiryID,SubmitterNRIC,ProjectID,Status,SubmissionDate,LastUpdateDate,Content,Replies(" + LIST_DELIMITER + " separated)";
    }

    @Override
    protected Enquiry parseCsvRow(String[] values) {
        if (values.length < 8) {
             System.err.println(TextFormatUtil.error("Skipping malformed enquiry row: Expected 8 columns, found " + values.length + ". Line: [" + String.join(",", values) + "]"));
             return null;
         }

        int enquiryId = safeParseInt(values[0], -1);
        String submitterNric = safeParseString(values[1]);
        int projectId = safeParseInt(values[2], -1);

        EnquiryStatus status = null;
        String statusStr = safeParseString(values[3]);
        if (!statusStr.isEmpty()) {
            try {
                 status = EnquiryStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                 System.err.println(TextFormatUtil.warning("Skipping enquiry row: Invalid Status value '" + values[3] + "' for EnquiryID " + values[0]));
                 return null;
            }
        } else {
             System.err.println(TextFormatUtil.warning("Skipping enquiry row: Status is empty for EnquiryID " + values[0]));
             return null;
        }


        Date submissionDate = DateUtils.parseDate(safeParseString(values[4]));
        Date lastUpdateDate = DateUtils.parseDate(safeParseString(values[5]));
        String content = safeParseString(values[6]);
        // *** Use updated LIST_DELIMITER for parsing replies ***
        List<String> replies = parseStringList(values[7]);

        if (enquiryId <= 0 || submitterNric.isEmpty() || projectId <= 0 || status == null || submissionDate == null || lastUpdateDate == null || content.isEmpty()) {
             System.err.println(TextFormatUtil.error("Skipping enquiry row: Invalid critical data after parsing for row starting with EnquiryID " + values[0]));
             return null;
        }


        return new Enquiry(enquiryId, submitterNric, projectId, content, replies, status, submissionDate, lastUpdateDate);
    }

    @Override
    protected String formatCsvRow(Enquiry enquiry) {
        // Basic CSV quoting for content which might contain commas
        String quotedContent = "\"" + enquiry.getContent().replace("\"", "\"\"") + "\"";

        return String.join(CSV_DELIMITER,
                String.valueOf(enquiry.getEnquiryId()),
                enquiry.getSubmitterNric(),
                String.valueOf(enquiry.getProjectId()),
                enquiry.getStatus().name(),
                DateUtils.formatDate(enquiry.getSubmissionDate()),
                DateUtils.formatDate(enquiry.getLastUpdateDate()),
                quotedContent,
                // *** Use updated LIST_DELIMITER for formatting replies ***
                formatStringList(enquiry.getReplies())
        );
    }

    @Override
    protected Integer getKey(Enquiry enquiry) {
        return enquiry.getEnquiryId();
    }

    // --- Helper methods (now using protected LIST_DELIMITER from Abstract class) ---
    // *** FIX: Changed visibility from private to protected ***
    protected List<String> parseStringList(String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Split by LIST_DELIMITER (handle potential regex special chars if needed)
        return Arrays.stream(data.split("\\" + LIST_DELIMITER)) // Escape if delimiter is special regex char
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                      // Basic de-quoting if replies were quoted (optional)
                     // .map(s -> s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1).replace("\"\"", "\"") : s)
                     .collect(Collectors.toList());
    }

    // *** FIX: Changed visibility from private to protected ***
    protected String formatStringList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        // Join using LIST_DELIMITER. Consider quoting elements if they might contain the delimiter.
        return list.stream()
                  // Basic quoting if element contains comma or list delimiter or quote
                  // .map(s -> "\"" + s.replace("\"", "\"\"") + "\"")
                   .collect(Collectors.joining(LIST_DELIMITER));
    }
}