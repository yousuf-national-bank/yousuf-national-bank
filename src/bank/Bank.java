package bank;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** The central engine: owns all customers, accounts, loans and the admin. */
public class Bank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String bankName;
    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Loan> loans = new LinkedHashMap<>();
    private final Map<String, Admin> admins = new LinkedHashMap<>();

    private final AtomicInteger accountSeq = new AtomicInteger(1000);
    private final AtomicInteger loanSeq = new AtomicInteger(500);

    public Bank(String bankName) {
        this(bankName, true);
    }

    private Bank(String bankName, boolean withDefaultAdmin) {
        this.bankName = bankName;
        if (withDefaultAdmin) {
            // default admin so the system is usable on first run
            admins.put("admin", new Admin("admin", "admin123"));
        }
    }

    /** An empty bank with no default admin — used when rehydrating from persistent storage. */
    public static Bank empty(String bankName) {
        return new Bank(bankName, false);
    }

    // ---------- Rehydration helpers (used by persistence layers) ----------

    public void restoreCustomer(Customer c) { customers.put(c.getUsername(), c); }
    public void restoreAccount(Account a) { accounts.put(a.getAccountNumber(), a); }
    public void restoreLoan(Loan l) { loans.put(l.getLoanId(), l); }
    public void restoreAdmin(Admin a) { admins.put(a.getUsername(), a); }

    /** Ensures new IDs generated after a reload never collide with previously stored ones. */
    public void fastForwardSequences(int maxAccountSuffix, int maxLoanSuffix) {
        while (accountSeq.get() < maxAccountSuffix) accountSeq.incrementAndGet();
        while (loanSeq.get() < maxLoanSuffix) loanSeq.incrementAndGet();
    }

    // ---------- Customer management ----------

    public Customer registerCustomer(String username, String pin, String fullName, String email, String phone) {
        if (customers.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }
        Customer c = new Customer(username, pin, fullName, email, phone);
        customers.put(username, c);
        return c;
    }

    public Customer login(String username, String pin) throws AuthenticationException {
        Customer c = customers.get(username);
        if (c == null || !c.checkPin(pin)) throw new AuthenticationException("Invalid username or PIN.");
        if (c.isLocked()) throw new AuthenticationException("This account is locked. Contact the bank.");
        return c;
    }

    public Admin loginAdmin(String username, String password) throws AuthenticationException {
        Admin a = admins.get(username);
        if (a == null || !a.checkPassword(password)) throw new AuthenticationException("Invalid admin credentials.");
        return a;
    }

    public Customer getCustomer(String username) { return customers.get(username); }
    public Map<String, Customer> getAllCustomers() { return customers; }

    // ---------- Account management ----------

    private String nextAccountNumber() {
        return "ACC" + accountSeq.incrementAndGet();
    }

    public Account openSavingsAccount(String username, double openingDeposit) {
        String accNo = nextAccountNumber();
        Account acc = new SavingsAccount(accNo, username, openingDeposit);
        accounts.put(accNo, acc);
        customers.get(username).addAccount(accNo);
        return acc;
    }

    public Account openCheckingAccount(String username, double openingDeposit) {
        String accNo = nextAccountNumber();
        Account acc = new CheckingAccount(accNo, username, openingDeposit);
        accounts.put(accNo, acc);
        customers.get(username).addAccount(accNo);
        return acc;
    }

    public Account openFixedDeposit(String username, double openingDeposit, int termMonths) {
        String accNo = nextAccountNumber();
        Account acc = new FixedDepositAccount(accNo, username, openingDeposit, termMonths);
        accounts.put(accNo, acc);
        customers.get(username).addAccount(accNo);
        return acc;
    }

    public Account getAccount(String accNo) throws AccountNotFoundException {
        Account a = accounts.get(accNo);
        if (a == null) throw new AccountNotFoundException("No account found with number " + accNo);
        return a;
    }

    public Map<String, Account> getAllAccounts() { return accounts; }

    public void transfer(String fromAcc, String toAcc, double amount, String note)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Account from = getAccount(fromAcc);
        Account to = getAccount(toAcc);
        from.withdraw(amount, "Transfer to " + toAcc + (note.isEmpty() ? "" : " (" + note + ")"));
        from.log(TransactionType.TRANSFER_OUT, amount, "To " + toAcc);
        to.deposit(amount, "Transfer from " + fromAcc + (note.isEmpty() ? "" : " (" + note + ")"));
        to.log(TransactionType.TRANSFER_IN, amount, "From " + fromAcc);
    }

    public void applyInterestToAll() {
        for (Account a : accounts.values()) a.applyInterest();
    }

    // ---------- Loan management ----------

    private String nextLoanId() { return "LOAN" + loanSeq.incrementAndGet(); }

    public Loan applyForLoan(String username, String linkedAccount, double principal, int termMonths) {
        String id = nextLoanId();
        Loan loan = new Loan(id, username, linkedAccount, principal, termMonths);
        loans.put(id, loan);
        customers.get(username).addLoan(id);
        return loan;
    }

    public Loan getLoan(String loanId) {
        return loans.get(loanId);
    }

    public Map<String, Loan> getAllLoans() { return loans; }

    public void approveLoan(String loanId) throws AccountNotFoundException, InvalidAmountException {
        Loan loan = loans.get(loanId);
        if (loan == null) throw new IllegalArgumentException("No such loan: " + loanId);
        loan.approve();
        Account acc = getAccount(loan.getLinkedAccountNumber());
        acc.deposit(loan.getPrincipal(), "Loan disbursement " + loanId);
        acc.log(TransactionType.LOAN_DISBURSEMENT, loan.getPrincipal(), loanId);
    }

    public void rejectLoan(String loanId) {
        Loan loan = loans.get(loanId);
        if (loan == null) throw new IllegalArgumentException("No such loan: " + loanId);
        loan.reject();
    }

    public void repayLoan(String loanId, String fromAccount, double amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Loan loan = loans.get(loanId);
        if (loan == null) throw new IllegalArgumentException("No such loan: " + loanId);
        Account acc = getAccount(fromAccount);
        acc.withdraw(amount, "Loan repayment " + loanId);
        acc.log(TransactionType.LOAN_REPAYMENT, amount, loanId);
        loan.repay(amount);
    }

    // ---------- Reporting ----------

    public double getTotalDeposits() {
        return accounts.values().stream().mapToDouble(Account::getBalance).sum();
    }

    public double getTotalOutstandingLoans() {
        return loans.values().stream()
                .filter(l -> l.getStatus() == LoanStatus.APPROVED)
                .mapToDouble(Loan::getOutstandingBalance).sum();
    }

    public String getBankName() { return bankName; }

    public java.util.Collection<Admin> getAllAdmins() { return admins.values(); }
}
