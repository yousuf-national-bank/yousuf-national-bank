package bank.gui;

import bank.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

class AdminDashboard extends JPanel {
    private final BankApp app;
    private final Admin admin;
    private final Bank bank;

    private DefaultTableModel customersModel;
    private DefaultTableModel accountsModel;
    private DefaultTableModel loansModel;
    private JLabel summaryLabel;

    AdminDashboard(BankApp app, Admin admin) {
        this.app = app;
        this.admin = admin;
        this.bank = app.bank();
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.FONT_BOLD);
        tabs.addTab("Customers", buildCustomersTab());
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Loans", buildLoansTab());
        tabs.addTab("Reports", buildReportsTab());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));
        wrapper.setBackground(UI.BG);
        wrapper.add(tabs, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        refreshAll();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UI.NAVY);
        bar.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("Admin Panel — " + admin.getUsername());
        title.setFont(UI.FONT_HEADER);
        title.setForeground(Color.WHITE);

        JButton logout = UI.secondaryButton("Log Out");
        logout.addActionListener(e -> { app.persist(); app.showLogin(); });

        bar.add(title, BorderLayout.WEST);
        bar.add(logout, BorderLayout.EAST);
        return bar;
    }

    // -------------------------------------------------------- Customers tab

    private JPanel buildCustomersTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        customersModel = new DefaultTableModel(new Object[]{"Username", "Full Name", "Email", "Phone", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(customersModel);
        UI.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton toggleLock = UI.secondaryButton("Lock / Unlock Selected");
        buttons.add(toggleLock);

        toggleLock.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { UI.showError(this, "Select a customer first."); return; }
            String username = (String) customersModel.getValueAt(row, 0);
            Customer c = bank.getCustomer(username);
            c.setLocked(!c.isLocked());
            app.persist();
            refreshAll();
        });

        p.add(buttons, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // --------------------------------------------------------- Accounts tab

    private JPanel buildAccountsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        accountsModel = new DefaultTableModel(new Object[]{"Account #", "Owner", "Type", "Balance", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(accountsModel);
        UI.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton toggleFreeze = UI.secondaryButton("Freeze / Unfreeze Selected");
        JButton applyInterest = UI.primaryButton("Apply Interest To All Accounts");
        buttons.add(toggleFreeze); buttons.add(applyInterest);

        toggleFreeze.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { UI.showError(this, "Select an account first."); return; }
            String accNo = (String) accountsModel.getValueAt(row, 0);
            try {
                Account acc = bank.getAccount(accNo);
                acc.setFrozen(!acc.isFrozen());
                app.persist();
                refreshAll();
            } catch (Exception ex) {
                UI.showError(this, ex.getMessage());
            }
        });

        applyInterest.addActionListener(e -> {
            bank.applyInterestToAll();
            app.persist();
            refreshAll();
            UI.showInfo(this, "Interest applied to all eligible accounts.");
        });

        p.add(buttons, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ Loans tab

    private JPanel buildLoansTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        loansModel = new DefaultTableModel(new Object[]{"Loan ID", "Customer", "Principal", "Term", "Owed", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(loansModel);
        UI.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton approve = UI.primaryButton("Approve Selected");
        JButton reject = UI.dangerButton("Reject Selected");
        buttons.add(approve); buttons.add(reject);

        approve.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { UI.showError(this, "Select a loan first."); return; }
            String loanId = (String) loansModel.getValueAt(row, 0);
            try {
                bank.approveLoan(loanId);
                app.persist();
                refreshAll();
                UI.showInfo(this, "Loan " + loanId + " approved and disbursed.");
            } catch (Exception ex) {
                UI.showError(this, ex.getMessage());
            }
        });

        reject.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { UI.showError(this, "Select a loan first."); return; }
            String loanId = (String) loansModel.getValueAt(row, 0);
            bank.rejectLoan(loanId);
            app.persist();
            refreshAll();
        });

        p.add(buttons, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // --------------------------------------------------------- Reports tab

    private JPanel buildReportsTab() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));

        JPanel card = UI.card();
        card.setLayout(new GridLayout(0, 1, 0, 10));
        card.setMaximumSize(new Dimension(480, 260));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        summaryLabel = new JLabel();
        summaryLabel.setFont(UI.FONT_BODY);
        card.add(UI.header("Bank Summary"));
        card.add(summaryLabel);

        JButton refresh = UI.secondaryButton("Refresh");
        refresh.addActionListener(e -> refreshAll());

        p.add(card);
        p.add(Box.createVerticalStrut(12));
        p.add(refresh);
        return p;
    }

    // -------------------------------------------------------------- helpers

    private void refreshAll() {
        customersModel.setRowCount(0);
        for (Customer c : bank.getAllCustomers().values()) {
            customersModel.addRow(new Object[]{
                    c.getUsername(), c.getFullName(), c.getEmail(), c.getPhone(),
                    c.isLocked() ? "LOCKED" : "ACTIVE"});
        }

        accountsModel.setRowCount(0);
        for (Account a : bank.getAllAccounts().values()) {
            accountsModel.addRow(new Object[]{
                    a.getAccountNumber(), a.getOwnerUsername(), a.getAccountType(),
                    String.format("%.2f", a.getBalance()), a.isFrozen() ? "FROZEN" : "ACTIVE"});
        }

        loansModel.setRowCount(0);
        for (Loan l : bank.getAllLoans().values()) {
            loansModel.addRow(new Object[]{
                    l.getLoanId(), l.getCustomerUsername(), String.format("%.2f", l.getPrincipal()),
                    l.getTermMonths() + " mo", String.format("%.2f", l.getOutstandingBalance()), l.getStatus()});
        }

        if (summaryLabel != null) {
            summaryLabel.setText(String.format(
                    "<html>Total customers: %d<br>Total accounts: %d<br>Total deposits: %.2f<br>" +
                    "Total loans issued: %d<br>Total outstanding loan balance: %.2f</html>",
                    bank.getAllCustomers().size(), bank.getAllAccounts().size(), bank.getTotalDeposits(),
                    bank.getAllLoans().size(), bank.getTotalOutstandingLoans()));
        }
    }
}
