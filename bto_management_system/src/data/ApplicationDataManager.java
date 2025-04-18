package data;

import models.BTOApplication;
import enums.BTOApplicationStatus;
import enums.FlatType;
import utils.DateUtils;
import utils.TextFormatUtil; 

import java.io.IOException;
import java.util.Arrays; // 
import java.util.Date;
import java.util.Map;

/**
 * DataManager implementation for BTOApplication using CSV files.
 */
public class ApplicationDataManager extends AbstractCsvDataManager<Integer, BTOApplication> {

    @Override
    protected String getHeaderLine() {
        return "AppID,ApplicantNRIC,ProjectID,Status,AppliedFlatType,BookedFlatType,BookingID,WithdrawalRequested,SubmissionDate";
    }

    @Override
    protected BTOApplication parseCsvRow(String[] values) {
         // Expected number of columns based on header
         int EXPECTED_COLUMNS = 9;
         if (values.length < EXPECTED_COLUMNS) {
             System.err.println(TextFormatUtil.error("Skipping malformed application row: Expected " + EXPECTED_COLUMNS + " columns, found " + values.length + ". Line: " + Arrays.toString(values)));
             return null;
         }

        // Indices correspond to header order (0-based)
        int appIdIdx = 0;
        int nricIdx = 1;
        int projIdIdx = 2;
        int statusIdx = 3;
        int appliedTypeIdx = 4;
        int bookedTypeIdx = 5;
        int bookingIdIdx = 6;
        int withdrawalIdx = 7;
        int submitDateIdx = 8;

        // Use safe parsing helpers from AbstractCsvDataManager
        int appId = safeParseInt(values[appIdIdx], -1);
        String applicantNric = safeParseString(values[nricIdx]);
        int projectId = safeParseInt(values[projIdIdx], -1);

        // Enum Parsing for Status
        BTOApplicationStatus status = null;
        String statusStr = safeParseString(values[statusIdx]);
        if (!statusStr.isEmpty()) {
            try {
                status = BTOApplicationStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println(TextFormatUtil.warning("Skipping application row: Invalid Status value '" + values[statusIdx] + "' for AppID " + values[appIdIdx]));
                return null;
            }
        } else {
             System.err.println(TextFormatUtil.warning("Skipping application row: Status is empty for AppID " + values[appIdIdx]));
             return null;
        }

        // Enum Parsing for Applied Flat Type
        FlatType appliedFlatType = null;
        String appliedTypeStr = safeParseString(values[appliedTypeIdx]);
        if (!appliedTypeStr.isEmpty()) {
            try {
                appliedFlatType = FlatType.valueOf(appliedTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                 System.err.println(TextFormatUtil.warning("Skipping application row: Invalid AppliedFlatType value '" + values[appliedTypeIdx] + "' for AppID " + values[appIdIdx]));
                 return null;
            }
        } else {
             System.err.println(TextFormatUtil.warning("Skipping application row: AppliedFlatType is empty for AppID " + values[appIdIdx]));
             return null;
        }

        // Booked Flat Type
        FlatType bookedFlatType = null;
        String bookedTypeStr = safeParseString(values[bookedTypeIdx]);
        if (!bookedTypeStr.isEmpty()) {
             try {
                bookedFlatType = FlatType.valueOf(bookedTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                 System.err.println(TextFormatUtil.warning("Invalid BookedFlatType value '" + values[bookedTypeIdx] + "' for AppID " + values[appIdIdx] + ". Setting to null."));
            }
        }

        // Booking ID 
        Integer bookingId = null;
        String bookingIdStr = safeParseString(values[bookingIdIdx]);
        if (!bookingIdStr.isEmpty()) {
             try {
                 int parsedId = Integer.parseInt(bookingIdStr);
                 if (parsedId > 0) { // Assuming Booking IDs are positive
                     bookingId = parsedId;
                 } else {
                      System.err.println(TextFormatUtil.warning("Invalid non-positive BookingID value '" + values[bookingIdIdx] + "' for AppID " + values[appIdIdx] + ". Setting to null."));
                 }
             } catch (NumberFormatException e) {
                 System.err.println(TextFormatUtil.warning("Invalid BookingID value (not a number) '" + values[bookingIdIdx] + "' for AppID " + values[appIdIdx] + ". Setting to null."));
             }
        }

        // Withdrawal Requested
        boolean withdrawalRequested = Boolean.parseBoolean(safeParseString(values[withdrawalIdx]).toLowerCase()); // Ensure lowercase "true"/"false"

        // Submission Date
        Date submissionDate = DateUtils.parseDate(safeParseString(values[submitDateIdx]));

        // Final Critical Data Validation 
         if (appId <= 0 || applicantNric.isEmpty() || projectId <= 0 || status == null || appliedFlatType == null || submissionDate == null) {
              System.err.println(TextFormatUtil.error("Skipping application row: Invalid critical data (AppID, NRIC, ProjectID, Status, AppliedType, SubmitDate) after parsing for row starting with AppID " + values[appIdIdx] + ". Line: " + Arrays.toString(values)));
              return null;
         }

         // If status is BOOKED, bookedFlatType and bookingId should not be null
         if (status == BTOApplicationStatus.BOOKED && (bookedFlatType == null || bookingId == null)) {
              System.err.println(TextFormatUtil.warning("Data inconsistency warning: Application " + appId + " has status BOOKED but missing BookedFlatType or BookingID."));
         }
         // If status is not BOOKED, bookedFlatType and bookingId should be null
          if (status != BTOApplicationStatus.BOOKED && (bookedFlatType != null || bookingId != null)) {
              System.err.println(TextFormatUtil.warning("Data inconsistency warning: Application " + appId + " status is " + status + " but has BookedFlatType or BookingID set. Clearing them."));
              bookedFlatType = null;
              bookingId = null;
          }


        // Construct the object using parsed values
        return new BTOApplication(appId, applicantNric, projectId, status, appliedFlatType,
                                  bookedFlatType, bookingId, withdrawalRequested, submissionDate);
    }

    /**
     * Formats a BTOApplication object into a CSV string row.
     * @param app The BTOApplication object to format.
     * @return The formatted CSV string row.
     */
    @Override
    protected String formatCsvRow(BTOApplication app) {
        return String.join(CSV_DELIMITER,
                String.valueOf(app.getApplicationId()),
                app.getApplicantNric(),
                String.valueOf(app.getProjectId()),
                app.getStatus().name(), // Store enum name
                app.getAppliedFlatType().name(), // Store enum name
                (app.getBookedFlatType() != null) ? app.getBookedFlatType().name() : "", // Empty if null
                (app.getFlatBookingId() != null) ? String.valueOf(app.getFlatBookingId()) : "", // Empty if null
                String.valueOf(app.isWithdrawalRequested()).toLowerCase(), // Store as "true" or "false"
                DateUtils.formatDate(app.getSubmissionDate()) // Format date
        );
    }

    /**
     * Gets the key (Application ID) for the BTOApplication object.
     * @param app The BTOApplication object.
     * @return The Application ID.
     */
    @Override
    protected Integer getKey(BTOApplication app) {
        return app.getApplicationId();
    }
}