package models; // Updated package

import enums.MaritalStatus; // Updated import
import enums.UserRole; // Updated import
import java.io.Serializable;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String nric;
    protected String password;
    protected String name;
    protected int age;
    protected MaritalStatus maritalStatus;
    protected UserRole role;

    // Constructor
    public User(String nric, String password, String name, int age, MaritalStatus maritalStatus, UserRole role) {
        this.nric = nric;
        this.password = password;
        this.name = name;
        this.age = age;
        this.maritalStatus = maritalStatus;
        this.role = role;
    }

    // Getters
    public String getNric() { return nric; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public MaritalStatus getMaritalStatus() { return maritalStatus; }
    public UserRole getRole() { return role; }

    // Setters
    public boolean setPassword(String oldPassword, String newPassword) {
        if (this.password.equals(oldPassword)) {
            this.password = newPassword;
            return true;
        }
        return false;
    }
    public void setAge(int age) { this.age = age; }
    public void setMaritalStatus(MaritalStatus maritalStatus) { this.maritalStatus = maritalStatus; }


    @Override
    public String toString() {
        return "User{" +
               "nric='" + nric + '\'' +
               ", name='" + name + '\'' +
               ", age=" + age +
               ", maritalStatus=" + maritalStatus +
               ", role=" + role +
               '}';
    }
}