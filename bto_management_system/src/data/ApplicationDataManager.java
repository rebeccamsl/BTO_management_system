package data;

import models.BTOApplication;
import enums.BTOApplicationStatus;
import enums.FlatType;
import utils.DateUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class ApplicationDataManager extends AbstractCsvDataManager<Integer, BTOApplication> {

    @Override
    protected String getHeaderLine() {
        return "AppID,ApplicantNRIC,ProjectID,Status,AppliedFlatType,BookedFlatType,BookingID,WithdrawalRequested,SubmissionDate";
    }

    @Override
    protected BTOApplication parseCsvRow(String[] values) {
         if (values.length < 9) {
             System.err.println("Skipping malformed application row: Not enough columns.");
             return null;
         }

        int appId = safeParseInt(values[0], -1);
        String applicantNric = safeParseString(values[1]);
        int projectId = safeParseInt(values[2], -1);
        BTOApplicationStatus status = BTOApplicationStatus.valueOf(safeParseString(values[3]).toUpperCase());
        FlatType appliedFlatType = FlatType.valueOf(safeParseString(values[4]).toUpperCase());
        FlatType bookedFlatType = null;
        if (!safeParseString(values[5]).isEmpty()) {
            bookedFlatType = FlatType.valueOf(safeParseString(values[5]).toUpperCase());
        }
        Integer bookingId = null;
        if (!safeParseString(values[6]).isEmpty()) {
             bookingId = safeParseInt(values[6], -1);
             if (bookingId <= 0) bookingId = null; // Treat invalid IDs as null
        }
        boolean withdrawalRequested = safeParseBoolean(values[7], false);
        Date submissionDate = DateUtils.parseDate(safeParseString(values[8]));

         if (appId <= 0 || applicantNric.isEmpty() || projectId <= 0 || submissionDate == null) {
              System.err.println("Skipping application row: Invalid critical data (IDs, NRIC, Date) for row starting with ID " + values[0]);
              return null;
         }


        return new BTOApplication(appId, applicantNric, projectId, status, appliedFlatType,
                                  bookedFlatType, bookingId, withdrawalRequested, submissionDate);
    }

    @Override
    protected String formatCsvRow(BTOApplication app) {
        return String.join(CSV_DELIMITER,
                String.valueOf(app.getApplicationId()),
                app.getApplicantNric(),
                String.valueOf(app.getProjectId()),
                app.getStatus().name(),
                app.getAppliedFlatType().name(),
                (app.getBookedFlatType() != null) ? app.getBookedFlatType().name() : "",
                (app.getFlatBookingId() != null) ? String.valueOf(app.getFlatBookingId()) : "",
                String.valueOf(app.isWithdrawalRequested()),
                DateUtils.formatDate(app.getSubmissionDate())
        );
    }

    @Override
    protected Integer getKey(BTOApplication app) {
        return app.getApplicationId();
    }
}