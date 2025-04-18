package utils;

public class FilePathConstants {
    private static final String DATA_DIR = "data/"; // for data files

    public static final String USERS_FILE = DATA_DIR + "users.csv";
    public static final String PROJECTS_FILE = DATA_DIR + "projects.csv";
    public static final String APPLICATIONS_FILE = DATA_DIR + "applications.csv";
    public static final String ENQUIRIES_FILE = DATA_DIR + "enquiries.csv";
    public static final String OFFICER_REGISTRATIONS_FILE = DATA_DIR + "officer_registrations.csv";
    public static final String FLAT_BOOKINGS_FILE = DATA_DIR + "flat_bookings.csv";

    // Prevent instantiation
    private FilePathConstants() {}

    // Getters 
    public static String getUserFilePath() { return USERS_FILE; }
    public static String getProjectsFilePath() { return PROJECTS_FILE; }
    public static String getApplicationsFilePath() { return APPLICATIONS_FILE; }
    public static String getEnquiriesFilePath() { return ENQUIRIES_FILE; }
    public static String getOfficerRegistrationsFilePath() { return OFFICER_REGISTRATIONS_FILE; }
    public static String getFlatBookingsFilePath() { return FLAT_BOOKINGS_FILE; }
}