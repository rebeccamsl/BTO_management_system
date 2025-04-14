package interfaces;

import models.User;

public interface IAuthService {
    /**
     * Attempts to log in a user with the given NRIC and password.
     * Sets the current user in AuthStore upon success.
     * @param nric User's NRIC.
     * @param password User's password.
     * @return The logged-in User object, or null if login fails.
     */
    User login(String nric, String password);

    /**
     * Logs out the current user by clearing AuthStore.
     */
    void logout();
}