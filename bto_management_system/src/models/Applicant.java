package models; 

import enums.MaritalStatus; 
import enums.UserRole; 

public class Applicant extends User {
    private static final long serialVersionUID = 1L;

    private Integer currentApplicationId = null;

    public Applicant(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        super(nric, password, name, age, maritalStatus, UserRole.APPLICANT);
    }

    public Integer getCurrentApplicationId() {
        return currentApplicationId;
    }

    public void setCurrentApplicationId(Integer currentApplicationId) {
        this.currentApplicationId = currentApplicationId;
    }

    public void clearCurrentApplication() {
        this.currentApplicationId = null;
    }
}
