package bank;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Map<String, Employee> employees = new LinkedHashMap<>();
    private final List<AuditEntry> auditLog = new ArrayList<>();
    private final Map<String, Beneficiary> beneficiaries = new LinkedHashMap<>();
    private final Map<String, StandingOrder> standingOrders = new LinkedHashMap<>();

    private final AtomicInteger accountSeq = new AtomicInteger(1000);
    private final AtomicInteger loanSeq = new AtomicInteger(500);
    private final AtomicInteger employeeSeq = new AtomicInteger(100);
    private final AtomicInteger voucherSeq = new AtomicInteger(9000);
    private final AtomicInteger beneficiarySeq = new AtomicInteger(1);
    private final AtomicInteger standingOrderSeq = new AtomicInteger(1);

    public Bank(String bankName) {
        this(bankName, true);
    }

    private Bank(String bankName, boolean withDefaultAdmin) {
        this.bankName = bankName;
        if (withDefaultAdmin) {
            // default staff logins so the system is usable on first run
            admins.put("admin", new Admin("admin", "admin123", StaffRole.ADMIN));
            admins.put("teller", new Admin("teller", "teller123", StaffRole.TELLER));
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

    public static final int MAX_FAILED_PIN_ATTEMPTS = 5;

    public Customer login(String username, String pin) throws AuthenticationException {
        Customer c = customers.get(username);
        if (c == null) throw new AuthenticationException("Invalid username or PIN.");
        if (c.isLocked()) throw new AuthenticationException("This account is locked. Contact the bank.");
        if (!c.checkPin(pin)) {
            c.recordFailedAttempt();
            if (c.getFailedPinAttempts() >= MAX_FAILED_PIN_ATTEMPTS) {
                c.setLocked(true);
                throw new AuthenticationException(
                        "Too many failed attempts — this account has been locked for security. Contact the bank.");
            }
            int remaining = MAX_FAILED_PIN_ATTEMPTS - c.getFailedPinAttempts();
            throw new AuthenticationException("Invalid username or PIN. " + remaining + " attempt(s) remaining before lockout.");
        }
        c.resetFailedAttempts();
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

    // ---------- Staff management ----------

    public Admin addStaff(String username, String password, StaffRole role) {
        if (admins.containsKey(username)) throw new IllegalArgumentException("That staff username is already taken.");
        Admin a = new Admin(username, password, role);
        admins.put(username, a);
        return a;
    }

    public void removeStaff(String username) {
        Admin a = admins.get(username);
        if (a == null) throw new IllegalArgumentException("No such staff account: " + username);
        long remainingAdmins = admins.values().stream().filter(Admin::isFullAdmin).count();
        if (a.isFullAdmin() && remainingAdmins <= 1) {
            throw new IllegalStateException("Can't remove the last remaining Admin account.");
        }
        admins.remove(username);
    }

    // ---------- Audit log ----------

    public void logAudit(String actorUsername, StaffRole actorRole, String action, String details) {
        auditLog.add(new AuditEntry(actorUsername, actorRole, action, details));
    }

    /** Most recent entries first. */
    public List<AuditEntry> getRecentAuditLog(int limit) {
        List<AuditEntry> copy = new ArrayList<>(auditLog);
        java.util.Collections.reverse(copy);
        return copy.subList(0, Math.min(limit, copy.size()));
    }

    // ---------- Account closing ----------

    public void closeAccount(String accountNumber) throws AccountNotFoundException {
        Account acc = getAccount(accountNumber);
        if (acc.isClosed()) throw new IllegalStateException("Account " + accountNumber + " is already closed.");
        if (Math.abs(acc.getBalance()) > 0.005) {
            throw new IllegalStateException(
                    "Account must have a zero balance before it can be closed. Withdraw or transfer the remaining " +
                    String.format("%.2f", acc.getBalance()) + " first.");
        }
        acc.setClosed(true);
    }

    // ---------- Daily transaction limits ----------

    public static final double DAILY_WITHDRAWAL_LIMIT = 5000.0;
    public static final double DAILY_TRANSFER_LIMIT = 10000.0;

    /** Sum of today's withdrawals (cash-out only, not transfers) already made from this account. */
    public double todaysWithdrawalTotal(Account acc) {
        LocalDate today = LocalDate.now();
        return acc.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL)
                .filter(t -> t.getTimestamp().toLocalDate().equals(today))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /** Sum of today's outgoing transfers already made from this account. */
    public double todaysTransferTotal(Account acc) {
        LocalDate today = LocalDate.now();
        return acc.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .filter(t -> t.getTimestamp().toLocalDate().equals(today))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    // ---------- Beneficiaries (saved payees) ----------

    public Beneficiary addBeneficiary(String ownerUsername, String nickname, String accountNumber)
            throws AccountNotFoundException {
        getAccount(accountNumber); // throws if the target account doesn't exist
        String id = "BEN" + beneficiarySeq.getAndIncrement();
        Beneficiary b = new Beneficiary(id, ownerUsername, nickname, accountNumber);
        beneficiaries.put(id, b);
        return b;
    }

    public void removeBeneficiary(String ownerUsername, String beneficiaryId) {
        Beneficiary b = beneficiaries.get(beneficiaryId);
        if (b == null || !b.getOwnerUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("No such saved payee.");
        }
        beneficiaries.remove(beneficiaryId);
    }

    public List<Beneficiary> getBeneficiariesFor(String ownerUsername) {
        List<Beneficiary> list = new ArrayList<>();
        for (Beneficiary b : beneficiaries.values()) {
            if (b.getOwnerUsername().equals(ownerUsername)) list.add(b);
        }
        return list;
    }

    // ---------- Standing orders (recurring/scheduled payments) ----------

    public StandingOrder createStandingOrder(String ownerUsername, String fromAccount, String toAccount,
                                              double amount, String note, Frequency frequency, LocalDate startDate)
            throws AccountNotFoundException {
        getAccount(fromAccount);
        getAccount(toAccount);
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
        String id = "SO" + standingOrderSeq.getAndIncrement();
        StandingOrder so = new StandingOrder(id, ownerUsername, fromAccount, toAccount, amount, note, frequency, startDate);
        standingOrders.put(id, so);
        return so;
    }

    public void cancelStandingOrder(String ownerUsername, String standingOrderId) {
        StandingOrder so = standingOrders.get(standingOrderId);
        if (so == null || !so.getOwnerUsername().equals(ownerUsername)) {
            throw new IllegalArgumentException("No such standing order.");
        }
        so.setActive(false);
    }

    public List<StandingOrder> getStandingOrdersFor(String ownerUsername) {
        List<StandingOrder> list = new ArrayList<>();
        for (StandingOrder so : standingOrders.values()) {
            if (so.getOwnerUsername().equals(ownerUsername)) list.add(so);
        }
        return list;
    }

    /**
     * Executes every standing order whose next run date has arrived (or passed). Safe to call
     * repeatedly/frequently — it only acts on orders that are actually due. Failures (e.g.
     * insufficient funds) don't stop the batch; that order just tries again on its next due date.
     */
    public List<String> runDueStandingOrders() {
        List<String> ranLog = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (StandingOrder so : standingOrders.values()) {
            if (!so.isActive()) continue;
            while (!so.getNextRunDate().isAfter(today)) {
                try {
                    transfer(so.getFromAccount(), so.getToAccount(), so.getAmount(),
                            "Standing order " + so.getId() + (so.getNote() == null || so.getNote().isEmpty() ? "" : " — " + so.getNote()));
                    so.incrementTimesRun();
                    so.setLastResult("OK on " + today);
                    ranLog.add(so.getId() + ": paid " + so.getAmount() + " " + so.getFromAccount() + " -> " + so.getToAccount());
                } catch (Exception e) {
                    so.setLastResult("FAILED on " + today + ": " + e.getMessage());
                    ranLog.add(so.getId() + ": FAILED — " + e.getMessage());
                    // don't loop forever retrying a failing order same-day; advance and try again next cycle
                }
                so.advance();
            }
        }
        return ranLog;
    }

    // ---------- Payroll ----------

    private String nextEmployeeId() { return "EMP" + employeeSeq.incrementAndGet(); }
    private String nextVoucherId() { return "VCH" + voucherSeq.incrementAndGet(); }

    public Employee addEmployee(String accountNumber, String employerAccountNumber, String position, double monthlySalary)
            throws AccountNotFoundException {
        Account employeeAcc = getAccount(accountNumber);          // throws if missing
        Account employerAcc = getAccount(employerAccountNumber);  // throws if missing
        Customer employerCustomer = customers.get(employerAcc.getOwnerUsername());
        String employerName = employerCustomer != null ? employerCustomer.getFullName() : employerAcc.getOwnerUsername();
        String id = nextEmployeeId();
        Employee emp = new Employee(id, accountNumber, employeeAcc.getOwnerUsername(),
                employerAccountNumber, employerName, position, monthlySalary);
        employees.put(id, emp);
        return emp;
    }

    public Employee getEmployee(String employeeId) { return employees.get(employeeId); }
    public Map<String, Employee> getAllEmployees() { return employees; }

    public void removeEmployee(String employeeId) {
        if (employees.remove(employeeId) == null) throw new IllegalArgumentException("No such employee: " + employeeId);
    }

    public void toggleEmployeeActive(String employeeId) {
        Employee e = employees.get(employeeId);
        if (e == null) throw new IllegalArgumentException("No such employee: " + employeeId);
        e.setActive(!e.isActive());
    }

    /**
     * Runs payroll for every active employee: debits the salary from the employer's own
     * funding account and credits it to the employee's account — a real transfer, not
     * money created out of nothing. Employees are skipped (not failed) if either account
     * is missing/frozen, or the employer's account doesn't have enough funds.
     */
    public PayrollRunResult runPayroll() {
        PayrollRunResult result = new PayrollRunResult();
        LocalDate today = LocalDate.now();
        for (Employee emp : employees.values()) {
            if (!emp.isActive()) continue;
            Customer cust = customers.get(emp.getCustomerUsername());
            String employeeName = cust != null ? cust.getFullName() : emp.getCustomerUsername();

            Account employerAcc = accounts.get(emp.getEmployerAccountNumber());
            Account employeeAcc = accounts.get(emp.getAccountNumber());
            if (employerAcc == null) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Employer account no longer exists.");
                continue;
            }
            if (employeeAcc == null) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Employee account no longer exists.");
                continue;
            }
            if (employerAcc.isFrozen()) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Employer's account is frozen.");
                continue;
            }
            if (employeeAcc.isFrozen()) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Employee's account is frozen.");
                continue;
            }

            String voucherId = nextVoucherId();
            try {
                employerAcc.withdraw(TransactionType.SALARY_DEBIT, emp.getMonthlySalary(),
                        "Salary paid to " + employeeName + " (" + voucherId + ")");
            } catch (InsufficientFundsException e) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Employer's account has insufficient funds.");
                continue;
            } catch (InvalidAmountException e) {
                result.addSkipped(emp.getEmployeeId(), employeeName, "Configured salary is not a valid amount.");
                continue;
            }
            try {
                employeeAcc.credit(TransactionType.SALARY_CREDIT, emp.getMonthlySalary(),
                        "Salary from " + emp.getEmployerName() + " (" + voucherId + ")");
            } catch (InvalidAmountException e) {
                // Shouldn't happen since we just validated the same amount above, but if it does,
                // refund the employer so money isn't lost mid-transfer.
                try {
                    employerAcc.credit(TransactionType.SALARY_CREDIT, emp.getMonthlySalary(),
                            "Reversal — failed payroll credit (" + voucherId + ")");
                } catch (InvalidAmountException ignored) {
                }
                result.addSkipped(emp.getEmployeeId(), employeeName, "Could not credit employee account; debit reversed.");
                continue;
            }

            emp.markPaid(today);
            result.addPaid(new Payslip(voucherId, emp.getEmployeeId(), employeeName, emp.getAccountNumber(),
                    emp.getEmployerName(), emp.getPosition(), emp.getMonthlySalary(), employeeAcc.getBalance(), today));
        }
        return result;
    }
}
