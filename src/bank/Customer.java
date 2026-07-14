package bank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String pinHash;
    private String fullName;
    private String email;
    private String phone;
    private final List<String> accountNumbers = new ArrayList<>();
    private final List<String> loanIds = new ArrayList<>();
    private boolean locked = false;
    private int failedPinAttempts = 0;

    public Customer(String username, String pin, String fullName, String email, String phone) {
        this(username, pin, fullName, email, phone, false, false);
    }

    /** Rehydration constructor. Set pinAlreadyHashed=true when loading a stored hash from the database. */
    public Customer(String username, String pinOrHash, String fullName, String email, String phone,
                     boolean pinAlreadyHashed, boolean locked) {
        this.username = username;
        this.pinHash = pinAlreadyHashed ? pinOrHash : SecurityUtil.hash(pinOrHash);
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.locked = locked;
    }

    public boolean checkPin(String pin) {
        return pinHash.equals(SecurityUtil.hash(pin));
    }

    public void changePin(String newPin) { this.pinHash = SecurityUtil.hash(newPin); }

    public void addAccount(String accNo) { accountNumbers.add(accNo); }
    public void addLoan(String loanId) { loanIds.add(loanId); }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public List<String> getAccountNumbers() { return accountNumbers; }
    public List<String> getLoanIds() { return loanIds; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getFailedPinAttempts() { return failedPinAttempts; }
    public void recordFailedAttempt() { failedPinAttempts++; }
    public void resetFailedAttempts() { failedPinAttempts = 0; }

    /** Exposes the stored hash only for writing to persistent storage — never for display or comparison. */
    public String getPinHashForPersistence() { return pinHash; }

    @Override
    public String toString() {
        return String.format("%-10s | %-20s | %-25s | %-12s | %s",
                username, fullName, email, phone, locked ? "LOCKED" : "ACTIVE");
    }
}
