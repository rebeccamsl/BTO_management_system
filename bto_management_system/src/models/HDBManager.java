package models; 

import enums.MaritalStatus; 
import enums.UserRole; 

public class HDBManager extends User {
     private static final long serialVersionUID = 1L;

    public HDBManager(String nric, String password, String name, int age, MaritalStatus maritalStatus) {
        super(nric, password, name, age, maritalStatus, UserRole.MANAGER);
    }
}