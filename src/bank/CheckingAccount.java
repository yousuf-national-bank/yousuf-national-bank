package bank;

public class CheckingAccount extends Account {
    private static final long serialVersionUID = 1L;
    private static final double OVERDRAFT_LIMIT = 500.0;

    public CheckingAccount(String accountNumber, String ownerUsername, double openingBalance) {
        super(accountNumber, ownerUsername, openingBalance);
    }

    public CheckingAccount(String accountNumber, String ownerUsername, double balance, boolean frozen, java.time.LocalDate openedOn) {
        super(accountNumber, ownerUsername, balance, frozen, openedOn);
    }

    @Override
    public String getAccountType() { return "CHECKING"; }

    @Override
    public double getInterestRate() { return 0.0; } // checking accounts don't earn interest

    @Override
    public double getOverdraftLimit() { return OVERDRAFT_LIMIT; }
}
