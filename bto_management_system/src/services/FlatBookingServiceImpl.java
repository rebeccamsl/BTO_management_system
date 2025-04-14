package services;

import interfaces.IFlatBookingService;
import interfaces.IProjectService; // Needed to decrement units
import models.*;
import enums.*;
import stores.DataStore;
import utils.DateUtils;
import utils.TextFormatUtil;

public class FlatBookingServiceImpl implements IFlatBookingService {

    private final IProjectService projectService; // Inject dependency

    public FlatBookingServiceImpl() {
        this.projectService = new ProjectServiceImpl(); // Simple instantiation
    }

    @Override
    public FlatBooking createBooking(int applicationId, FlatType bookedFlatType, String officerNric) {
        BTOApplication application = DataStore.getApplicationById(applicationId);
        User officer = DataStore.getUserByNric(officerNric);
        Project project = (application != null) ? DataStore.getProjectById(application.getProjectId()) : null;

        // 1. Validation
        if (application == null || officer == null || officer.getRole() != UserRole.OFFICER || project == null) {
             System.err.println(TextFormatUtil.error("Booking failed: Invalid application, officer, or project."));
             return null;
        }
         // Check if Officer is assigned to this project
         if (!project.getAssignedHDBOfficerNrics().contains(officerNric)) {
              System.err.println(TextFormatUtil.error("Booking failed: Officer " + officerNric + " is not authorized to manage bookings for project " + project.getProjectId() + "."));
              return null;
         }

        if (application.getStatus() != BTOApplicationStatus.SUCCESSFUL) {
             System.err.println(TextFormatUtil.error("Booking failed: Application status must be SUCCESSFUL (Current: " + application.getStatus() + ")."));
             return null;
        }
        // Check if the booked type is valid for the project (should be, but safe check)
        if (project.getTotalUnits().getOrDefault(bookedFlatType, 0) <= 0) {
             System.err.println(TextFormatUtil.error("Booking failed: Project does not offer the selected flat type: " + bookedFlatType.getDisplayName() + "."));
             return null;
        }


        // 2. Attempt to decrement unit count (atomicity check)
        if (!projectService.decrementProjectUnit(application.getProjectId(), bookedFlatType)) {
            // decrementProjectUnit prints its own error if no units available
            return null; // Failed to secure a unit
        }

        // 3. Create Booking Record
        FlatBooking newBooking = new FlatBooking(applicationId, application.getApplicantNric(),
                                                 application.getProjectId(), bookedFlatType, officerNric);
        DataStore.addFlatBooking(newBooking);

        // 4. Update Application Status and Details
        application.setStatus(BTOApplicationStatus.BOOKED);
        application.setBookedFlatType(bookedFlatType); // Set the type confirmed during booking
        application.setFlatBookingId(newBooking.getBookingId()); // Link application to booking

        // 5. Persist All Changes
        DataStore.saveAllData();

        return newBooking;
    }

    @Override
    public String generateBookingReceipt(int bookingId) {
        FlatBooking booking = DataStore.getFlatBookingById(bookingId);
        if (booking == null) {
            return TextFormatUtil.error("Receipt generation failed: Booking ID " + bookingId + " not found.");
        }

        BTOApplication application = DataStore.getApplicationById(booking.getApplicationId());
        User applicant = DataStore.getUserByNric(booking.getApplicantNric());
        Project project = DataStore.getProjectById(booking.getProjectId());
        User officer = DataStore.getUserByNric(booking.getBookingOfficerNric());

        // Handle potential nulls if data is inconsistent (though shouldn't happen with good FKs)
        if (application == null || applicant == null || project == null || officer == null) {
             return TextFormatUtil.error("Receipt generation failed: Could not retrieve all required information for booking ID " + bookingId + ".");
        }


        // Format the receipt string
        StringBuilder receipt = new StringBuilder();
        receipt.append("\n=========================================\n");
        receipt.append("      BTO Flat Booking Receipt\n");
        receipt.append("=========================================\n");
        receipt.append(String.format("Booking ID      : %d\n", booking.getBookingId()));
        receipt.append(String.format("Booking Date    : %s\n", DateUtils.formatDate(booking.getBookingDate())));
        receipt.append(String.format("Application ID  : %d\n", application.getApplicationId()));
        receipt.append("\n--- Applicant Details ---\n");
        receipt.append(String.format("Name            : %s\n", applicant.getName()));
        receipt.append(String.format("NRIC            : %s\n", applicant.getNric()));
        receipt.append(String.format("Age             : %d\n", applicant.getAge()));
        receipt.append(String.format("Marital Status  : %s\n", applicant.getMaritalStatus().name()));
        receipt.append("\n--- Booking Details ---\n");
        receipt.append(String.format("Project Name    : %s\n", project.getProjectName()));
        receipt.append(String.format("Neighborhood    : %s\n", project.getNeighborhood()));
        receipt.append(String.format("Booked Flat Type: %s\n", booking.getBookedFlatType().getDisplayName()));
        receipt.append("\n--- Officer Details ---\n");
        receipt.append(String.format("Booking Officer : %s (%s)\n", officer.getName(), officer.getNric()));
        receipt.append("=========================================\n");

        return receipt.toString();
    }

     @Override
     public FlatBooking getBookingById(int bookingId) {
         return DataStore.getFlatBookingById(bookingId);
     }
}