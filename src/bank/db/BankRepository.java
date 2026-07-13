package bank.db;

import bank.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.Map;

/**
 * Reads and writes the whole {@link Bank} object graph to SQLite.
 *
 * Usage mirrors the old FileStorage class:
 *   Bank bank = repository.loadAll("My Bank");   // at startup
 *   repository.saveAll(bank);                     // after every mutation
 *
 * Customers/accounts/loans/admins are upserted (INSERT OR REPLACE); transactions
 * are append-only and inserted with INSERT OR IGNORE so re-saving never duplicates them.
 */
public class BankRepository {
    private final DatabaseManager db;

    public BankRepository(DatabaseManager db) {
        this.db = db;
    }

    public void init() throws SQLException {
        db.initSchema();
    }

    // ---------------------------------------------------------------- SAVE

    public void saveAll(Bank bank) throws SQLException {
        Connection c = db.connect();
        boolean prevAutoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            saveAdmins(c, bank);
            saveCustomers(c, bank);
            saveAccounts(c, bank);
            saveLoans(c, bank);
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(prevAutoCommit);
        }
    }

    private void saveAdmins(Connection c, Bank bank) throws SQLException {
        String sql = "INSERT OR REPLACE INTO admins (username, password_hash) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Admin a : reflectAdmins(bank)) {
                ps.setString(1, a.getUsername());
                ps.setString(2, passwordHashOf(a));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveCustomers(Connection c, Bank bank) throws SQLException {
        String sql = "INSERT OR REPLACE INTO customers (username, pin_hash, full_name, email, phone, locked) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Customer cust : bank.getAllCustomers().values()) {
                ps.setString(1, cust.getUsername());
                ps.setString(2, pinHashOf(cust));
                ps.setString(3, cust.getFullName());
                ps.setString(4, cust.getEmail());
                ps.setString(5, cust.getPhone());
                ps.setInt(6, cust.isLocked() ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveAccounts(Connection c, Bank bank) throws SQLException {
        String accSql = "INSERT OR REPLACE INTO accounts " +
                "(account_number, owner_username, acc_type, balance, frozen, opened_on, term_months, maturity_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String txSql = "INSERT OR IGNORE INTO transactions " +
                "(transaction_id, account_number, tx_type, amount, balance_after, ts, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement accPs = c.prepareStatement(accSql);
             PreparedStatement txPs = c.prepareStatement(txSql)) {
            for (Account a : bank.getAllAccounts().values()) {
                accPs.setString(1, a.getAccountNumber());
                accPs.setString(2, a.getOwnerUsername());
                accPs.setString(3, a.getAccountType());
                accPs.setDouble(4, a.getBalance());
                accPs.setInt(5, a.isFrozen() ? 1 : 0);
                accPs.setString(6, a.getOpenedOn().toString());
                if (a instanceof FixedDepositAccount) {
                    FixedDepositAccount fd = (FixedDepositAccount) a;
                    accPs.setInt(7, fd.getTermMonths());
                    accPs.setString(8, fd.getMaturityDate().toString());
                } else {
                    accPs.setNull(7, Types.INTEGER);
                    accPs.setNull(8, Types.VARCHAR);
                }
                accPs.addBatch();

                for (Transaction t : a.getTransactions()) {
                    txPs.setString(1, t.getTransactionId());
                    txPs.setString(2, t.getAccountNumber());
                    txPs.setString(3, t.getType().name());
                    txPs.setDouble(4, t.getAmount());
                    txPs.setDouble(5, t.getBalanceAfter());
                    txPs.setString(6, t.getTimestamp().toString());
                    txPs.setString(7, t.getDescription());
                    txPs.addBatch();
                }
            }
            accPs.executeBatch();
            txPs.executeBatch();
        }
    }

    private void saveLoans(Connection c, Bank bank) throws SQLException {
        String sql = "INSERT OR REPLACE INTO loans " +
                "(loan_id, customer_username, linked_account, principal, term_months, outstanding_balance, status, applied_on) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Loan l : bank.getAllLoans().values()) {
                ps.setString(1, l.getLoanId());
                ps.setString(2, l.getCustomerUsername());
                ps.setString(3, l.getLinkedAccountNumber());
                ps.setDouble(4, l.getPrincipal());
                ps.setInt(5, l.getTermMonths());
                ps.setDouble(6, l.getOutstandingBalance());
                ps.setString(7, l.getStatus().name());
                ps.setString(8, l.getAppliedOn().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------- LOAD

    /** Rebuilds a full Bank from the database. Returns an empty bank (with a default admin) if the DB has no data yet. */
    public Bank loadAll(String bankName) throws SQLException {
        Connection c = db.connect();
        Bank bank = Bank.empty(bankName);

        boolean anyAdmin = false;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT username, password_hash FROM admins")) {
            while (rs.next()) {
                bank.restoreAdmin(new Admin(rs.getString("username"), rs.getString("password_hash"), true));
                anyAdmin = true;
            }
        }
        if (!anyAdmin) {
            bank.restoreAdmin(new Admin("admin", "admin123")); // default admin on a brand-new database
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM customers")) {
            while (rs.next()) {
                Customer cust = new Customer(
                        rs.getString("username"),
                        rs.getString("pin_hash"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        true,
                        rs.getInt("locked") == 1);
                bank.restoreCustomer(cust);
            }
        }

        int maxAccSuffix = 1000;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM accounts")) {
            while (rs.next()) {
                String accNo = rs.getString("account_number");
                String type = rs.getString("acc_type");
                String owner = rs.getString("owner_username");
                double balance = rs.getDouble("balance");
                boolean frozen = rs.getInt("frozen") == 1;
                LocalDate openedOn = LocalDate.parse(rs.getString("opened_on"));

                Account acc;
                if ("FIXED-D".equals(type)) {
                    int term = rs.getInt("term_months");
                    LocalDate maturity = LocalDate.parse(rs.getString("maturity_date"));
                    acc = new FixedDepositAccount(accNo, owner, balance, frozen, openedOn, term, maturity);
                } else if ("CHECKING".equals(type)) {
                    acc = new CheckingAccount(accNo, owner, balance, frozen, openedOn);
                } else {
                    acc = new SavingsAccount(accNo, owner, balance, frozen, openedOn);
                }
                bank.restoreAccount(acc);
                Customer owningCust = bank.getCustomer(owner);
                if (owningCust != null && !owningCust.getAccountNumbers().contains(accNo)) {
                    owningCust.addAccount(accNo);
                }
                maxAccSuffix = Math.max(maxAccSuffix, extractSuffix(accNo, "ACC"));
            }
        }

        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM transactions ORDER BY ts ASC")) {
            while (rs.next()) {
                String accNo = rs.getString("account_number");
                Account acc = bank.getAllAccounts().get(accNo);
                if (acc == null) continue;
                Transaction t = new Transaction(
                        rs.getString("transaction_id"),
                        accNo,
                        TransactionType.valueOf(rs.getString("tx_type")),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_after"),
                        rs.getString("description"));
                acc.restoreTransaction(t);
            }
        }

        int maxLoanSuffix = 500;
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM loans")) {
            while (rs.next()) {
                String loanId = rs.getString("loan_id");
                Loan loan = new Loan(
                        loanId,
                        rs.getString("customer_username"),
                        rs.getString("linked_account"),
                        rs.getDouble("principal"),
                        rs.getInt("term_months"),
                        rs.getDouble("outstanding_balance"),
                        LoanStatus.valueOf(rs.getString("status")),
                        LocalDate.parse(rs.getString("applied_on")));
                bank.restoreLoan(loan);
                Customer owningCust = bank.getCustomer(loan.getCustomerUsername());
                if (owningCust != null && !owningCust.getLoanIds().contains(loanId)) {
                    owningCust.addLoan(loanId);
                }
                maxLoanSuffix = Math.max(maxLoanSuffix, extractSuffix(loanId, "LOAN"));
            }
        }

        bank.fastForwardSequences(maxAccSuffix, maxLoanSuffix);
        return bank;
    }

    private int extractSuffix(String id, String prefix) {
        try {
            return Integer.parseInt(id.substring(prefix.length()));
        } catch (Exception e) {
            return 0;
        }
    }

    // small reflection-free helpers since Admin/Customer don't expose hashes publicly by design
    private Iterable<Admin> reflectAdmins(Bank bank) {
        return bank.getAllAdmins();
    }

    private String passwordHashOf(Admin a) { return a.getPasswordHashForPersistence(); }
    private String pinHashOf(Customer c) { return c.getPinHashForPersistence(); }
}
