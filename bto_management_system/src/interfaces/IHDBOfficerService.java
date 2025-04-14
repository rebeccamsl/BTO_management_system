package interfaces;

import models.BTOApplication;
import models.HDBOfficerRegistration;
import java.util.List;

public interface IHDBOfficerService {
    /**
     * Creates a registration request for an HDB Officer to handle a specific project.
     * Performs eligibility checks (not already applicant, not handling conflicting project).
     * @param officerNric NRIC of the officer applying.
     * @param projectId ID of the project to register for.
     * @return The created HDBOfficerRegistration object (status PENDING), or null if ineligible.
     */
    HDBOfficerRegistration registerForProject(String officerNric, int projectId);

    /**
     * Retrieves all registration requests made by a specific HDB Officer.
     * @param officerNric NRIC of the officer.
     * @return List of HDBOfficerRegistration objects.
     */
    List<HDBOfficerRegistration> getOfficerRegistrations(String officerNric);

    /**
     * Retrieves pending registration requests for a specific project (for Manager view).
     * @param projectId ID of the project.
     * @return List of HDBOfficerRegistration objects with PENDING status.
     */
    List<HDBOfficerRegistration> getPendingRegistrationsForProject(int projectId);

    /**
     * Checks if an officer is eligible to register for a project based on brief's criteria.
     * (Not an applicant for the project, not handling another project in the same period).
     * @param officerNric Officer's NRIC.
     * @param projectId Project ID.
     * @return true if eligible, false otherwise.
     */
    boolean checkOfficerEligibilityForRegistration(String officerNric, int projectId);

    /**
     * Retrieves a BTO application suitable for booking (Status: SUCCESSFUL) using the applicant's NRIC.
     * @param applicantNric NRIC of the applicant whose application to retrieve.
     * @return The BTOApplication object, or null if no successful application found for this NRIC.
     */
    BTOApplication retrieveApplicationForBooking(String applicantNric);
}