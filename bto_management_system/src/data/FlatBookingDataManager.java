package data;

import models.FlatBooking;
import enums.FlatType;
import utils.DateUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class FlatBookingDataManager extends AbstractCsvDataManager<Integer, FlatBooking> {

    @Override
    protected String getHeaderLine() {
        return "BookingID,AppID,ApplicantNRIC,ProjectID,BookedFlatType,OfficerNRIC,BookingDate";
    }

    @Override
    protected FlatBooking parseCsvRow(String[] values) {
        if (values.length < 7) {
             System.err.println("Skipping malformed flat booking row: Not enough columns.");
             return null;
         }

        int bookingId = safeParseInt(values[0], -1);
        int appId = safeParseInt(values[1], -1);
        String applicantNric = safeParseString(values[2]);
        int projectId = safeParseInt(values[3], -1);
        FlatType bookedFlatType = FlatType.valueOf(safeParseString(values[4]).toUpperCase());
        String officerNric = safeParseString(values[5]);
        Date bookingDate = DateUtils.parseDate(safeParseString(values[6]));

         if (bookingId <= 0 || appId <= 0 || applicantNric.isEmpty() || projectId <= 0 || officerNric.isEmpty() || bookingDate == null) {
             System.err.println("Skipping flat booking row: Invalid critical data for row starting with ID " + values[0]);
             return null;
         }


        return new FlatBooking(bookingId, appId, applicantNric, projectId, bookedFlatType, officerNric, bookingDate);
    }

    @Override
    protected String formatCsvRow(FlatBooking booking) {
        return String.join(CSV_DELIMITER,
                String.valueOf(booking.getBookingId()),
                String.valueOf(booking.getApplicationId()),
                booking.getApplicantNric(),
                String.valueOf(booking.getProjectId()),
                booking.getBookedFlatType().name(),
                booking.getBookingOfficerNric(),
                DateUtils.formatDate(booking.getBookingDate())
        );
    }

    @Override
    protected Integer getKey(FlatBooking booking) {
        return booking.getBookingId();
    }
}