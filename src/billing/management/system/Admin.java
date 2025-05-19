package billing.management.system;

public class Admin extends User {
    public Admin(String name, String password, int id, String email) {
        super(name, password, id, email);
    }

    @Override
    public String toDataString() {
        return "Admin," + getName() + "," + getPassword() + "," + getId() + "," + getEmail();
    }
}