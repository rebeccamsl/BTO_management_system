package interfaces;

import models.*;
import enums.*;
import java.util.List;
import java.util.Map;

public interface IApplicantService {

    /**
     * Finds the currently active (Pending, Successful, Booked) application for an applicant.
     * @param applicantNric NRIC of the applicant.
     * @return The BTOApplication object, or null if none found or only unsuccessful/withdrawn exist.
     */
    BTOApplication viewApplicationStatus(String applicantNric);

    /**
     * Creates a new BTO application if the applicant is eligible and doesn't have an active application.
     * Performs eligibility checks based on age, marital status, flat type, and project availability/period.
     * @param applicantNric NRIC of the applicant.
     * @param projectId ID of the project to apply for.
     * @param flatType The specific flat type the applicant is applying for.
     * @return The created BTOApplication object, or null if application failed.
     */
    BTOApplication applyForProject(String applicantNric, int projectId, FlatType flatType);

    /**
     * Marks an application with a withdrawal request. Manager approval is needed separately.
     * @param applicationId The ID of the application to withdraw.
     * @param applicantNric The NRIC of the applicant making the request (for verification).
     * @return true if the request was successfully marked, false otherwise (e.g., app not found, already withdrawn).
     */
    boolean requestWithdrawal(int applicationId, String applicantNric);

     /**
     * Filters a list of projects based on given criteria.
     * @param projects The list of projects to filter.
     * @param filters A map where keys are filter types (e.g., "neighborhood", "flatType") and values are the desired filter values.
     * @return A filtered list of projects.
     */
    List<Project> filterProjects(List<Project> projects, Map<String, String> filters);
}