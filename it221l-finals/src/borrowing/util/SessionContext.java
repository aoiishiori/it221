package borrowing.util;

public class SessionContext {
    private static SessionContext instance;
    private String personId, username, role;
    private SessionContext() {}
    public static SessionContext getInstance() {
        if (instance == null) instance = new SessionContext();
        return instance;
    }
    public void login(String personId, String username, String role) {
        this.personId = personId; this.username = username; this.role = role;
    }
    public void logout() { this.personId = null; this.username = null; this.role = null; }
    public String getPersonId() { return personId; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
}
