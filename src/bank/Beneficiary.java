package bank;

import java.io.Serializable;
import java.time.LocalDate;

/** A saved payee — a nickname pointing at an account number, so transfers don't need retyping it every time. */
public class Beneficiary implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String ownerUsername;
    private String nickname;
    private final String accountNumber;
    private final LocalDate addedOn;

    public Beneficiary(String id, String ownerUsername, String nickname, String accountNumber) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.nickname = nickname;
        this.accountNumber = accountNumber;
        this.addedOn = LocalDate.now();
    }

    public String getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAccountNumber() { return accountNumber; }
    public LocalDate getAddedOn() { return addedOn; }
}
