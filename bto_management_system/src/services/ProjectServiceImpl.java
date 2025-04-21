package services;

import interfaces.IProjectService;
import models.*;
import enums.*;
import stores.DataStore;
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
     * Retrieves all projects, sorted by Project ID. Usually for Manager view.
     * @return List of all Project objects sorted by ID.
     */
    @Override
    public List<Project> getAllProjects() {
         return DataStore.getProjects().values().stream()
                 .sorted(Comparator.comparingInt(Project::getProjectId))
                 .collect(Collectors.toList());
    }

    /**
     * Retrieves projects visible and open for application for a specific user.
     * Filters based on project visibility and application period.
     * Sorts the result by Project ID.
     * @param userNric NRIC of the user viewing the projects (Applicant or Officer).
     * @return List of visible and open Project objects, sorted by ID.
     */
    @Override
    public List<Project> getVisibleProjectsForApplicant(String userNric) {
        User user = DataStore.getUserByNric(userNric);
         if (user == null || !(user.getRole() == UserRole.APPLICANT || user.getRole() == UserRole.OFFICER)) {
             System.err.println(TextFormatUtil.error("Error finding visible projects: User (" + userNric + ") not found or not an Applicant/Officer."));
             return Collections.emptyList();
        }

        List<Project> allProjects = new ArrayList<>(DataStore.getProjects().values());
        Date today = new Date();

        return allProjects.stream()
                .filter(Project::isVisible)
                .filter(p -> p.isWithinApplicationPeriod(today))
                .sorted(Comparator.comparingInt(Project::getProjectId))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all projects managed by a specific HDB Manager.
     * @param managerNric NRIC of the manager.
     * @return List of Project objects managed by this manager, sorted by Project ID.
     */
    @Override
    public List<Project> getProjectsManagedBy(String managerNric) {
         User manager = DataStore.getUserByNric(managerNric);
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Error finding managed projects: User (" + managerNric + ") not found or not a manager."));
               return Collections.emptyList();
          }
         return DataStore.getProjects().values().stream()
                 .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric()))
                 .sorted(Comparator.comparingInt(Project::getProjectId))
                 .collect(Collectors.toList());
    }



     @Override
    public Project getProjectById(int projectId) {
        Project project = DataStore.getProjectById(projectId);
        if (project == null) {
            System.err.println(TextFormatUtil.warning("Project with ID " + projectId + " not found in DataStore."));
        }
        return project;
    }

     @Override
    public Project createProject(String managerNric, String name, String neighborhood, Map<FlatType, Integer> units, Date open, Date close, int slots) {
          User manager = DataStore.getUserByNric(managerNric);
          if (manager == null || manager.getRole() != UserRole.MANAGER) {
               System.err.println(TextFormatUtil.error("Create project failed: User ("+managerNric+") not found or is not an HDB Manager."));
               return null;
          }
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
          if (isManagerHandlingAnotherProjectInPeriod(managerNric, open, close)) {
             System.err.println(TextFormatUtil.error("Create project failed: Manager ("+managerNric+") is already handling another project during this application period."));
             return null;
         }
          if (units == null || units.entrySet().stream().anyMatch(entry -> entry.getValue() < 0)) {
              System.err.println(TextFormatUtil.error("Create project failed: Unit counts cannot be negative."));
              return null;
          }
           if (units.values().stream().mapToInt(Integer::intValue).sum() == 0) {
               System.err.println(TextFormatUtil.warning("Creating project with zero total units specified. Is this intended?"));
           }

        Project newProject = new Project(name.trim(), neighborhood.trim(), units, open, close, managerNric, slots);
        DataStore.addProject(newProject);
        DataStore.saveAllData();
        return newProject;
    }

     private boolean isManagerHandlingAnotherProjectInPeriod(String managerNric, Date newOpen, Date newClose) {
         return DataStore.getProjects().values().stream()
             .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric()))
             .anyMatch(existingProject -> {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 return existingOpen != null && existingClose != null &&
                        !newClose.before(existingOpen) && !newOpen.after(existingClose);
             });
     }

     @Override
     public boolean editProject(int projectId, Project updatedDetailsContainer, String editorNric) {
          Project projectToEdit = DataStore.getProjectById(projectId);
          if (projectToEdit == null) {
              System.err.println(TextFormatUtil.error("Edit project failed: Project ID " + projectId + " not found."));
              return false;
          }
           User editor = DataStore.getUserByNric(editorNric);
           if (editor == null || editor.getRole() != UserRole.MANAGER || !projectToEdit.getAssignedHDBManagerNric().equals(editorNric)) {
                System.err.println(TextFormatUtil.error("Edit project failed: User ("+editorNric+") is not the assigned HDB Manager (" + projectToEdit.getAssignedHDBManagerNric() + ") for project " + projectId + "."));
                return false;
           }

             Date newOpen = updatedDetailsContainer.getApplicationOpeningDate();
             Date newClose = updatedDetailsContainer.getApplicationClosingDate();
             int newSlots = updatedDetailsContainer.getMaxOfficerSlots();
             Map<FlatType, Integer> newUnitsMap = updatedDetailsContainer.getTotalUnits();
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
             if (isManagerHandlingAnotherProjectInPeriodExcludingSelf(editorNric, newOpen, newClose, projectId)) {
                  System.err.println(TextFormatUtil.error("Edit project failed: Manager ("+editorNric+") is already handling another project during the updated application period."));
                  return false;
             }

             boolean appsExist = DataStore.getApplications().values().stream().anyMatch(a -> a.getProjectId() == projectId);
             Map<FlatType, Integer> unitsToActuallySet = new HashMap<>(projectToEdit.getTotalUnits());
             boolean unitsWereChanged = false;

             if (!appsExist) {
                  unitsToActuallySet = newUnitsMap;
                  unitsWereChanged = !projectToEdit.getTotalUnits().equals(unitsToActuallySet);
                  // if(unitsWereChanged) System.out.println("Debug: Total units will be updated as no applications exist.");
             } else if (!projectToEdit.getTotalUnits().equals(newUnitsMap)) {
                 System.err.println(TextFormatUtil.warning("Edit project warning: Cannot change total unit counts after applications have been received. Unit counts remain unchanged. Other details will be updated if changed."));
             }

            projectToEdit.setProjectName(newName);
            projectToEdit.setNeighborhood(newNeighborhood);
            projectToEdit.setApplicationOpeningDate(newOpen);
            projectToEdit.setApplicationClosingDate(newClose);
            projectToEdit.setMaxOfficerSlots(newSlots);
            projectToEdit.setTotalUnits(unitsToActuallySet);
            if (unitsWereChanged) {
                 projectToEdit.setAvailableUnits(new HashMap<>(unitsToActuallySet));
                 System.out.println(TextFormatUtil.info("Available units have been reset to match the new total unit counts."));
            }

            DataStore.saveAllData();
            return true;
     }

      private boolean isManagerHandlingAnotherProjectInPeriodExcludingSelf(String managerNric, Date newOpen, Date newClose, int projectIdToExclude) {
          return DataStore.getProjects().values().stream()
             .filter(p -> p.getProjectId() != projectIdToExclude)
             .filter(p -> managerNric.equals(p.getAssignedHDBManagerNric()))
             .anyMatch(existingProject -> {
                 Date existingOpen = existingProject.getApplicationOpeningDate();
                 Date existingClose = existingProject.getApplicationClosingDate();
                 return existingOpen != null && existingClose != null &&
                        !newClose.before(existingOpen) && !newOpen.after(existingClose);
             });
     }

      @Override
     public boolean deleteProject(int projectId, String deleterNric) {
         Project project = DataStore.getProjectById(projectId);
         if (project == null) {
             System.err.println(TextFormatUtil.error("Delete project failed: Project ID " + projectId + " not found."));
             return false;
         }

          User deleter = DataStore.getUserByNric(deleterNric);
           if (deleter == null || deleter.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(deleterNric)) {
                System.err.println(TextFormatUtil.error("Delete project failed: User ("+deleterNric+") is not the assigned HDB Manager for project " + projectId + "."));
                return false;
           }

          boolean hasBookings = DataStore.getApplications().values().stream()
                                .anyMatch(a -> a.getProjectId() == projectId && a.getStatus() == BTOApplicationStatus.BOOKED);
          if (hasBookings) {
              System.err.println(TextFormatUtil.error("Delete project failed: Cannot delete project " + projectId + " because it has active flat bookings. Applicants must withdraw or bookings resolved first."));
              return false;
          }

          boolean hasActiveApps = DataStore.getApplications().values().stream()
                                   .anyMatch(a -> a.getProjectId() == projectId &&
                                             (a.getStatus() == BTOApplicationStatus.PENDING || a.getStatus() == BTOApplicationStatus.SUCCESSFUL));
           if (hasActiveApps) {
                System.out.println(TextFormatUtil.warning("Warning: Deleting project " + projectId + " with pending/successful applications. These will also be removed."));
           }

          DataStore.removeProject(projectId);
          DataStore.getApplications().values().removeIf(a -> a.getProjectId() == projectId);
          DataStore.getEnquiries().values().removeIf(e -> e.getProjectId() == projectId);
          DataStore.getOfficerRegistrations().values().removeIf(r -> r.getProjectId() == projectId);
          DataStore.getFlatBookings().values().removeIf(b -> b.getProjectId() == projectId);

          DataStore.saveAllData();
          return true;
     }

      @Override
     public boolean toggleProjectVisibility(int projectId, boolean isVisible, String managerNric) {
          Project project = DataStore.getProjectById(projectId);
          if (project == null) {
              System.err.println(TextFormatUtil.error("Toggle visibility failed: Project ID " + projectId + " not found."));
              return false;
          }

           User manager = DataStore.getUserByNric(managerNric);
           if (manager == null || manager.getRole() != UserRole.MANAGER || !project.getAssignedHDBManagerNric().equals(managerNric)) {
                System.err.println(TextFormatUtil.error("Toggle visibility failed: User ("+managerNric+") is not the assigned HDB Manager for project " + projectId + "."));
                return false;
           }

          project.setVisibility(isVisible);
          DataStore.saveAllData();
          return true;
     }

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
              if (officer instanceof HDBOfficer) {
                   if (Objects.equals(((HDBOfficer) officer).getHandlingProjectId(), projectId)) {
                        ((HDBOfficer) officer).clearHandlingProject();
                   }
              }
            DataStore.saveAllData();
            return true;
        }
         System.err.println(TextFormatUtil.warning("Remove officer warning: Officer " + officerNric + " was not found in the assigned list for project " + projectId + "."));
        return false;
    }

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

    @Override
    public boolean isProjectWithinApplicationPeriod(int projectId, Date date) {
        Project project = DataStore.getProjectById(projectId);
        if (date == null) return false;
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
         return null;
     }
}