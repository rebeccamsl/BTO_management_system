package services;

import interfaces.IAuthService;
import models.User;
import stores.AuthStore;
import stores.DataStore;
import utils.NRICValidator;
import utils.TextFormatUtil; // Import TextFormatUtil

public class AuthServiceImpl implements IAuthService {

    @Override
    public User login(String nric, String password) {
        // 1. Validate NRIC Format
        if (!NRICValidator.isValidFormat(nric)) {
            // Error message handled by controller/view based on null return
            return null;
        }

        // 2. Retrieve User
        User user = DataStore.getUserByNric(nric);

        // 3. Check User Existence
        if (user == null) {
            // Error message handled by controller/view
            return null;
        }

        // 4. Verify Password (Plain text comparison as per brief's assumption)
        // IMPORTANT: In a real system, use secure password hashing and comparison.
        if (user.getPassword().equals(password)) {
            // 5. Set current user in AuthStore
            AuthStore.setCurrentUser(user);
            return user; // Login successful
        } else {
            // Incorrect password, error message handled by controller/view
            return null;
        }
    }

    @Override
    public void logout() {
        AuthStore.clearCurrentUser();
        // Perform any other necessary logout cleanup if required
        System.out.println(TextFormatUtil.info("Logout successful.")); // Optional direct feedback
    }
}