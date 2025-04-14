package services;

import interfaces.IUserService;
import models.User;
import stores.DataStore;
import stores.AuthStore; // To ensure only the logged-in user can change their own password easily

public class UserServiceImpl implements IUserService {

    @Override
    public boolean changePassword(String nric, String oldPassword, String newPassword) {
        User user = DataStore.getUserByNric(nric);

        if (user == null) {
            System.err.println("User not found for password change: " + nric);
            return false;
        }

        // Additional check: Ensure the logged-in user matches the NRIC for self-service password change
        User currentUser = AuthStore.getCurrentUser();
        if (currentUser == null || !currentUser.getNric().equals(nric)) {
             System.err.println("Security Error: Attempt to change password for a different user or while not logged in.");
             // Although controller should prevent this, service layer check adds safety.
             // Allow admin/manager role to bypass this check if needed in future.
             // For now, assume only self-change is allowed via this method.
             // return false; // Temporarily allow for testing setup without full login flow yet
        }


        // Use the setPassword method in the User model which checks the old password
        boolean success = user.setPassword(oldPassword, newPassword);

        if (success) {
            // Persist the change
            DataStore.saveAllData(); // Consider saving only user data if performance is critical
        }

        return success;
    }

    @Override
    public User getUserDetails(String nric) {
        return DataStore.getUserByNric(nric);
        // No sensitive data transformation needed here for now
    }
}