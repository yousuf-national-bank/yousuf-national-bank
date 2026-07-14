package bank;

import java.io.Serializable;
import java.time.LocalDate;

/** A recurring transfer the bank runs automatically on its own schedule, without the customer initiating it each time. */
public class StandingOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String ownerUsername;
    private final String fromAccount;
    private final String toAccount;
    private final double amount;
    private final String note;
    private final Frequency frequency;
    private LocalDate nextRunDate;
    private boolean active;
    private int timesRun;
    private String lastResult; // null until it has run at least once

    public StandingOrder(String id, String ownerUsername, String fromAccount, String toAccount,
                          double amount, String note, Frequency frequency, LocalDate startDate) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.note = note;
        this.frequency = frequency;
        this.nextRunDate = startDate;
        this.active = true;
        this.timesRun = 0;
    }

    public LocalDate advance() {
        switch (frequency) {
            case DAILY: nextRunDate = nextRunDate.plusDays(1); break;
            case WEEKLY: nextRunDate = nextRunDate.plusWeeks(1); break;
            case MONTHLY: nextRunDate = nextRunDate.plusMonths(1); break;
        }
        return nextRunDate;
    }

    public String getId() { return id; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }
    public double getAmount() { return amount; }
    public String getNote() { return note; }
    public Frequency getFrequency() { return frequency; }
    public LocalDate getNextRunDate() { return nextRunDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getTimesRun() { return timesRun; }
    public void incrementTimesRun() { timesRun++; }
    public String getLastResult() { return lastResult; }
    public void setLastResult(String lastResult) { this.lastResult = lastResult; }
}
