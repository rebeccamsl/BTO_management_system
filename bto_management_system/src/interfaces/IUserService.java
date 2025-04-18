package interfaces;

import models.User;

public interface IUserService {
    /**
     * Changes the password for the user identified by NRIC.
     * Requires correct old password.
     * @param nric NRIC of the user.
     * @param oldPassword Current password.
     * @param newPassword New password to set.
     * @return true if password changed successfully, false otherwise.
     */
    boolean changePassword(String nric, String oldPassword, String newPassword);

    /**
     * Retrieves user details by NRIC.
     * @param nric NRIC of the user.
     * @return User object or null if not found.
     */
    User getUserDetails(String nric);


}