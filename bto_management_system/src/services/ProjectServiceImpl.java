package services;

import interfaces.IProjectService;
import models.*;
import enums.*;
import stores.DataStore;
import stores.AuthStore;
import utils.DateUtils;
import utils.TextFormatUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the IProjectService interface.
 * Handles business logic related to BTO projects.
 */
public class ProjectServiceImpl implements IProjectService {


    /**
     * Retrieves all projects, sorted by name. Usually for Manager view.
     * @return List of all Project objects.
     */
    @Override
    public List<Project> getAllProjects() {
         return DataStore.getProjects().values().stream()
                 .sorted(Comparator.comparing(Project::getProjectName))
                 .collect(Collectors.toList());
    }

    /**
     * Retrieves projects visible and open for application for a specific applicant/officer role.
     * Filters based on project visibility and application period ONLY.
     * Applicant eligibility for specific flat types is checked during application submission.
     * @param applicantNric NRIC of the user viewing the projects (Applicant or Officer).
     * @return List of visible and open Project objects.
     */
    @Override
    public List<Project> getVisibleProjectsForApplicant(String applicantNric) {
        User applicant = DataStore.getUserByNric(applicantNric);
         if (applicant == null || !(applicant.getRole() == UserRole.APPLICANT || applicant.getRole() == UserRole.OFFICER)) {
             System.err.println(TextFormatUtil.error("Error finding visible projects: User (" + applicantNric + ") not found or not an Applicant/Officer."));
             return Collections.emptyList();
        }

        List<Project> allProjects = new ArrayList<>(DataStore.getProjects().values());
        Date today = new Date();

        // Filters only on project attributes for viewing list
        return allProjects.stream()
                .filter(Project::isVisible) // Filter 1: Project visibility toggle must be ON
                .filter(p -> p.isWithinApplicationPeriod(today)) // Filter 2: Current date must be within application period
                .sorted(Comparator.comparing(Project::getProjectName)) 
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific project by its ID.
     * @param projectId ID of the project.
     * @return Project object or null if not found.
     */
    @Override
    public Project getProjectById(int projectId) {
        Project project = DataStore.getProjectById(projectId);
        if (project == null) {
            System.err.println(TextFormatUtil.warning("Project with ID " + projectId + " not found in DataStore."));
        }
        return project;
    }

     /**
      * Creates a new BTO project after performing validation checks.
      * @param managerNric NRIC of the HDB Manager creating the project.
      * @param name Project name.
      * @param neighborhood Project location.
      * @param units Map of FlatType to total unit count.
      * @param open Application opening date.
      * @param close Application closing date.
      * @param slots Maximum number of HDB Officer slots (1-10).
      * @return The created Project object, or null on failure.
      */
     @Override
    public Project createProject(String managerNric, String name, String neighborhood, Map<FlatType, Integer> units, Date open, Date close, int slots) {
          User manager = DataStore.getUserByNric(managerNric);
          // 1. Permission Check
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Create project failed: User ("+managerNric+") not found or is not an HDB Manager."));
               return null;
          }
          // 2. Input Validation
          if (name == null || name.trim().isEmpty() || neighborhood == null || neighborhood.trim().isEmpty()) {
               System.err.println(TextFormatUtil.error("Create project failed: Project Name and Neighborhood cannot be empty."));
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
          // 3. Validation: Manager Period Conflict
          if (isManagerHandlingAnotherProjectInPeriod(managerNric, open, close)) {
             System.err.println(TextFormatUtil.error("Create project failed: Manager ("+managerNric+") is already handling another project during this application period."));
             return null;
         }
          // 4. Unit Validation
          if (units == null || units.entrySet().stream().anyMatch(entry -> entry.getValue() < 0)) {
              System.err.println(TextFormatUtil.error("Create project failed: Unit counts cannot be negative."));
              return null;
          }
           if (units.values().stream().mapToInt(Integer::intValue).sum() == 0) { // Check if total units is zero
               System.err.println(TextFormatUtil.warning("Creating project with zero total units specified. Is this intended?"));
           }

        // 5. Create and Store
        Project newProject = new Project(name.trim(), neighborhood.trim(), units, open, close, managerNric, slots);
        DataStore.addProject(newProject);
        DataStore.saveAllData(); // Persist
        System.out.println("Debug: Project created with ID: " + newProject.getProjectId());
        return newProject;
    }

     /** Helper to check manager period conflict */
     private boolean isManagerHandlingAnotherProjectInPeriod(String managerNric, Date newOpen, Date newClose) {
         return DataStore.getProjects().values().stream()
             .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric()))
             .anyMatch(existingProject -> {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 // Check for period overlap
                 return existingOpen != null && existingClose != null &&
                        !newClose.before(existingOpen) && !newOpen.after(existingClose);
             });
     }

     /**
      * Edits details of an existing project after validation.
      * @param projectId ID of the project to edit.
      * @param updatedDetailsContainer A temporary Project object holding the desired *new* values from the view.
      * @param editorNric NRIC of the manager attempting the edit.
      * @return true if edit successful, false otherwise.
      */
     @Override
     public boolean editProject(int projectId, Project updatedDetailsContainer, String editorNric) {
          Project projectToEdit = DataStore.getProjectById(projectId); // The actual project object
          if (projectToEdit == null) {
              System.err.println(TextFormatUtil.error("Edit project failed: Project ID " + projectId + " not found."));
              return false;
          }
           // 1. Permission Check
           User editor = DataStore.getUserByNric(editorNric);
           if (editor == null || editor.getRole() != UserRole.MANAGER || !projectToEdit.getAssignedHDBManagerNric().equals(editorNric)) {
                System.err.println(TextFormatUtil.error("Edit project failed: User ("+editorNric+") is not the assigned HDB Manager (" + projectToEdit.getAssignedHDBManagerNric() + ") for project " + projectId + "."));
                return false;
           }

            // 2. Extract and Validate new Data from the container object
             Date newOpen = updatedDetailsContainer.getApplicationOpeningDate();
             Date newClose = updatedDetailsContainer.getApplicationClosingDate();
             int newSlots = updatedDetailsContainer.getMaxOfficerSlots();
             Map<FlatType, Integer> newUnitsMap = updatedDetailsContainer.getTotalUnits(); // This holds the *intended* new totals
             String newName = updatedDetailsContainer.getProjectName();
             String newNeighborhood = updatedDetailsContainer.getNeighborhood();

             if (newName == null || newName.trim().isEmpty() ||
                 newNeighborhood == null || newNeighborhood.trim().isEmpty()) {
                  System.err.println(TextFormatUtil.error("Edit project failed: Project Name and Neighborhood cannot be empty."));
                  return false;
             }
             if (newOpen == null || newClose == null || newOpen.after(newClose)) {
                 System.err.println(TextFormatUtil.error("Edit project failed: Invalid application dates."));
                 return false;
             }
             if (newSlots <= 0 || newSlots > 10) {
                 System.err.println(TextFormatUtil.error("Edit project failed: Invalid number of officer slots (must be 1-10)."));
                 return false;
             }
              if (newUnitsMap == null || newUnitsMap.entrySet().stream().anyMatch(entry -> entry.getValue() < 0)) {
                  System.err.println(TextFormatUtil.error("Edit project failed: Unit counts cannot be negative."));
                  return false;
              }
              // 3. Validation: Manager Period Conflict (excluding self)
             if (isManagerHandlingAnotherProjectInPeriodExcludingSelf(editorNric, newOpen, newClose, projectId)) {
                  System.err.println(TextFormatUtil.error("Edit project failed: Manager ("+editorNric+") is already handling another project during the updated application period."));
                  return false;
             }

             // 4. Handle Unit Count Editing Restriction
             boolean appsExist = DataStore.getApplications().values().stream().anyMatch(a -> a.getProjectId() == projectId);
             Map<FlatType, Integer> unitsToActuallySet = new HashMap<>(projectToEdit.getTotalUnits()); // Start with current values
             boolean unitsWereChanged = false;

             if (!appsExist) {
                 // No applications yet, allow changing total units
                 unitsToActuallySet = newUnitsMap; // Use the new map from input
                 unitsWereChanged = !projectToEdit.getTotalUnits().equals(unitsToActuallySet);
                 if(unitsWereChanged) System.out.println("Debug: Total units will be updated as no applications exist.");
             } else if (!projectToEdit.getTotalUnits().equals(newUnitsMap)) {
                 // Applications exist AND user tried to change units
                 System.err.println(TextFormatUtil.warning("Edit project warning: Cannot change total unit counts after applications have been received. Unit counts remain unchanged. Other details will be updated if changed."));
                 // Keep unitsToActuallySet as the original project's units
             }


            // 5. Apply Allowed Changes to the actual project object in DataStore
            projectToEdit.setProjectName(newName);
            projectToEdit.setNeighborhood(newNeighborhood);
            projectToEdit.setApplicationOpeningDate(newOpen);
            projectToEdit.setApplicationClosingDate(newClose);
            projectToEdit.setMaxOfficerSlots(newSlots);
            // Only update units if allowed (no apps exist or units weren't changed by user)
            projectToEdit.setTotalUnits(unitsToActuallySet);

            // Recalculate available units ONLY if total units were successfully changed 
            if (unitsWereChanged) { // Check the flag set when units were allowed to change
                 // Reset available counts to match the new total counts
                 projectToEdit.setAvailableUnits(new HashMap<>(unitsToActuallySet));
                 System.out.println(TextFormatUtil.info("Available units have been reset to match the new total unit counts."));
            }

            DataStore.saveAllData(); 
            System.out.println("Debug: Project " + projectId + " edited successfully.");
            return true;
     }

      /** Helper to check manager period conflict excluding the project being edited */
      private boolean isManagerHandlingAnotherProjectInPeriodExcludingSelf(String managerNric, Date newOpen, Date newClose, int projectIdToExclude) {
          return DataStore.getProjects().values().stream()
             .filter(p -> p.getProjectId() != projectIdToExclude) // Exclude self
             .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric()))
             .anyMatch(existingProject -> {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 // Check for period overlap
                 return existingOpen != null && existingClose != null &&
                        !newClose.before(existingOpen) && !newOpen.after(existingClose);
             });
     }

      /**
       * Deletes a project after checking permissions and booking status.
       * @param projectId ID of the project to delete.
       * @param deleterNric NRIC of the manager attempting deletion.
       * @return true if deletion successful, false otherwise.
       */
      @Override
     public boolean deleteProject(int projectId, String deleterNric) {
         Project project = DataStore.getProjectById(projectId);
         if (project == null) {
             System.err.println(TextFormatUtil.error("Delete project failed: Project ID " + projectId + " not found."));
             return false;
         }

          // Permission Check
          User deleter = DataStore.getUserByNric(deleterNric);
           if (deleter == null || deleter.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(deleterNric)) {
                System.err.println(TextFormatUtil.error("Delete project failed: User ("+deleterNric+") is not the assigned HDB Manager for project " + projectId + "."));
                return false;
           }

           // Check: Cannot delete if bookings exist
          boolean hasBookings = DataStore.getApplications().values().stream()
                                .anyMatch(a -> a.getProjectId() == projectId && a.getStatus() == BTOApplicationStatus.BOOKED);
          if (hasBookings) {
              System.err.println(TextFormatUtil.error("Delete project failed: Cannot delete project " + projectId + " because it has active flat bookings. Applicants must withdraw or bookings resolved first."));
              return false;
          }


          System.out.println(TextFormatUtil.warning("Deleting project " + projectId + " will also remove associated applications, enquiries, and officer registrations."));


          DataStore.removeProject(projectId);
          DataStore.getApplications().values().removeIf(a -> a.getProjectId() == projectId);
          DataStore.getEnquiries().values().removeIf(e -> e.getProjectId() == projectId);
          DataStore.getOfficerRegistrations().values().removeIf(r -> r.getProjectId() == projectId);
          DataStore.getFlatBookings().values().removeIf(b -> b.getProjectId() == projectId);

          DataStore.saveAllData(); 
          System.out.println("Debug: Project " + projectId + " and associated records deleted by " + deleterNric);
          return true;
     }

      /**
       * Toggles the visibility of a project after checking permissions.
       * @param projectId ID of the project.
       * @param isVisible The desired new visibility state.
       * @param managerNric NRIC of the manager performing the action.
       * @return true if toggle successful, false otherwise.
       */
      @Override
     public boolean toggleProjectVisibility(int projectId, boolean isVisible, String managerNric) {
          Project project = DataStore.getProjectById(projectId);
          if (project == null) {
              System.err.println(TextFormatUtil.error("Toggle visibility failed: Project ID " + projectId + " not found."));
              return false;
          }

           // Permission Check
           User manager = DataStore.getUserByNric(managerNric);
           if (manager == null || manager.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(managerNric)) {
                System.err.println(TextFormatUtil.error("Toggle visibility failed: User ("+managerNric+") is not the assigned HDB Manager for project " + projectId + "."));
                return false;
           }

          project.setVisibility(isVisible); // Set the new state
          DataStore.saveAllData(); // Persist
          System.out.println("Debug: Visibility for project " + projectId + " set to " + isVisible);
          return true;
     }

    /**
     * Retrieves all projects managed by a specific HDB Manager.
     * @param managerNric NRIC of the manager.
     * @return List of Project objects managed by this manager, sorted by name.
     */
    @Override
    public List<Project> getProjectsManagedBy(String managerNric) {
         User manager = DataStore.getUserByNric(managerNric);
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Error finding managed projects: User (" + managerNric + ") not found or not a manager."));
               return Collections.emptyList();
          }
         return DataStore.getProjects().values().stream()
                 .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric())) // Use equals for string comparison
                 .sorted(Comparator.comparing(Project::getProjectName))
                 .collect(Collectors.toList());
    }

    /**
     * Adds an officer to a project's assigned list (Internal method called by ManagerService).
     * @param projectId ID of the project.
     * @param officerNric NRIC of the officer to add.
     * @return true if officer added successfully, false otherwise.
     */
    @Override
    public boolean addOfficerToProject(int projectId, String officerNric) {
        Project project = DataStore.getProjectById(projectId);
         User officer = DataStore.getUserByNric(officerNric);

          if (project == null || officer == null || officer.getRole() != UserRole.OFFICER) {
               System.err.println(TextFormatUtil.error("Internal error (addOfficerToProject): Project ("+projectId+") or Officer ("+officerNric+") not found/invalid."));
               return false;
          }

        boolean added = project.addOfficer(officerNric);
        if (added) {
             // Update the officer's state to show they are handling this project
             if (officer instanceof HDBOfficer) {
                 ((HDBOfficer) officer).setHandlingProjectId(projectId);
             }
            DataStore.saveAllData();
            return true;
        } else {
            System.err.println(TextFormatUtil.error("Internal error (addOfficerToProject): Failed to add officer " + officerNric + " to project " + projectId + " (check slots/duplicates)."));
            return false;
        }
    }

    /**
     * Removes an officer from a project's assigned list.
     * @param projectId ID of the project.
     * @param officerNric NRIC of the officer to remove.
     * @return true if removal successful.
     */
    @Override
    public boolean removeOfficerFromProject(int projectId, String officerNric) {
         Project project = DataStore.getProjectById(projectId);
          User officer = DataStore.getUserByNric(officerNric);

           if (project == null || officer == null || officer.getRole() != UserRole.OFFICER) {
               System.err.println(TextFormatUtil.error("Remove officer failed: Project ("+projectId+") or Officer ("+officerNric+") not found/invalid."));
               return false;
          }

         boolean removed = project.removeOfficer(officerNric);
         if (removed) {
              // Clear the officer's state only if they handling THIS project
              if (officer instanceof HDBOfficer) {
                   if (Objects.equals(((HDBOfficer) officer).getHandlingProjectId(), projectId)) {
                        ((HDBOfficer) officer).clearHandlingProject();
                   }
              }
            DataStore.saveAllData();
            return true;
        }
        // Officer wasn't on the list
         System.err.println(TextFormatUtil.warning("Remove officer warning: Officer " + officerNric + " was not found in the assigned list for project " + projectId + "."));
        return false;
    }

    /**
     * Decrements available units for a project/flat type (Internal method called by BookingService).
     * @param projectId ID of the project.
     * @param type FlatType being booked.
     * @return true if unit count decremented successfully, false if no units were available.
     */
    @Override
    public boolean decrementProjectUnit(int projectId, FlatType type) {
        Project project = DataStore.getProjectById(projectId);
        if (project == null) {
             System.err.println(TextFormatUtil.error("Internal error (decrementProjectUnit): Project ID " + projectId + " not found."));
             return false;
        }
         if (type == null) {
             System.err.println(TextFormatUtil.error("Internal error (decrementProjectUnit): Flat type cannot be null for project " + projectId + "."));
             return false;
         }
        boolean success = project.decrementAvailableUnits(type);
        if (success) {
            DataStore.saveAllData(); 
        } else {
            System.err.println(TextFormatUtil.error("Internal error (decrementProjectUnit): No available units for " + type.getDisplayName() + " in project " + projectId + ". Booking cannot proceed."));
        }
        return success;
    }

    /**
     * Increments available units (Internal method called by ManagerService on withdrawal approval).
     * @param projectId ID of the project.
     * @param type FlatType being returned.
     * @return true if unit count incremented successfully.
     */
    @Override
    public boolean incrementProjectUnit(int projectId, FlatType type) {
         Project project = DataStore.getProjectById(projectId);
         if (project == null) {
              System.err.println(TextFormatUtil.error("Internal error (incrementProjectUnit): Project ID " + projectId + " not found."));
              return false;
         }
         if (type == null) {
              System.err.println(TextFormatUtil.error("Internal error (incrementProjectUnit): Flat type cannot be null for project " + projectId + "."));
              return false;
         }
         project.incrementAvailableUnits(type); 
         DataStore.saveAllData();
         return true;
    }

    /**
     * Checks if a given date is within a project's application period.
     * @param projectId ID of the project.
     * @param date The date to check.
     * @return true if within period (inclusive), false otherwise or if project/dates invalid.
     */
    @Override
    public boolean isProjectWithinApplicationPeriod(int projectId, Date date) {
        Project project = DataStore.getProjectById(projectId);
        if (date == null) return false; // Cannot check against null date
        return project != null && project.isWithinApplicationPeriod(date);
    }

     /**
      * Retrieves the project an HDB Officer is currently assigned to handle.
      * @param officerNric NRIC of the HDB Officer.
      * @return Project object or null if not an officer or not assigned.
      */
     @Override
     public Project getHandlingProjectForOfficer(String officerNric) {
         User user = DataStore.getUserByNric(officerNric);
         if (user instanceof HDBOfficer) {
             HDBOfficer officer = (HDBOfficer) user;
             // Read the handling project ID stored in the officer object (set during approval)
             Integer handlingProjectId = officer.getHandlingProjectId();
             if (handlingProjectId != null) {
                 return DataStore.getProjectById(handlingProjectId);
             }
         }
         return null; // Not an officer or not currently handling any project
     }
}