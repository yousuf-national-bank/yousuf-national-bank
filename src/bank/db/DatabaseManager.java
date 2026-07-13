package bank.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection to the SQLite database file and creates the schema
 * on first run.
 *
 * NOTE: this class talks to SQLite through the standard java.sql API, which
 * ships with the JDK. To actually connect at runtime you need the SQLite
 * JDBC driver jar (org.xerial:sqlite-jdbc) on your classpath — see the
 * project README for the one-line download/setup.
 */
public final class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:sqlite:bankdata.db";
    private final String url;
    private Connection connection;

    public DatabaseManager() {
        this(DEFAULT_URL);
    }

    public DatabaseManager(String jdbcUrl) {
        this.url = jdbcUrl;
    }

    public Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "SQLite JDBC driver not found on the classpath. Download sqlite-jdbc.jar " +
                        "(org.xerial:sqlite-jdbc) and add it with -cp, e.g.:\n" +
                        "  java -cp out;sqlite-jdbc-3.46.0.0.jar bank.gui.BankApp   (Windows)\n" +
                        "  java -cp out:sqlite-jdbc-3.46.0.0.jar bank.gui.BankApp   (macOS/Linux)", e);
            }
            connection = DriverManager.getConnection(url);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void initSchema() throws SQLException {
        try (Statement st = connect().createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "username TEXT PRIMARY KEY," +
                    "password_hash TEXT NOT NULL)");

            st.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "username TEXT PRIMARY KEY," +
                    "pin_hash TEXT NOT NULL," +
                    "full_name TEXT," +
                    "email TEXT," +
                    "phone TEXT," +
                    "locked INTEGER NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                    "account_number TEXT PRIMARY KEY," +
                    "owner_username TEXT NOT NULL," +
                    "acc_type TEXT NOT NULL," +
                    "balance REAL NOT NULL," +
                    "frozen INTEGER NOT NULL DEFAULT 0," +
                    "opened_on TEXT NOT NULL," +
                    "term_months INTEGER," +
                    "maturity_date TEXT," +
                    "FOREIGN KEY(owner_username) REFERENCES customers(username))");

            st.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id TEXT PRIMARY KEY," +
                    "account_number TEXT NOT NULL," +
                    "tx_type TEXT NOT NULL," +
                    "amount REAL NOT NULL," +
                    "balance_after REAL NOT NULL," +
                    "ts TEXT NOT NULL," +
                    "description TEXT," +
                    "FOREIGN KEY(account_number) REFERENCES accounts(account_number))");

            st.execute("CREATE TABLE IF NOT EXISTS loans (" +
                    "loan_id TEXT PRIMARY KEY," +
                    "customer_username TEXT NOT NULL," +
                    "linked_account TEXT NOT NULL," +
                    "principal REAL NOT NULL," +
                    "term_months INTEGER NOT NULL," +
                    "outstanding_balance REAL NOT NULL," +
                    "status TEXT NOT NULL," +
                    "applied_on TEXT NOT NULL," +
                    "FOREIGN KEY(customer_username) REFERENCES customers(username))");
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {
        }
    }
}
