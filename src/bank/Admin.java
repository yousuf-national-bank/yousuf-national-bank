package bank;

import java.io.Serializable;

public class Admin implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String passwordHash;

    public Admin(String username, String password) {
        this.username = username;
        this.passwordHash = SecurityUtil.hash(password);
    }

    /** Rehydration constructor for loading an already-hashed password from the database. */
    public Admin(String username, String passwordHash, boolean alreadyHashed) {
        this.username = username;
        this.passwordHash = alreadyHashed ? passwordHash : SecurityUtil.hash(passwordHash);
    }

    public boolean checkPassword(String password) {
        return passwordHash.equals(SecurityUtil.hash(password));
    }

    public String getUsername() { return username; }

    /** Exposes the stored hash only for writing to persistent storage — never for display or comparison. */
    public String getPasswordHashForPersistence() { return passwordHash; }
}
