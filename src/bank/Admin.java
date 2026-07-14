package bank;

import java.io.Serializable;

/** A bank staff login — either a front-desk TELLER or a full ADMIN. */
public class Admin implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String username;
    private String passwordHash;
    private final StaffRole role;

    public Admin(String username, String password, StaffRole role) {
        this.username = username;
        this.passwordHash = SecurityUtil.hash(password);
        this.role = role;
    }

    /** Rehydration constructor for loading an already-hashed password from the database. */
    public Admin(String username, String passwordHash, StaffRole role, boolean alreadyHashed) {
        this.username = username;
        this.passwordHash = alreadyHashed ? passwordHash : SecurityUtil.hash(passwordHash);
        this.role = role;
    }

    public boolean checkPassword(String password) {
        return passwordHash.equals(SecurityUtil.hash(password));
    }

    public String getUsername() { return username; }
    public StaffRole getRole() { return role; }
    public boolean isFullAdmin() { return role == StaffRole.ADMIN; }

    /** Exposes the stored hash only for writing to persistent storage — never for display or comparison. */
    public String getPasswordHashForPersistence() { return passwordHash; }
}
