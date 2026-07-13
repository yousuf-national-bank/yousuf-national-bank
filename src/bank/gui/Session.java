package bank.gui;

import bank.Bank;
import bank.db.BankRepository;
import bank.db.DatabaseManager;

import java.sql.SQLException;

/** Holds the live Bank instance plus the repository, and centralizes the "save after every change" call. */
final class Session {
    final Bank bank;
    private final BankRepository repository;

    private Session(Bank bank, BankRepository repository) {
        this.bank = bank;
        this.repository = repository;
    }

    static Session start(String bankName) throws SQLException {
        DatabaseManager dbManager = new DatabaseManager();
        BankRepository repo = new BankRepository(dbManager);
        repo.init();
        Bank bank = repo.loadAll(bankName);
        return new Session(bank, repo);
    }

    void persist() {
        try {
            repository.saveAll(bank);
        } catch (SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "Could not save to the database:\n" + e.getMessage(),
                    "Database error", javax.swing.JOptionPane.WARNING_MESSAGE);
        }
    }
}
