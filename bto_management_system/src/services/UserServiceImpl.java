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
        }


        // Use the setPassword method in the User model, checks the old password
        boolean success = user.setPassword(oldPassword, newPassword);

        if (success) {
            DataStore.saveAllData(); 
        }

        return success;
    }

    @Override
    public User getUserDetails(String nric) {
        return DataStore.getUserByNric(nric);
    }
}