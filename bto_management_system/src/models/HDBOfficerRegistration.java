package models; // Updated package

import enums.RequestStatus; // Updated import
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class HDBOfficerRegistration implements Serializable {
    private static final long serialVersionUID = 1L;
     private static AtomicInteger idCounter = new AtomicInteger(0);

    private int registrationId;
    private String officerNric;
    private int projectId;
    private RequestStatus status;
    private Date requestDate;
    private Date decisionDate;

    // Constructor for new registration request
    public HDBOfficerRegistration(String officerNric, int projectId) {
        this.registrationId = idCounter.incrementAndGet();
        this.officerNric = officerNric;
        this.projectId = projectId;
        this.status = RequestStatus.PENDING;
        this.requestDate = new Date();
        this.decisionDate = null;
    }

     // Constructor for loading existing registration
    public HDBOfficerRegistration(int registrationId, String officerNric, int projectId,
                                  RequestStatus status, Date requestDate, Date decisionDate) {
        this.registrationId = registrationId;
        this.officerNric = officerNric;
        this.projectId = projectId;
        this.status = status;
        this.requestDate = requestDate;
        this.decisionDate = decisionDate;
         if (registrationId >= idCounter.get()) {
             idCounter.set(registrationId + 1);
         }
    }

    // Getters
    public int getRegistrationId() { return registrationId; }
    public String getOfficerNric() { return officerNric; }
    public int getProjectId() { return projectId; }
    public RequestStatus getStatus() { return status; }
    public Date getRequestDate() { return requestDate; }
    public Date getDecisionDate() { return decisionDate; }

    // Setters (used by Manager Service)
    public void approve() {
        if (this.status == RequestStatus.PENDING) {
            this.status = RequestStatus.APPROVED;
            this.decisionDate = new Date();
        } else {
            System.err.println("Cannot approve registration " + registrationId + " with status " + status);
        }
    }
    public void reject() {
         if (this.status == RequestStatus.PENDING) {
            this.status = RequestStatus.REJECTED;
            this.decisionDate = new Date();
        } else {
             System.err.println("Cannot reject registration " + registrationId + " with status " + status);
        }
    }

    @Override
    public String toString() {
        return "HDBOfficerRegistration{" +
               "registrationId=" + registrationId + ", officerNric='" + officerNric + '\'' + ", projectId=" + projectId +
               ", status=" + status + ", requestDate=" + requestDate + '}';
    }
    public static void resetIdCounter() { idCounter.set(0); }
    public static void updateIdCounter(int maxId) { if (maxId >= idCounter.get()) idCounter.set(maxId + 1); }
}