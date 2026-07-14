package bank;

import java.time.LocalDate;

public class FixedDepositAccount extends Account {
    private static final long serialVersionUID = 1L;
    private static final double INTEREST_RATE = 0.07; // 7% per cycle, higher reward
    private final int termMonths;
    private final LocalDate maturityDate;

    public FixedDepositAccount(String accountNumber, String ownerUsername, double openingBalance, int termMonths) {
        super(accountNumber, ownerUsername, openingBalance);
        this.termMonths = termMonths;
        this.maturityDate = LocalDate.now().plusMonths(termMonths);
    }

    /** Rehydration constructor — preserves the original maturity date instead of recomputing it. */
    public FixedDepositAccount(String accountNumber, String ownerUsername, double balance, boolean frozen,
                                LocalDate openedOn, int termMonths, LocalDate maturityDate) {
        super(accountNumber, ownerUsername, balance, frozen, openedOn);
        this.termMonths = termMonths;
        this.maturityDate = maturityDate;
    }

    @Override
    public String getAccountType() { return "FIXED-D"; }

    @Override
    public double getInterestRate() { return INTEREST_RATE; }

    public boolean isMatured() { return !LocalDate.now().isBefore(maturityDate); }

    public LocalDate getMaturityDate() { return maturityDate; }

    public int getTermMonths() { return termMonths; }

    @Override
    public void withdraw(TransactionType type, double amount, String description) throws InvalidAmountException, InsufficientFundsException {
        if (!isMatured()) {
            throw new IllegalStateException(
                    "Fixed deposit " + accountNumber + " has not matured yet (matures " + maturityDate + ").");
        }
        super.withdraw(type, amount, description);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" | Matures: %s", maturityDate);
    }
}
