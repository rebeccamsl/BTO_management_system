package stores;

import models.*; // Import all models
import data.*;    // Import all data managers
import utils.FilePathConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Use ConcurrentHashMap for thread safety if needed, HashMap is usually fine for CLI

public class DataStore {

    // Using Map<KeyType, ModelObject> for efficient lookup
    private static Map<String, User> userData = new ConcurrentHashMap<>(); // Key: NRIC (String)
    private static Map<Integer, Project> projectData = new ConcurrentHashMap<>(); // Key: Project ID (Integer)
    private static Map<Integer, BTOApplication> applicationData = new ConcurrentHashMap<>(); // Key: Application ID (Integer)
    private static Map<Integer, Enquiry> enquiryData = new ConcurrentHashMap<>(); // Key: Enquiry ID (Integer)
    private static Map<Integer, HDBOfficerRegistration> officerRegistrationData = new ConcurrentHashMap<>(); // Key: Registration ID (Integer)
    private static Map<Integer, FlatBooking> flatBookingData = new ConcurrentHashMap<>(); // Key: Booking ID (Integer)

    // Data Manager instances
    private static final UserDataManager userDataManager = new UserDataManager();
    private static final ProjectDataManager projectDataManager = new ProjectDataManager();
    private static final ApplicationDataManager applicationDataManager = new ApplicationDataManager();
    private static final EnquiryDataManager enquiryDataManager = new EnquiryDataManager();
    private static final HDBOfficerRegDataManager officerRegDataManager = new HDBOfficerRegDataManager();
    private static final FlatBookingDataManager flatBookingDataManager = new FlatBookingDataManager();


    // Prevent instantiation
    private DataStore() {}

    /**
     * Initializes the DataStore by loading data from files.
     * Should be called once at application startup.
     */
    public static void initialize() {
        System.out.println("Initializing DataStore...");
        try {
            userData = userDataManager.load(FilePathConstants.USERS_FILE);
            projectData = projectDataManager.load(FilePathConstants.PROJECTS_FILE);
            applicationData = applicationDataManager.load(FilePathConstants.APPLICATIONS_FILE);
            enquiryData = enquiryDataManager.load(FilePathConstants.ENQUIRIES_FILE);
            officerRegistrationData = officerRegDataManager.load(FilePathConstants.OFFICER_REGISTRATIONS_FILE);
            flatBookingData = flatBookingDataManager.load(FilePathConstants.FLAT_BOOKINGS_FILE);

             // Update ID counters after loading
             updateIdCounters();

             System.out.println("DataStore initialized successfully.");

        } catch (Exception e) {
            System.err.println("FATAL: Failed to initialize DataStore. Exiting.");
            e.printStackTrace();
            System.exit(1); // Exit if critical data cannot be loaded
        }
    }

     /**
     * Updates the static ID counters in model classes based on loaded data.
     */
    private static void updateIdCounters() {
        int maxProjectId = projectData.keySet().stream().max(Integer::compareTo).orElse(0);
        Project.updateIdCounter(maxProjectId);

        int maxAppId = applicationData.keySet().stream().max(Integer::compareTo).orElse(0);
        BTOApplication.updateIdCounter(maxAppId);

        int maxEnquiryId = enquiryData.keySet().stream().max(Integer::compareTo).orElse(0);
        Enquiry.updateIdCounter(maxEnquiryId);

        int maxRegId = officerRegistrationData.keySet().stream().max(Integer::compareTo).orElse(0);
        HDBOfficerRegistration.updateIdCounter(maxRegId);

        int maxBookingId = flatBookingData.keySet().stream().max(Integer::compareTo).orElse(0);
        FlatBooking.updateIdCounter(maxBookingId);
    }

    /**
     * Saves all data back to their respective files.
     * Should be called before application exit or periodically.
     */
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
            System.err.println("Error saving data:");
            e.printStackTrace();
        }
    }

    // --- Getters for Data Maps (Return read-only or copies if mutation outside services is a concern) ---
    // For simplicity in CLI app, returning direct reference is often acceptable if services manage writes.
    // Returning unmodifiable maps is safer.

    public static Map<String, User> getUsers() {
        // return Collections.unmodifiableMap(userData); // Safer option
         return userData; // Direct access (simpler for this context)
    }

    public static Map<Integer, Project> getProjects() {
        // return Collections.unmodifiableMap(projectData);
        return projectData;
    }

    public static Map<Integer, BTOApplication> getApplications() {
       // return Collections.unmodifiableMap(applicationData);
        return applicationData;
    }

    public static Map<Integer, Enquiry> getEnquiries() {
        // return Collections.unmodifiableMap(enquiryData);
         return enquiryData;
    }

     public static Map<Integer, HDBOfficerRegistration> getOfficerRegistrations() {
        // return Collections.unmodifiableMap(officerRegistrationData);
        return officerRegistrationData;
    }

     public static Map<Integer, FlatBooking> getFlatBookings() {
       // return Collections.unmodifiableMap(flatBookingData);
        return flatBookingData;
    }


    // --- Convenience Methods for Adding/Updating Data (Called by Services) ---

    public static void addUser(User user) {
        if (user != null) userData.put(user.getNric(), user);
        // Consider immediate save or rely on saveAllData
    }
     public static void removeUser(String nric) {
        userData.remove(nric);
    }


    public static void addProject(Project project) {
        if (project != null) projectData.put(project.getProjectId(), project);
    }
     public static void removeProject(int projectId) {
        projectData.remove(projectId);
    }

    public static void addApplication(BTOApplication application) {
        if (application != null) applicationData.put(application.getApplicationId(), application);
    }
     public static void removeApplication(int applicationId) {
        applicationData.remove(applicationId);
    }

    public static void addEnquiry(Enquiry enquiry) {
        if (enquiry != null) enquiryData.put(enquiry.getEnquiryId(), enquiry);
    }
     public static void removeEnquiry(int enquiryId) {
        enquiryData.remove(enquiryId);
    }

    public static void addOfficerRegistration(HDBOfficerRegistration registration) {
        if (registration != null) officerRegistrationData.put(registration.getRegistrationId(), registration);
    }
     public static void removeOfficerRegistration(int registrationId) {
        officerRegistrationData.remove(registrationId);
    }

    public static void addFlatBooking(FlatBooking booking) {
        if (booking != null) flatBookingData.put(booking.getBookingId(), booking);
    }
     public static void removeFlatBooking(int bookingId) {
        flatBookingData.remove(bookingId);
    }


    // --- Getters for Specific Items ---

    public static User getUserByNric(String nric) {
        return userData.get(nric);
    }

    public static Project getProjectById(int projectId) {
        return projectData.get(projectId);
    }

     public static BTOApplication getApplicationById(int applicationId) {
        return applicationData.get(applicationId);
    }
      public static Enquiry getEnquiryById(int enquiryId) {
        return enquiryData.get(enquiryId);
    }
     public static HDBOfficerRegistration getOfficerRegistrationById(int registrationId) {
        return officerRegistrationData.get(registrationId);
    }
     public static FlatBooking getFlatBookingById(int bookingId) {
        return flatBookingData.get(bookingId);
    }

}