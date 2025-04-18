package models; 

import enums.BTOApplicationStatus; 
import enums.FlatType; 
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class BTOApplication implements Serializable {
    private static final long serialVersionUID = 1L;
    private static AtomicInteger idCounter = new AtomicInteger(0);

    private int applicationId;
    private String applicantNric;
    private int projectId;
    private BTOApplicationStatus status;
    private FlatType appliedFlatType;
    private FlatType bookedFlatType;
    private Integer flatBookingId;
    private boolean withdrawalRequested;
    private Date submissionDate;

    // Constructor for new application
    public BTOApplication(String applicantNric, int projectId, FlatType appliedFlatType) {
        this.applicationId = idCounter.incrementAndGet();
        this.applicantNric = applicantNric;
        this.projectId = projectId;
        this.status = BTOApplicationStatus.PENDING;
        this.appliedFlatType = appliedFlatType;
        this.bookedFlatType = null;
        this.flatBookingId = null;
        this.withdrawalRequested = false;
        this.submissionDate = new Date();
    }

     // Constructor for loading existing application
    public BTOApplication(int applicationId, String applicantNric, int projectId, BTOApplicationStatus status,
                          FlatType appliedFlatType, FlatType bookedFlatType, Integer flatBookingId,
                          boolean withdrawalRequested, Date submissionDate) {
        this.applicationId = applicationId;
        this.applicantNric = applicantNric;
        this.projectId = projectId;
        this.status = status;
        this.appliedFlatType = appliedFlatType;
        this.bookedFlatType = bookedFlatType;
        this.flatBookingId = flatBookingId;
        this.withdrawalRequested = withdrawalRequested;
        this.submissionDate = submissionDate;
         if (applicationId >= idCounter.get()) {
             idCounter.set(applicationId + 1);
         }
    }

    // Getters
    public int getApplicationId() { return applicationId; }
    public String getApplicantNric() { return applicantNric; }
    public int getProjectId() { return projectId; }
    public BTOApplicationStatus getStatus() { return status; }
    public FlatType getAppliedFlatType() { return appliedFlatType; }
    public FlatType getBookedFlatType() { return bookedFlatType; }
    public Integer getFlatBookingId() { return flatBookingId; }
    public boolean isWithdrawalRequested() { return withdrawalRequested; }
    public Date getSubmissionDate() { return submissionDate; }

    // Setters
    public void setStatus(BTOApplicationStatus status) { this.status = status; }
    public void setBookedFlatType(FlatType bookedFlatType) {
        if (this.status == BTOApplicationStatus.BOOKED) {
            this.bookedFlatType = bookedFlatType;
        } else {
             System.err.println("Warning: Attempted to set booked flat type for application " + applicationId + " while status is " + this.status);
        }
    }
    public void setFlatBookingId(Integer flatBookingId) {
        if (this.status == BTOApplicationStatus.BOOKED && flatBookingId != null) {
            this.flatBookingId = flatBookingId;
        } else if (flatBookingId == null) {
            this.flatBookingId = null;
        } else {
             System.err.println("Warning: Attempted to set flat booking ID for application " + applicationId + " while status is " + this.status + " or with null ID when booking");
        }
    }
    public void requestWithdrawal() { this.withdrawalRequested = true; }
    public void approveWithdrawal() {
        this.withdrawalRequested = false;
        this.setStatus(BTOApplicationStatus.WITHDRAWN);
    }
    public void rejectWithdrawal() { this.withdrawalRequested = false; }

    @Override
    public String toString() {
        return "BTOApplication{" +
               "applicationId=" + applicationId + ", applicantNric='" + applicantNric + '\'' + ", projectId=" + projectId +
               ", status=" + status + ", appliedFlatType=" + appliedFlatType + ", bookedFlatType=" + bookedFlatType +
               ", withdrawalRequested=" + withdrawalRequested + '}';
    }
    public static void resetIdCounter() { idCounter.set(0); }
    public static void updateIdCounter(int maxId) { if (maxId >= idCounter.get()) idCounter.set(maxId + 1); }
}