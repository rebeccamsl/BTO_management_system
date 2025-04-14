package models; // Updated package

import enums.MaritalStatus; // Updated import
import enums.UserRole; // Updated import
import java.util.ArrayList;
import java.util.List;


public class HDBOfficer extends User {
    private static final long serialVersionUID = 1L;

    private Integer handlingProjectId = null;

    public HDBOfficer(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        super(nric, password, name, age, maritalStatus, UserRole.OFFICER);
    }

    // Getters/Setters specific to HDBOfficer
    public Integer getHandlingProjectId() {
        return handlingProjectId;
    }

    public void setHandlingProjectId(Integer handlingProjectId) {
        this.handlingProjectId = handlingProjectId;
    }

    public void clearHandlingProject() {
        this.handlingProjectId = null;
    }
}