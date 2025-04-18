package data;

import models.HDBOfficerRegistration;
import enums.RequestStatus;
import utils.DateUtils;
import utils.TextFormatUtil; 

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class HDBOfficerRegDataManager extends AbstractCsvDataManager<Integer, HDBOfficerRegistration> {

    @Override
    protected String getHeaderLine() {
        return "RegID,OfficerNRIC,ProjectID,Status,RequestDate,DecisionDate";
    }

    @Override
    protected HDBOfficerRegistration parseCsvRow(String[] values) {
         // Check column count FIRST
         if (values.length < 6) {
             System.err.println(TextFormatUtil.error("Skipping malformed officer registration row: Expected 6 columns, found " + values.length + ". Line: [" + String.join(",", values) + "]"));
             return null;
         }

        int regId = safeParseInt(values[0], -1);
        String officerNric = safeParseString(values[1]);
        int projectId = safeParseInt(values[2], -1);

        //Enum Parsing 
        RequestStatus status = null;
        String statusStr = safeParseString(values[3]);
        if (!statusStr.isEmpty()) {
            try {
                status = RequestStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println(TextFormatUtil.warning("Skipping officer registration row: Invalid Status value '" + values[3] + "' for RegID " + values[0]));
                return null; // Skip row if enum is invalid
            }
        } else {
            System.err.println(TextFormatUtil.warning("Skipping officer registration row: Status is empty for RegID " + values[0]));
            return null; // Skip if status is mandatory and empty
        }



        Date requestDate = DateUtils.parseDate(safeParseString(values[4]));
        Date decisionDate = DateUtils.parseDate(safeParseString(values[5])); // Can be null if parsing fails or string is empty

        // Final validation
        if (regId <= 0 || officerNric.isEmpty() || projectId <= 0 || status == null || requestDate == null) {
             System.err.println(TextFormatUtil.error("Skipping officer registration row: Invalid critical data after parsing for row starting with RegID " + values[0]));
             return null;
        }


        return new HDBOfficerRegistration(regId, officerNric, projectId, status, requestDate, decisionDate);
    }

    @Override
    protected String formatCsvRow(HDBOfficerRegistration reg) {
        return String.join(CSV_DELIMITER,
                String.valueOf(reg.getRegistrationId()),
                reg.getOfficerNric(),
                String.valueOf(reg.getProjectId()),
                reg.getStatus().name(), 
                DateUtils.formatDate(reg.getRequestDate()),
                DateUtils.formatDate(reg.getDecisionDate()) // Returns "" if date is null
        );
    }

    @Override
    protected Integer getKey(HDBOfficerRegistration reg) {
        return reg.getRegistrationId();
    }
}