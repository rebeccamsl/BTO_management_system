package interfaces;

public interface IHDBManagerService {
    /**
     * Approves a pending HDB Officer registration request for a project managed by this manager.
     * Updates registration status, assigns officer to project (via ProjectService), updates officer profile.
     * Checks for available slots.
     * @param registrationId ID of the registration request.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if approval successful, false otherwise.
     */
    boolean approveOfficerRegistration(int registrationId, String managerNric);

    /**
     * Rejects a pending HDB Officer registration request.
     * Updates registration status.
     * @param registrationId ID of the registration request.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if rejection successful, false otherwise.
     */
    boolean rejectOfficerRegistration(int registrationId, String managerNric);

    /**
     * Approves a pending BTO application.
     * Checks flat availability for the applied type. Updates application status to SUCCESSFUL.
     * @param applicationId ID of the application.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if approval successful, false otherwise (e.g., no units, not manager).
     */
    boolean approveApplication(int applicationId, String managerNric);

    /**
     * Rejects a pending BTO application.
     * Updates application status to UNSUCCESSFUL.
     * @param applicationId ID of the application.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if rejection successful, false otherwise.
     */
    boolean rejectApplication(int applicationId, String managerNric);

    /**
     * Approves an applicant's request to withdraw their application.
     * Updates application status (e.g., to WITHDRAWN). Increments available unit count if booked.
     * @param applicationId ID of the application with a withdrawal request.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if approval successful, false otherwise.
     */
    boolean approveWithdrawal(int applicationId, String managerNric);

    /**
     * Rejects an applicant's request to withdraw their application.
     * Clears the withdrawal request flag.
     * @param applicationId ID of the application with a withdrawal request.
     * @param managerNric NRIC of the manager performing the action (for verification).
     * @return true if rejection successful, false otherwise.
     */
    boolean rejectWithdrawal(int applicationId, String managerNric);
}