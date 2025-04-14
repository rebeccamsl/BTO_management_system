package stores;

import models.User; // Updated import

public class AuthStore {
    private static User currentUser = null;

    // Prevent instantiation
    private AuthStore() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clearCurrentUser() {
        currentUser = null;
    }

    public static String getCurrentUserNric() {
        return (currentUser != null) ? currentUser.getNric() : null;
    }
}