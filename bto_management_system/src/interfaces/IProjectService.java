package interfaces;

import models.Project;
import enums.FlatType;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IProjectService {
    /**
     * Retrieves all projects in the system.
     * @return List of all Project objects.
     */
    List<Project> getAllProjects();

    /**
     * Retrieves projects visible and eligible for a specific applicant based on their profile and project status/dates/visibility.
     * @param applicantNric NRIC of the applicant.
     * @return List of eligible and visible Project objects.
     */
    List<Project> getVisibleProjectsForApplicant(String applicantNric);

    /**
     * Retrieves a specific project by its ID.
     * @param projectId ID of the project.
     * @return Project object or null if not found.
     */
    Project getProjectById(int projectId);

    /**
     * Creates a new BTO project. Requires manager privileges.
     * Performs checks like manager availability during the period.
     * @param managerNric NRIC of the HDB Manager creating the project.
     * @param name Project name.
     * @param neighborhood Project location.
     * @param units Map of FlatType to total unit count.
     * @param open Application opening date.
     * @param close Application closing date.
     * @param slots Maximum number of HDB Officer slots (1-10).
     * @return The created Project object, or null on failure (e.g., validation error, manager conflict).
     */
    Project createProject(String managerNric, String name, String neighborhood, Map<FlatType, Integer> units, Date open, Date close, int slots);

    /**
     * Edits the details of an existing project. Requires manager privileges (assigned manager).
     * @param projectId ID of the project to edit.
     * @param updatedDetails Project object containing the new details (or pass individual fields).
     *                       Care must be taken regarding which fields are editable.
     * @param editorNric NRIC of the manager attempting the edit.
     * @return true if edit successful, false otherwise.
     */
    boolean editProject(int projectId, Project updatedDetails, String editorNric); // Consider passing specific fields instead of whole object

    /**
     * Deletes a project. Requires manager privileges (assigned manager).
     * Handles cleanup of related entities (applications, enquiries, etc.) carefully.
     * May be prevented if active bookings exist.
     * @param projectId ID of the project to delete.
     * @param deleterNric NRIC of the manager attempting deletion.
     * @return true if deletion successful, false otherwise.
     */
    boolean deleteProject(int projectId, String deleterNric);

    /**
     * Toggles the visibility of a project for applicants. Requires manager privileges (assigned manager).
     * @param projectId ID of the project.
     * @param isVisible true to make visible, false to hide.
     * @param managerNric NRIC of the manager performing the action.
     * @return true if toggle successful, false otherwise.
     */
    boolean toggleProjectVisibility(int projectId, boolean isVisible, String managerNric);

    /**
     * Retrieves all projects managed by a specific HDB Manager.
     * @param managerNric NRIC of the manager.
     * @return List of Project objects managed by this manager.
     */
    List<Project> getProjectsManagedBy(String managerNric);

    /**
     * Adds an officer's NRIC to the list of assigned officers for a project.
     * Typically called by HDBManagerService after approving a registration.
     * @param projectId ID of the project.
     * @param officerNric NRIC of the officer to add.
     * @return true if officer added successfully (and slots available), false otherwise.
     */
    boolean addOfficerToProject(int projectId, String officerNric);

    /**
     * Removes an officer's NRIC from a project (e.g., if officer resigns/is removed).
     * @param projectId ID of the project.
     * @param officerNric NRIC of the officer to remove.
     * @return true if removal successful.
     */
    boolean removeOfficerFromProject(int projectId, String officerNric);

    /**
     * Decrements the available unit count for a specific flat type in a project. Used during booking.
     * @param projectId ID of the project.
     * @param type FlatType being booked.
     * @return true if unit count decremented successfully, false if no units were available.
     */
    boolean decrementProjectUnit(int projectId, FlatType type);

    /**
     * Increments the available unit count for a specific flat type in a project. Used if a booking is cancelled.
     * @param projectId ID of the project.
     * @param type FlatType being returned.
     * @return true if unit count incremented successfully.
     */
    boolean incrementProjectUnit(int projectId, FlatType type);

    /**
     * Checks if a given date falls within the application period of a project.
     * @param projectId ID of the project.
     * @param date The date to check.
     * @return true if the date is within the application period (inclusive), false otherwise.
     */
    boolean isProjectWithinApplicationPeriod(int projectId, Date date);

    /**
     * Retrieves the project an HDB Officer is currently assigned to handle.
     * @param officerNric NRIC of the HDB Officer.
     * @return Project object or null if not assigned.
     */
    Project getHandlingProjectForOfficer(String officerNric);
}