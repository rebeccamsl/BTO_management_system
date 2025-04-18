package models; 

import enums.FlatType; 
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class FlatBooking implements Serializable {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);

    private int bookingId;
    private int applicationId;
    private String applicantNric;
    private int projectId;
    private FlatType bookedFlatType;
    private String bookingOfficerNric;
    private Date bookingDate;

     // Constructor for new booking
    public FlatBooking(int applicationId, String applicantNric, int projectId, FlatType bookedFlatType, String bookingOfficerNric) {
        this.bookingId = idCounter.incrementAndGet();
        this.applicationId = applicationId;
        this.applicantNric = applicantNric;
        this.projectId = projectId;
        this.bookedFlatType = bookedFlatType;
        this.bookingOfficerNric = bookingOfficerNric;
        this.bookingDate = new Date();
    }

      // Constructor for loading existing booking
    public FlatBooking(int bookingId, int applicationId, String applicantNric, int projectId,
                       FlatType bookedFlatType, String bookingOfficerNric, Date bookingDate) {
        this.bookingId = bookingId;
        this.applicationId = applicationId;
        this.applicantNric = applicantNric;
        this.projectId = projectId;
        this.bookedFlatType = bookedFlatType;
        this.bookingOfficerNric = bookingOfficerNric;
        this.bookingDate = bookingDate;
         if (bookingId >= idCounter.get()) {
             idCounter.set(bookingId + 1);
         }
    }

    // Getters
    public int getBookingId() { return bookingId; }
    public int getApplicationId() { return applicationId; }
    public String getApplicantNric() { return applicantNric; }
    public int getProjectId() { return projectId; }
    public FlatType getBookedFlatType() { return bookedFlatType; }
    public String getBookingOfficerNric() { return bookingOfficerNric; }
    public Date getBookingDate() { return bookingDate; }

    @Override
    public String toString() {
        return "FlatBooking{" +
               "bookingId=" + bookingId + ", applicationId=" + applicationId + ", applicantNric='" + applicantNric + '\'' +
               ", projectId=" + projectId + ", bookedFlatType=" + bookedFlatType + ", bookingOfficerNric='" + bookingOfficerNric + '\'' +
               ", bookingDate=" + bookingDate + '}';
    }
    public static void resetIdCounter() { idCounter.set(0); }
    public static void updateIdCounter(int maxId) { if (maxId >= idCounter.get()) idCounter.set(maxId + 1); }
}