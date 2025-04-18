package stores;

import models.*;
import enums.RequestStatus;
import data.*;
import utils.FilePathConstants;
import utils.TextFormatUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects; 


public class DataStore {

    private static Map<String, User> userData = new ConcurrentHashMap<>();
    private static Map<Integer, Project> projectData = new ConcurrentHashMap<>();
    private static Map<Integer, BTOApplication> applicationData = new ConcurrentHashMap<>();
    private static Map<Integer, Enquiry> enquiryData = new ConcurrentHashMap<>();
    private static Map<Integer, HDBOfficerRegistration> officerRegistrationData = new ConcurrentHashMap<>();
    private static Map<Integer, FlatBooking> flatBookingData = new ConcurrentHashMap<>();

    private static final UserDataManager userDataManager = new UserDataManager();
    private static final ProjectDataManager projectDataManager = new ProjectDataManager();
    private static final ApplicationDataManager applicationDataManager = new ApplicationDataManager();
    private static final EnquiryDataManager enquiryDataManager = new EnquiryDataManager();
    private static final HDBOfficerRegDataManager officerRegDataManager = new HDBOfficerRegDataManager();
    private static final FlatBookingDataManager flatBookingDataManager = new FlatBookingDataManager();

    private DataStore() {}

    public static void initialize() {
        System.out.println("Initializing DataStore..."); 
        try {
            userData = userDataManager.load(FilePathConstants.USERS_FILE);
            projectData = projectDataManager.load(FilePathConstants.PROJECTS_FILE);
            applicationData = applicationDataManager.load(FilePathConstants.APPLICATIONS_FILE);
            enquiryData = enquiryDataManager.load(FilePathConstants.ENQUIRIES_FILE);
            officerRegistrationData = officerRegDataManager.load(FilePathConstants.OFFICER_REGISTRATIONS_FILE);
            flatBookingData = flatBookingDataManager.load(FilePathConstants.FLAT_BOOKINGS_FILE);

            setInitialOfficerHandlingState(); 
            updateIdCounters(); 

            System.out.println("DataStore initialized successfully.");

        } catch (Exception e) {
            System.err.println(TextFormatUtil.error("FATAL: Failed to initialize DataStore due to: " + e.getMessage()));
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void setInitialOfficerHandlingState() {
        Map<String, HDBOfficerRegistration> latestApprovedRegMap = new HashMap<>();

        officerRegistrationData.values().stream()
            .filter(reg -> reg.getStatus() == RequestStatus.APPROVED)
            .sorted(Comparator.comparing(HDBOfficerRegistration::getDecisionDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .forEach(reg -> latestApprovedRegMap.putIfAbsent(reg.getOfficerNric(), reg));

        latestApprovedRegMap.forEach((officerNric, latestReg) -> {
            User user = userData.get(officerNric);
            if (user instanceof HDBOfficer) {
                Project project = projectData.get(latestReg.getProjectId());
                if (project != null) {
                    ((HDBOfficer) user).setHandlingProjectId(latestReg.getProjectId());
                } else {
                     System.err.println(TextFormatUtil.warning("Warning during init: Officer " + officerNric + " approved for non-existent project " + latestReg.getProjectId() + ". Handling state not set."));
                }
            }
        });
    }

    private static void updateIdCounters() {
         Project.updateIdCounter(projectData.keySet().stream().max(Integer::compareTo).orElse(0));
         BTOApplication.updateIdCounter(applicationData.keySet().stream().max(Integer::compareTo).orElse(0));
         Enquiry.updateIdCounter(enquiryData.keySet().stream().max(Integer::compareTo).orElse(0));
         HDBOfficerRegistration.updateIdCounter(officerRegistrationData.keySet().stream().max(Integer::compareTo).orElse(0));
         FlatBooking.updateIdCounter(flatBookingData.keySet().stream().max(Integer::compareTo).orElse(0));

    }

    public static void saveAllData() {
        System.out.println("Saving data...");
        try {
            userDataManager.save(FilePathConstants.USERS_FILE, userData);
            projectDataManager.save(FilePathConstants.PROJECTS_FILE, projectData);
            applicationDataManager.save(FilePathConstants.APPLICATIONS_FILE, applicationData);
            enquiryDataManager.save(FilePathConstants.ENQUIRIES_FILE, enquiryData);
            officerRegDataManager.save(FilePathConstants.OFFICER_REGISTRATIONS_FILE, officerRegistrationData);
            flatBookingDataManager.save(FilePathConstants.FLAT_BOOKINGS_FILE, flatBookingData);
            System.out.println("Data saved successfully.");
        } catch (Exception e) {
            System.err.println(TextFormatUtil.error("Error saving data: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    // Getters and other methods 
    public static Map<String, User> getUsers() { return userData; }
    public static Map<Integer, Project> getProjects() { return projectData; }
    public static Map<Integer, BTOApplication> getApplications() { return applicationData; }
    public static Map<Integer, Enquiry> getEnquiries() { return enquiryData; }
    public static Map<Integer, HDBOfficerRegistration> getOfficerRegistrations() { return officerRegistrationData; }
    public static Map<Integer, FlatBooking> getFlatBookings() { return flatBookingData; }
    public static void addUser(User user) { if (user != null) userData.put(user.getNric(), user); }
    public static void removeUser(String nric) { userData.remove(nric); }
    public static void addProject(Project project) { if (project != null) projectData.put(project.getProjectId(), project); }
    public static void removeProject(int projectId) { projectData.remove(projectId); }
    public static void addApplication(BTOApplication application) { if (application != null) applicationData.put(application.getApplicationId(), application); }
    public static void removeApplication(int applicationId) { applicationData.remove(applicationId); }
    public static void addEnquiry(Enquiry enquiry) { if (enquiry != null) enquiryData.put(enquiry.getEnquiryId(), enquiry); }
    public static void removeEnquiry(int enquiryId) { enquiryData.remove(enquiryId); }
    public static void addOfficerRegistration(HDBOfficerRegistration registration) { if (registration != null) officerRegistrationData.put(registration.getRegistrationId(), registration); }
    public static void removeOfficerRegistration(int registrationId) { officerRegistrationData.remove(registrationId); }
    public static void addFlatBooking(FlatBooking booking) { if (booking != null) flatBookingData.put(booking.getBookingId(), booking); }
    public static void removeFlatBooking(int bookingId) { flatBookingData.remove(bookingId); }
    public static User getUserByNric(String nric) { return userData.get(nric); }
    public static Project getProjectById(int projectId) { return projectData.get(projectId); }
    public static BTOApplication getApplicationById(int applicationId) { return applicationData.get(applicationId); }
    public static Enquiry getEnquiryById(int enquiryId) { return enquiryData.get(enquiryId); }
    public static HDBOfficerRegistration getOfficerRegistrationById(int registrationId) { return officerRegistrationData.get(registrationId); }
    public static FlatBooking getFlatBookingById(int bookingId) { return flatBookingData.get(bookingId); }
}