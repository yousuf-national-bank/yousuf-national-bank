package bank;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One recorded staff action, for accountability — who approved what, who froze which account, etc. */
public class AuditEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String actorUsername;
    private final StaffRole actorRole;
    private final String action;
    private final String details;
    private final LocalDateTime timestamp;

    public AuditEntry(String actorUsername, StaffRole actorRole, String action, String details) {
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public String getActorUsername() { return actorUsername; }
    public StaffRole getActorRole() { return actorRole; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
