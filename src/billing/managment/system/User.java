package billing.management.system;

import java.util.Objects;

public abstract class User {
    private String userName;
    private String password;
    private int id;
    private String email;
    
    
    public User(String name, String password, int id, String email) {
        this.userName = name;
        this.password = password;
        this.id = id;
        this.email = email;
    }

    public String getName() {
        return userName;
    }

    public void setName(String name) {
        this.userName = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean login(String password, String name) {
        return Objects.equals(this.password, password) && Objects.equals(this.userName, name);
    }

    public abstract String toDataString();
}