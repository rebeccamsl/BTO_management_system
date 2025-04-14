package interfaces;

import models.FlatBooking;
import enums.FlatType;

public interface IFlatBookingService {
    /**
     * Creates a new flat booking record.
     * Updates the corresponding BTO application status to BOOKED and sets booking details.
     * Decrements the available unit count in the project (via ProjectService).
     * This is typically initiated by an HDB Officer.
     * @param applicationId ID of the SUCCESSFUL BTO application.
     * @param bookedFlatType The specific flat type chosen during booking.
     * @param officerNric NRIC of the officer facilitating the booking.
     * @return The created FlatBooking object, or null on failure (e.g., app not successful, units unavailable).
     */
    FlatBooking createBooking(int applicationId, FlatType bookedFlatType, String officerNric);

    /**
     * Generates a formatted string receipt for a given booking.
     * Retrieves applicant, project, and booking details.
     * @param bookingId ID of the flat booking.
     * @return A formatted string representing the receipt, or null if booking not found.
     */
    String generateBookingReceipt(int bookingId); // Or use Application ID? Booking ID seems better.

    /**
     * Retrieves a flat booking by its ID.
     * @param bookingId ID of the booking.
     * @return FlatBooking object or null if not found.
     */
    FlatBooking getBookingById(int bookingId);
}