package bank;

public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;
    private static final double INTEREST_RATE = 0.03; // 3% per cycle
    private static final double MIN_BALANCE = 100.0;

    public SavingsAccount(String accountNumber, String ownerUsername, double openingBalance) {
        super(accountNumber, ownerUsername, openingBalance);
    }

    public SavingsAccount(String accountNumber, String ownerUsername, double balance, boolean frozen, java.time.LocalDate openedOn) {
        super(accountNumber, ownerUsername, balance, frozen, openedOn);
    }

    @Override
    public String getAccountType() { return "SAVINGS"; }

    @Override
    public double getInterestRate() { return INTEREST_RATE; }

    @Override
    public void withdraw(TransactionType type, double amount, String description) throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive.");
        if (closed) throw new IllegalStateException("Account " + accountNumber + " is closed.");
        if (frozen) throw new IllegalStateException("Account " + accountNumber + " is frozen.");
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(
                    "Withdrawal declined: savings accounts must keep a minimum balance of " + MIN_BALANCE);
        }
        balance -= amount;
        log(type, amount, description);
    }
}
