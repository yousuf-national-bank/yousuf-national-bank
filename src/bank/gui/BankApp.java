package bank.gui;

import bank.Bank;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class BankApp extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel();
    private Session session;

    private static final String CARD_LOGIN = "login";
    private static final String CARD_CUSTOMER = "customer";
    private static final String CARD_ADMIN = "admin";

    public BankApp() {
        super("Yousuf National Bank");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);

        try {
            session = Session.start("Yousuf National Bank");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to the database:\n" + e.getMessage() +
                    "\n\nMake sure the SQLite JDBC driver jar is on your classpath.",
                    "Database connection failed", JOptionPane.ERROR_MESSAGE);
            session = null;
        }

        cards.setLayout(cardLayout);
        cards.add(new LoginPanel(this), CARD_LOGIN);
        setContentPane(cards);
        showLogin();
    }

    Bank bank() {
        return session != null ? session.bank : null;
    }

    void persist() {
        if (session != null) session.persist();
    }

    void showLogin() {
        rebuildCard(CARD_LOGIN, new LoginPanel(this));
    }

    void showCustomerDashboard(bank.Customer customer) {
        rebuildCard(CARD_CUSTOMER, new CustomerDashboard(this, customer));
    }

    void showAdminDashboard(bank.Admin admin) {
        rebuildCard(CARD_ADMIN, new AdminDashboard(this, admin));
    }

    private void rebuildCard(String name, JPanel panel) {
        for (Component c : cards.getComponents()) {
            if (name.equals(c.getName())) {
                cards.remove(c);
                break;
            }
        }
        panel.setName(name);
        cards.add(panel, name);
        cardLayout.show(cards, name);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new BankApp().setVisible(true));
    }
}
