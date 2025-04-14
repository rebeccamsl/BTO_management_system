package services;

import interfaces.IProjectService;
import models.*;
import enums.*;
import stores.DataStore;
import stores.AuthStore; // To check user roles for actions
import utils.DateUtils;
import utils.TextFormatUtil; // Added for error messages

import java.util.*; // For List, Map etc.
import java.util.stream.Collectors;


public class ProjectServiceImpl implements IProjectService {

    // Constants for eligibility rules
    private static final int MIN_SINGLE_AGE = 35;
    private static final int MIN_MARRIED_AGE = 21;

    @Override
    public List<Project> getAllProjects() {
        // Manager view - return all projects regardless of visibility or dates
         // Optionally sort them
         return new ArrayList<>(DataStore.getProjects().values())
                 .stream()
                 .sorted(Comparator.comparing(Project::getProjectName))
                 .collect(Collectors.toList());
    }

    @Override
    public List<Project> getVisibleProjectsForApplicant(String applicantNric) {
        User applicant = DataStore.getUserByNric(applicantNric);
         // Allow Officers to view projects like applicants
         if (applicant == null || !(applicant.getRole() == UserRole.APPLICANT || applicant.getRole() == UserRole.OFFICER)) {
             System.err.println(TextFormatUtil.error("Error: User not found or not an Applicant/Officer."));
             return Collections.emptyList();
        }

        List<Project> allProjects = new ArrayList<>(DataStore.getProjects().values());
        Date today = new Date(); // Use current date for filtering

        return allProjects.stream()
                .filter(Project::isVisible) // Must be visible
                .filter(p -> p.isWithinApplicationPeriod(today)) // Must be within application period
                .filter(p -> isApplicantEligibleForAnyFlatInProject(applicant, p)) // Check eligibility based on rules for *any* flat type offered
                .sorted(Comparator.comparing(Project::getProjectName)) // Default sort by name
                .collect(Collectors.toList());
    }

     // Helper method for eligibility check for *any* flat type in the project
     // This determines if the project shows up in the list at all for the applicant.
     // The specific flat type choice is handled during application.
    private boolean isApplicantEligibleForAnyFlatInProject(User applicant, Project project) {
        int age = applicant.getAge();
        MaritalStatus status = applicant.getMaritalStatus();

         boolean offersTwoRoom = project.getTotalUnits().getOrDefault(FlatType.TWO_ROOM, 0) > 0;
         boolean offersThreeRoom = project.getTotalUnits().getOrDefault(FlatType.THREE_ROOM, 0) > 0;

         if (status == MaritalStatus.SINGLE) {
            // Single, >= 35 needs 2-Room to be eligible for the project listing
            return age >= MIN_SINGLE_AGE && offersTwoRoom;
        } else if (status == MaritalStatus.MARRIED) {
            // Married, >= 21 needs either 2-Room or 3-Room
            return age >= MIN_MARRIED_AGE && (offersTwoRoom || offersThreeRoom);
        } else {
            return false; // Should not happen
        }
    }


    @Override
    public Project getProjectById(int projectId) {
        return DataStore.getProjectById(projectId);
    }

     @Override
    public Project createProject(String managerNric, String name, String neighborhood, Map<FlatType, Integer> units, Date open, Date close, int slots) {
          User manager = DataStore.getUserByNric(managerNric);
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Create project failed: Only HDB Managers can create projects."));
               return null;
          }

          if (open == null || close == null || open.after(close)) {
             System.err.println(TextFormatUtil.error("Create project failed: Invalid application dates. Closing date must be on or after opening date."));
             return null;
         }
         if (slots <= 0 || slots > 10) {
             System.err.println(TextFormatUtil.error("Create project failed: Invalid number of officer slots (must be 1-10)."));
             return null;
         }
          if (isManagerHandlingAnotherProjectInPeriod(managerNric, open, close)) {
             System.err.println(TextFormatUtil.error("Create project failed: Manager ("+managerNric+") is already handling another project during this application period."));
             return null;
         }
          // Validate units map (e.g., non-negative values)
          if (units == null || units.entrySet().stream().anyMatch(entry -> entry.getValue() < 0)) {
              System.err.println(TextFormatUtil.error("Create project failed: Unit counts cannot be negative."));
              return null;
          }
          // Ensure at least one unit type is specified? Optional validation.
           if (units.isEmpty() || units.values().stream().allMatch(count -> count == 0)) {
               System.err.println(TextFormatUtil.warning("Creating project with zero total units specified."));
           }


        Project newProject = new Project(name, neighborhood, units, open, close, managerNric, slots);
        DataStore.addProject(newProject);
        DataStore.saveAllData(); // Save after creation
        return newProject;
    }

     private boolean isManagerHandlingAnotherProjectInPeriod(String managerNric, Date newOpen, Date newClose) {
         for (Project existingProject : DataStore.getProjects().values()) {
             if (managerNric.equals(existingProject.getAssignedHDBManagerNric())) {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 if (existingOpen != null && existingClose != null) {
                     // Check for overlap: (StartA <= EndB) and (EndA >= StartB)
                     if (!newOpen.after(existingClose) && !newClose.before(existingOpen)) {
                         return true; // Overlap found
                     }
                 }
             }
         }
         return false; // No conflicting project found
     }


     @Override
     public boolean editProject(int projectId, Project updatedDetails, String editorNric) {
          // NOTE: Passing the whole 'updatedDetails' object can be problematic.
          // It's safer to pass specific fields to update.
          // Let's assume for now the controller constructs a valid 'updatedDetails'
          // based on user input for editable fields only.

          Project project = DataStore.getProjectById(projectId);
          if (project == null) {
              System.err.println(TextFormatUtil.error("Edit project failed: Project ID " + projectId + " not found."));
              return false;
          }
           User editor = DataStore.getUserByNric(editorNric);
           if (editor == null || editor.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(editorNric)) {
                System.err.println(TextFormatUtil.error("Edit project failed: Only the assigned HDB Manager (" + project.getAssignedHDBManagerNric() + ") can edit project " + projectId + "."));
                return false;
           }

            // Validation before applying changes
             Date newOpen = updatedDetails.getApplicationOpeningDate();
             Date newClose = updatedDetails.getApplicationClosingDate();
             int newSlots = updatedDetails.getMaxOfficerSlots();

             if (newOpen == null || newClose == null || newOpen.after(newClose)) {
                 System.err.println(TextFormatUtil.error("Edit project failed: Invalid application dates."));
                 return false;
             }
             if (newSlots <= 0 || newSlots > 10) {
                 System.err.println(TextFormatUtil.error("Edit project failed: Invalid number of officer slots (must be 1-10)."));
                 return false;
             }
             // Check manager period conflict *excluding the project being edited*
             if (isManagerHandlingAnotherProjectInPeriodExcludingSelf(editorNric, newOpen, newClose, projectId)) {
                  System.err.println(TextFormatUtil.error("Edit project failed: Manager ("+editorNric+") is already handling another project during the updated application period."));
                  return false;
             }
             // Editing unit counts needs care - prevent if applications exist?
             boolean appsExist = DataStore.getApplications().values().stream().anyMatch(a -> a.getProjectId() == projectId);
             if (appsExist && !project.getTotalUnits().equals(updatedDetails.getTotalUnits())) {
                 System.err.println(TextFormatUtil.error("Edit project failed: Cannot change total unit counts after applications have been received."));
                 // Allow editing only other fields if units are changed? Or reject entirely? Rejecting is safer.
                 return false;
                 // Or, only allow increasing units? Complex. Let's disallow changes to total units if apps exist.
             }


            // Apply changes for editable fields
            project.setProjectName(updatedDetails.getProjectName());
            project.setNeighborhood(updatedDetails.getNeighborhood());
            project.setApplicationOpeningDate(newOpen);
            project.setApplicationClosingDate(newClose);
            project.setMaxOfficerSlots(newSlots);
            // Assuming total units aren't changed if apps exist (based on check above)
            // project.setTotalUnits(updatedDetails.getTotalUnits()); // Apply if allowed
            // Visibility is handled by toggleProjectVisibility

            DataStore.saveAllData();
            return true;
     }

      // Helper to check manager conflict excluding the project being edited
      private boolean isManagerHandlingAnotherProjectInPeriodExcludingSelf(String managerNric, Date newOpen, Date newClose, int projectIdToExclude) {
         for (Project existingProject : DataStore.getProjects().values()) {
             if (existingProject.getProjectId() == projectIdToExclude) continue; // Skip self

             if (managerNric.equals(existingProject.getAssignedHDBManagerNric())) {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 if (existingOpen != null && existingClose != null) {
                     if (!newOpen.after(existingClose) && !newClose.before(existingOpen)) {
                         return true; // Overlap found
                     }
                 }
             }
         }
         return false;
     }

      @Override
     public boolean deleteProject(int projectId, String deleterNric) {
         Project project = DataStore.getProjectById(projectId);
         if (project == null) {
             System.err.println(TextFormatUtil.error("Delete project failed: Project not found."));
             return false;
         }

          User deleter = DataStore.getUserByNric(deleterNric);
           if (deleter == null || deleter.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(deleterNric)) {
                System.err.println(TextFormatUtil.error("Delete project failed: Only the assigned HDB Manager can delete this project."));
                return false;
           }

          // Check implications: Prevent deletion if BOOKED applications exist.
          boolean hasBookings = DataStore.getApplications().values().stream()
                                .anyMatch(a -> a.getProjectId() == projectId && a.getStatus() == BTOApplicationStatus.BOOKED);
          if (hasBookings) {
              System.err.println(TextFormatUtil.error("Delete project failed: Cannot delete project with active flat bookings."));
              return false;
          }

          // Optional: Warn if other active (Pending/Successful) applications exist
          boolean hasActiveApps = DataStore.getApplications().values().stream()
                                   .anyMatch(a -> a.getProjectId() == projectId &&
                                             (a.getStatus() == BTOApplicationStatus.PENDING || a.getStatus() == BTOApplicationStatus.SUCCESSFUL));
           if (hasActiveApps) {
                System.out.println(TextFormatUtil.warning("Warning: Deleting project with pending/successful applications. These will also be removed."));
           }


          // Proceed with deletion
          DataStore.removeProject(projectId);
          // Remove associated applications, enquiries, registrations, bookings
          DataStore.getApplications().values().removeIf(a -> a.getProjectId() == projectId);
          DataStore.getEnquiries().values().removeIf(e -> e.getProjectId() == projectId);
          DataStore.getOfficerRegistrations().values().removeIf(r -> r.getProjectId() == projectId);
          DataStore.getFlatBookings().values().removeIf(b -> b.getProjectId() == projectId); // Should be none due to check above, but good practice

          DataStore.saveAllData();
          System.out.println(TextFormatUtil.success("Project " + projectId + " and associated records deleted successfully."));
          return true;
     }

      @Override
     public boolean toggleProjectVisibility(int projectId, boolean isVisible, String managerNric) {
          Project project = DataStore.getProjectById(projectId);
          if (project == null) {
              System.err.println(TextFormatUtil.error("Toggle visibility failed: Project not found."));
              return false;
          }

           User manager = DataStore.getUserByNric(managerNric);
           if (manager == null || manager.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(managerNric)) {
                System.err.println(TextFormatUtil.error("Toggle visibility failed: Only the assigned HDB Manager can perform this action."));
                return false;
           }

          project.setVisibility(isVisible);
          DataStore.saveAllData();
          return true;
     }


    @Override
    public List<Project> getProjectsManagedBy(String managerNric) {
         User manager = DataStore.getUserByNric(managerNric);
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Error: User not found or not a manager."));
               return Collections.emptyList();
          }
         return DataStore.getProjects().values().stream()
                 .filter(p -> p.getAssignedHDBManagerNric().equals(managerNric))
                 .sorted(Comparator.comparing(Project::getProjectName))
                 .collect(Collectors.toList());
    }

    @Override
    public boolean addOfficerToProject(int projectId, String officerNric) {
        Project project = DataStore.getProjectById(projectId);
         User officer = DataStore.getUserByNric(officerNric);

          if (project == null || officer == null || officer.getRole() != UserRole.OFFICER) {
               System.err.println(TextFormatUtil.error("Add officer failed: Project or Officer not found/invalid."));
               return false;
          }

        boolean added = project.addOfficer(officerNric); // Model checks slots and uniqueness
        if (added) {
            // Update officer's handling project ID
             if (officer instanceof HDBOfficer) {
                 ((HDBOfficer) officer).setHandlingProjectId(projectId);
             }
            DataStore.saveAllData();
            return true;
        } else {
            System.err.println(TextFormatUtil.error("Add officer failed: No remaining slots or officer already assigned to project " + projectId + "."));
            return false;
        }
    }

    @Override
    public boolean removeOfficerFromProject(int projectId, String officerNric) {
         Project project = DataStore.getProjectById(projectId);
          User officer = DataStore.getUserByNric(officerNric);

           if (project == null || officer == null || officer.getRole() != UserRole.OFFICER) {
               System.err.println(TextFormatUtil.error("Remove officer failed: Project or Officer not found/invalid."));
               return false;
          }

         boolean removed = project.removeOfficer(officerNric);
         if (removed) {
             // Clear officer's handling project ID
              if (officer instanceof HDBOfficer) {
                   // Only clear if they were handling *this* project
                   if (Objects.equals(((HDBOfficer) officer).getHandlingProjectId(), projectId)) {
                        ((HDBOfficer) officer).clearHandlingProject();
                   }
              }
            DataStore.saveAllData();
            return true;
        } else {
             System.err.println(TextFormatUtil.warning("Remove officer warning: Officer " + officerNric + " was not assigned to project " + projectId + "."));
             return false; // Or true if not being assigned isn't an error? Let's say false.
        }
    }


    @Override
    public boolean decrementProjectUnit(int projectId, FlatType type) {
        Project project = DataStore.getProjectById(projectId);
        if (project == null) {
             System.err.println(TextFormatUtil.error("Decrement unit failed: Project not found."));
             return false;
        }
        boolean success = project.decrementAvailableUnits(type);
        if (success) {
            DataStore.saveAllData();
        } else {
             System.err.println(TextFormatUtil.error("Decrement unit failed: No available units for " + type.getDisplayName() + " in project " + projectId + "."));
        }
        return success;
    }

    @Override
    public boolean incrementProjectUnit(int projectId, FlatType type) {
         Project project = DataStore.getProjectById(projectId);
         if (project == null) {
              System.err.println(TextFormatUtil.error("Increment unit failed: Project not found."));
              return false;
         }
         project.incrementAvailableUnits(type); // Assumes model handles logic & warnings
         DataStore.saveAllData();
         return true;
    }

    @Override
    public boolean isProjectWithinApplicationPeriod(int projectId, Date date) {
        Project project = DataStore.getProjectById(projectId);
        return project != null && project.isWithinApplicationPeriod(date);
    }

     @Override
     public Project getHandlingProjectForOfficer(String officerNric) {
         User user = DataStore.getUserByNric(officerNric);
         if (user instanceof HDBOfficer) {
             HDBOfficer officer = (HDBOfficer) user;
             Integer handlingProjectId = officer.getHandlingProjectId();
             if (handlingProjectId != null) {
                 return DataStore.getProjectById(handlingProjectId);
             }
         }
         return null; // Not an officer or not handling any project
     }
}