package interfaces;

import models.Enquiry;
import java.util.List;

public interface IEnquiryService {
    /**
     * Submits a new enquiry for a specific project.
     * @param submitterNric NRIC of the user submitting.
     * @param projectId ID of the project.
     * @param content The enquiry text.
     * @return The created Enquiry object, or null on failure.
     */
    Enquiry submitEnquiry(String submitterNric, int projectId, String content);

    /**
     * Retrieves all enquiries submitted by a specific user.
     * @param submitterNric NRIC of the user.
     * @return List of Enquiry objects.
     */
    List<Enquiry> viewMyEnquiries(String submitterNric);

    /**
     * Edits the content of an existing enquiry. Only the submitter can edit, and usually only if OPEN/ANSWERED.
     * @param enquiryId ID of the enquiry to edit.
     * @param newContent The updated enquiry text.
     * @param editorNric NRIC of the user attempting the edit (for verification).
     * @return true if edit successful, false otherwise.
     */
    boolean editEnquiry(int enquiryId, String newContent, String editorNric);

    /**
     * Deletes an enquiry. Only the submitter can delete.
     * @param enquiryId ID of the enquiry to delete.
     * @param deleterNric NRIC of the user attempting deletion (for verification).
     * @return true if deletion successful, false otherwise.
     */
    boolean deleteEnquiry(int enquiryId, String deleterNric);

    /**
     * Retrieves all enquiries associated with a specific project. For Officer/Manager view.
     * @param projectId ID of the project.
     * @return List of Enquiry objects.
     */
    List<Enquiry> viewProjectEnquiries(int projectId);

    /**
     * Retrieves all enquiries across all projects. For Manager view.
     * @return List of all Enquiry objects.
     */
    List<Enquiry> viewAllEnquiries();

    /**
     * Adds a reply to an enquiry. Typically done by Officer or Manager handling the project.
     * @param enquiryId ID of the enquiry to reply to.
     * @param replierNric NRIC of the Officer/Manager replying.
     * @param replyText The reply content.
     * @return true if reply added successfully, false otherwise.
     */
    boolean replyToEnquiry(int enquiryId, String replierNric, String replyText);

    /**
     * Marks an enquiry as closed (optional feature).
     * @param enquiryId ID of the enquiry to close.
     * @param closerNric NRIC of the user closing (for permission check).
     * @return true if closed successfully.
     */
    boolean closeEnquiry(int enquiryId, String closerNric);
}