package bank;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Base class for every kind of account the bank offers. */
public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String accountNumber;
    protected final String ownerUsername;
    protected double balance;
    protected boolean frozen;
    protected boolean closed = false;
    protected final LocalDate openedOn;
    protected final List<Transaction> transactions = new ArrayList<>();

    public Account(String accountNumber, String ownerUsername, double openingBalance) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.balance = openingBalance;
        this.frozen = false;
        this.openedOn = LocalDate.now();
    }

    /** Used to rehydrate an account exactly as it was stored (e.g. loaded from the database). */
    protected Account(String accountNumber, String ownerUsername, double balance, boolean frozen, LocalDate openedOn) {
        this.accountNumber = accountNumber;
        this.ownerUsername = ownerUsername;
        this.balance = balance;
        this.frozen = frozen;
        this.openedOn = openedOn;
    }

    /** Appends a transaction that was already recorded previously (no new ID/timestamp generated). */
    public void restoreTransaction(Transaction t) {
        transactions.add(t);
    }

    public abstract String getAccountType();

    /** Interest rate applied per interest cycle (0 for accounts that don't earn interest). */
    public abstract double getInterestRate();

    /** Whether the account is allowed to go below zero, and by how much (0 = no overdraft). */
    public double getOverdraftLimit() {
        return 0.0;
    }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    public void deposit(double amount, String description) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Deposit amount must be positive.");
        if (closed) throw new IllegalStateException("Account " + accountNumber + " is closed.");
        if (frozen) throw new IllegalStateException("Account " + accountNumber + " is frozen.");
        balance += amount;
        log(TransactionType.DEPOSIT, amount, description);
    }

    /** A generic credit used for money the bank itself issues (salary, refunds, etc.), tagged with its own type. */
    public void credit(TransactionType type, double amount, String description) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Credit amount must be positive.");
        if (closed) throw new IllegalStateException("Account " + accountNumber + " is closed.");
        if (frozen) throw new IllegalStateException("Account " + accountNumber + " is frozen.");
        balance += amount;
        log(type, amount, description);
    }

    public void withdraw(double amount, String description) throws InvalidAmountException, InsufficientFundsException {
        withdraw(TransactionType.WITHDRAWAL, amount, description);
    }

    /** Same withdrawal logic, but tags the transaction with a specific type (e.g. a payroll debit). */
    public void withdraw(TransactionType type, double amount, String description) throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive.");
        if (closed) throw new IllegalStateException("Account " + accountNumber + " is closed.");
        if (frozen) throw new IllegalStateException("Account " + accountNumber + " is frozen.");
        if (balance - amount < -getOverdraftLimit()) {
            throw new InsufficientFundsException(
                    "Insufficient funds in " + accountNumber + ". Available: " + (balance + getOverdraftLimit()));
        }
        balance -= amount;
        log(type, amount, description);
    }

    protected void log(TransactionType type, double amount, String description) {
        String txId = "TXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        transactions.add(new Transaction(txId, accountNumber, type, amount, balance, description));
    }

    public void applyInterest() {
        double rate = getInterestRate();
        if (rate <= 0 || balance <= 0) return;
        double interest = balance * rate;
        balance += interest;
        log(TransactionType.INTEREST_CREDIT, interest, "Interest credited at " + (rate * 100) + "%");
    }

    public String getAccountNumber() { return accountNumber; }
    public String getOwnerUsername() { return ownerUsername; }
    public double getBalance() { return balance; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }
    public LocalDate getOpenedOn() { return openedOn; }
    public List<Transaction> getTransactions() { return Collections.unmodifiableList(transactions); }

    @Override
    public String toString() {
        return String.format("%-6s | %-12s | Owner: %-10s | Balance: %10.2f | %s",
                getAccountType(), accountNumber, ownerUsername, balance, frozen ? "FROZEN" : "ACTIVE");
    }
}
