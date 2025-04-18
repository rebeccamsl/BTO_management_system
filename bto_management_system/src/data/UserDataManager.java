package data;

import models.*; 
import enums.*; 
import utils.DateUtils;
import utils.NRICValidator;

import java.io.IOException;
import java.util.Map;

public class UserDataManager extends AbstractCsvDataManager<String, User> {

    @Override
    protected String getHeaderLine() {
        return "NRIC,Password,Name,Age,MaritalStatus,Role";
    }

    @Override
    protected User parseCsvRow(String[] values) {
        if (values.length < 6) {
             System.err.println("Skipping malformed user row: Not enough columns.");
             return null; // Skip row if not enough columns
        }

        String nric = safeParseString(values[0]);
        String password = safeParseString(values[1]);
        String name = safeParseString(values[2]);
        int age = safeParseInt(values[3], 0); 
        MaritalStatus maritalStatus = MaritalStatus.valueOf(safeParseString(values[4]).toUpperCase());
        UserRole role = UserRole.valueOf(safeParseString(values[5]).toUpperCase());

        // Basic validation
        if (!NRICValidator.isValidFormat(nric)) {
            System.err.println("Skipping user row: Invalid NRIC format '" + nric + "'");
            return null;
        }
         if (age <= 0) { 
             System.err.println("Skipping user row: Invalid age '" + values[3] + "' for NRIC " + nric);
             return null;
         }


        // Instantiate correct subclass based on role
        switch (role) {
            case APPLICANT:
                return new Applicant(nric, password, name, age, maritalStatus);
            case OFFICER:
                return new HDBOfficer(nric, password, name, age, maritalStatus);
            case MANAGER:
                return new HDBManager(nric, password, name, age, maritalStatus);
            default:
                 System.err.println("Skipping user row: Unknown role '" + values[5] + "' for NRIC " + nric);
                 return null; // Skip unknown roles
        }
    }

    @Override
    protected String formatCsvRow(User user) {
        return String.join(CSV_DELIMITER,
                user.getNric(),
                user.getPassword(), // Store plain text as per brief (HASH IN REAL WORLD!)
                user.getName(),
                String.valueOf(user.getAge()),
                user.getMaritalStatus().name(),
                user.getRole().name()
        );
    }

    @Override
    protected String getKey(User user) {
        return user.getNric();
    }
}