package services;

import interfaces.IEnquiryService;
import models.Enquiry;
import models.Project;
import models.User;
import stores.AuthStore;
import stores.DataStore;
import enums.*; // Import necessary enums
import utils.TextFormatUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EnquiryServiceImpl implements IEnquiryService {

    @Override
    public Enquiry submitEnquiry(String submitterNric, int projectId, String content) {
        User submitter = DataStore.getUserByNric(submitterNric);
        Project project = DataStore.getProjectById(projectId);

        if (submitter == null || project == null) {
            System.err.println(TextFormatUtil.error("Submit enquiry failed: User or Project not found."));
            return null;
        }
        if (content == null || content.trim().isEmpty()) {
             System.err.println(TextFormatUtil.error("Submit enquiry failed: Content cannot be empty."));
             return null;
        }

        Enquiry newEnquiry = new Enquiry(submitterNric, projectId, content.trim());
        DataStore.addEnquiry(newEnquiry);
        DataStore.saveAllData(); // Persist
        return newEnquiry;
    }

    @Override
    public List<Enquiry> viewMyEnquiries(String submitterNric) {
        return DataStore.getEnquiries().values().stream()
                .filter(e -> e.getSubmitterNric().equals(submitterNric))
                .sorted(Comparator.comparing(Enquiry::getSubmissionDate).reversed()) // Show newest first
                .collect(Collectors.toList());
    }

    @Override
    public boolean editEnquiry(int enquiryId, String newContent, String editorNric) {
        Enquiry enquiry = DataStore.getEnquiryById(enquiryId);

        if (enquiry == null) {
             System.err.println(TextFormatUtil.error("Edit enquiry failed: Enquiry not found."));
             return false;
        }
        if (!enquiry.getSubmitterNric().equals(editorNric)) {
            System.err.println(TextFormatUtil.error("Edit enquiry failed: Only the submitter can edit their enquiry."));
            return false;
        }
         if (enquiry.getStatus() == EnquiryStatus.CLOSED) {
             System.err.println(TextFormatUtil.error("Edit enquiry failed: Cannot edit a closed enquiry."));
             return false;
         }
         if (newContent == null || newContent.trim().isEmpty()) {
             System.err.println(TextFormatUtil.error("Edit enquiry failed: New content cannot be empty."));
             return false;
         }


        enquiry.setContent(newContent.trim()); // Model handles timestamp update
        DataStore.saveAllData(); // Persist
        return true;
    }

    @Override
    public boolean deleteEnquiry(int enquiryId, String deleterNric) {
        Enquiry enquiry = DataStore.getEnquiryById(enquiryId);

        if (enquiry == null) {
             System.err.println(TextFormatUtil.error("Delete enquiry failed: Enquiry not found."));
             return false;
        }
        if (!enquiry.getSubmitterNric().equals(deleterNric)) {
            System.err.println(TextFormatUtil.error("Delete enquiry failed: Only the submitter can delete their enquiry."));
            return false;
        }

        DataStore.removeEnquiry(enquiryId);
        DataStore.saveAllData(); // Persist
        return true;
    }

    @Override
    public List<Enquiry> viewProjectEnquiries(int projectId) {
         // Check if project exists? Optional, stream will just be empty.
        return DataStore.getEnquiries().values().stream()
                .filter(e -> e.getProjectId() == projectId)
                .sorted(Comparator.comparing(Enquiry::getSubmissionDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Enquiry> viewAllEnquiries() {
         // Typically only for Manager
        User currentUser = AuthStore.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != UserRole.MANAGER) {
            System.err.println(TextFormatUtil.error("Access denied: Only HDB Managers can view all enquiries."));
            return new ArrayList<>(); // Return empty list
        }

        return new ArrayList<>(DataStore.getEnquiries().values())
                .stream()
                .sorted(Comparator.comparing(Enquiry::getSubmissionDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean replyToEnquiry(int enquiryId, String replierNric, String replyText) {
        Enquiry enquiry = DataStore.getEnquiryById(enquiryId);
        User replier = DataStore.getUserByNric(replierNric);
        Project project; // Project associated with the enquiry

        if (enquiry == null || replier == null) {
            System.err.println(TextFormatUtil.error("Reply failed: Enquiry or Replier not found."));
            return false;
        }
        project = DataStore.getProjectById(enquiry.getProjectId());
        if (project == null) {
             System.err.println(TextFormatUtil.error("Reply failed: Project associated with enquiry not found."));
             return false; // Should not happen if data is consistent
        }

         if (replyText == null || replyText.trim().isEmpty()) {
            System.err.println(TextFormatUtil.error("Reply failed: Reply text cannot be empty."));
            return false;
        }


        // Permission Check: Only Officer handling the project or the Manager handling the project (or any Manager?)
        boolean canReply = false;
        String replierInfo = replier.getName() + " (" + replier.getRole() + ")";

        if (replier.getRole() == UserRole.OFFICER) {
            // Check if this officer is assigned to this project
            if (project.getAssignedHDBOfficerNrics().contains(replierNric)) {
                canReply = true;
            }
        } else if (replier.getRole() == UserRole.MANAGER) {
            // Check if this manager is assigned to this project
             if (project.getAssignedHDBManagerNric().equals(replierNric)) {
                 canReply = true;
             }
             // Brief allows manager to view ALL, but reply only to handled? Let's stick to handled for replies.
             // If manager should reply to any, remove the check above for manager role.
        }

        if (!canReply) {
             System.err.println(TextFormatUtil.error("Reply failed: You do not have permission to reply to enquiries for this project."));
             return false;
        }


        // Add reply and update status
        enquiry.addReply(replierInfo, replyText.trim()); // Model updates status to ANSWERED
        DataStore.saveAllData(); // Persist
        return true;
    }

    @Override
    public boolean closeEnquiry(int enquiryId, String closerNric) {
        Enquiry enquiry = DataStore.getEnquiryById(enquiryId);
        User closer = DataStore.getUserByNric(closerNric);
        Project project;

         if (enquiry == null || closer == null) {
             System.err.println(TextFormatUtil.error("Close enquiry failed: Enquiry or User not found."));
             return false;
         }
         project = DataStore.getProjectById(enquiry.getProjectId());
         if (project == null) {
             System.err.println(TextFormatUtil.error("Close enquiry failed: Project not found."));
             return false;
         }

         // Permission Check: Only officer/manager handling the project? Or submitter?
         // Let's assume Officer/Manager handling it can close.
         boolean canClose = false;
         if (closer.getRole() == UserRole.OFFICER && project.getAssignedHDBOfficerNrics().contains(closerNric)) {
             canClose = true;
         } else if (closer.getRole() == UserRole.MANAGER && project.getAssignedHDBManagerNric().equals(closerNric)) {
              canClose = true;
         }
          // else if (enquiry.getSubmitterNric().equals(closerNric)) { // Allow submitter to close?
          //    canClose = true;
          // }


         if (!canClose) {
             System.err.println(TextFormatUtil.error("Close enquiry failed: Insufficient permissions."));
             return false;
         }

         if (enquiry.getStatus() == EnquiryStatus.CLOSED) {
             System.err.println(TextFormatUtil.warning("Enquiry already closed."));
             return false; // Not an error, but action not needed
         }

        enquiry.closeEnquiry(); // Model updates status
        DataStore.saveAllData(); // Persist
        return true;
    }
}