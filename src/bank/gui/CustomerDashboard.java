package bank.gui;

import bank.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

class CustomerDashboard extends JPanel {
    private final BankApp app;
    private final Customer customer;
    private final Bank bank;

    private DefaultTableModel accountsModel;
    private JTable accountsTable;
    private JComboBox<String> accountPicker;
    private DefaultTableModel txModel;
    private DefaultTableModel loansModel;
    private JLabel netWorthLabel;

    CustomerDashboard(BankApp app, Customer customer) {
        this.app = app;
        this.customer = customer;
        this.bank = app.bank();
        setLayout(new BorderLayout());
        setBackground(UI.BG);

        add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.FONT_BOLD);
        tabs.addTab("Accounts", buildAccountsTab());
        tabs.addTab("Transactions", buildTransactionsTab());
        tabs.addTab("Loans", buildLoansTab());
        tabs.addTab("Profile", buildProfileTab());

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

        JLabel welcome = new JLabel("Welcome back, " + customer.getFullName());
        welcome.setFont(UI.FONT_HEADER);
        welcome.setForeground(Color.WHITE);

        netWorthLabel = new JLabel();
        netWorthLabel.setFont(UI.FONT_SUBTITLE);
        netWorthLabel.setForeground(new Color(0xB9C6DA));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(welcome);
        left.add(netWorthLabel);

        JButton logout = UI.secondaryButton("Log Out");
        logout.addActionListener(e -> { app.persist(); app.showLogin(); });

        bar.add(left, BorderLayout.WEST);
        bar.add(logout, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------- Accounts tab

    private JPanel buildAccountsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        accountsModel = new DefaultTableModel(new Object[]{"Account #", "Type", "Balance", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        accountsTable = new JTable(accountsModel);
        UI.styleTable(accountsTable);
        JScrollPane scroll = new JScrollPane(accountsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton open = UI.primaryButton("Open New Account");
        JButton deposit = UI.secondaryButton("Deposit");
        JButton withdraw = UI.secondaryButton("Withdraw");
        JButton transfer = UI.secondaryButton("Transfer");
        buttons.add(open); buttons.add(deposit); buttons.add(withdraw); buttons.add(transfer);

        open.addActionListener(e -> openAccountDialog());
        deposit.addActionListener(e -> depositDialog());
        withdraw.addActionListener(e -> withdrawDialog());
        transfer.addActionListener(e -> transferDialog());

        p.add(buttons, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void openAccountDialog() {
        JComboBox<String> type = new JComboBox<>(new String[]{"Savings", "Checking", "Fixed Deposit"});
        JTextField amount = new JTextField("0");
        JTextField term = new JTextField("12");
        term.setEnabled(false);
        type.addActionListener(e -> term.setEnabled(type.getSelectedIndex() == 2));

        JPanel form = formPanel(
                new String[]{"Account Type", "Opening Deposit", "Term (months, FD only)"},
                new JComponent[]{type, amount, term});

        if (JOptionPane.showConfirmDialog(this, form, "Open New Account",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            double amt = Double.parseDouble(amount.getText().trim());
            Account acc;
            switch (type.getSelectedIndex()) {
                case 0: acc = bank.openSavingsAccount(customer.getUsername(), amt); break;
                case 1: acc = bank.openCheckingAccount(customer.getUsername(), amt); break;
                default:
                    int months = Integer.parseInt(term.getText().trim());
                    acc = bank.openFixedDeposit(customer.getUsername(), amt, months);
            }
            app.persist();
            refreshAll();
            UI.showInfo(this, "Account opened: " + acc.getAccountNumber());
        } catch (NumberFormatException ex) {
            UI.showError(this, "Please enter valid numbers.");
        }
    }

    private void depositDialog() {
        String accNo = pickOwnAccount("Deposit To");
        if (accNo == null) return;
        String amtStr = JOptionPane.showInputDialog(this, "Amount to deposit:");
        if (amtStr == null) return;
        try {
            Account acc = bank.getAccount(accNo);
            acc.deposit(Double.parseDouble(amtStr.trim()), "Deposit via GUI");
            app.persist();
            refreshAll();
            UI.showInfo(this, "New balance: " + String.format("%.2f", acc.getBalance()));
        } catch (Exception ex) {
            UI.showError(this, ex.getMessage());
        }
    }

    private void withdrawDialog() {
        String accNo = pickOwnAccount("Withdraw From");
        if (accNo == null) return;
        String amtStr = JOptionPane.showInputDialog(this, "Amount to withdraw:");
        if (amtStr == null) return;
        try {
            Account acc = bank.getAccount(accNo);
            acc.withdraw(Double.parseDouble(amtStr.trim()), "Withdrawal via GUI");
            app.persist();
            refreshAll();
            UI.showInfo(this, "New balance: " + String.format("%.2f", acc.getBalance()));
        } catch (Exception ex) {
            UI.showError(this, ex.getMessage());
        }
    }

    private void transferDialog() {
        String fromAcc = pickOwnAccount("Transfer From");
        if (fromAcc == null) return;
        JTextField toField = UI.textField();
        JTextField amtField = UI.textField();
        JTextField noteField = UI.textField();
        JPanel form = formPanel(
                new String[]{"Destination Account #", "Amount", "Note (optional)"},
                new JComponent[]{toField, amtField, noteField});
        if (JOptionPane.showConfirmDialog(this, form, "Transfer Funds",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            bank.transfer(fromAcc, toField.getText().trim(), Double.parseDouble(amtField.getText().trim()),
                    noteField.getText().trim());
            app.persist();
            refreshAll();
            UI.showInfo(this, "Transfer complete.");
        } catch (Exception ex) {
            UI.showError(this, ex.getMessage());
        }
    }

    // ------------------------------------------------------ Transactions tab

    private JPanel buildTransactionsTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        top.setOpaque(false);
        top.add(new JLabel("Account:"));
        accountPicker = new JComboBox<>();
        accountPicker.addActionListener(e -> refreshTransactions());
        top.add(accountPicker);

        txModel = new DefaultTableModel(new Object[]{"Date/Time", "Type", "Amount", "Balance After", "Description"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable txTable = new JTable(txModel);
        UI.styleTable(txTable);
        JScrollPane scroll = new JScrollPane(txTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        p.add(top, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void refreshTransactions() {
        txModel.setRowCount(0);
        String accNo = (String) accountPicker.getSelectedItem();
        if (accNo == null) return;
        try {
            Account acc = bank.getAccount(accNo);
            for (Transaction t : acc.getTransactions()) {
                txModel.addRow(new Object[]{
                        t.getTimestamp().toString().replace('T', ' '),
                        t.getType(),
                        String.format("%.2f", t.getAmount()),
                        String.format("%.2f", t.getBalanceAfter()),
                        t.getDescription()});
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ Loans tab

    private JPanel buildLoansTab() {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        p.setOpaque(false);

        loansModel = new DefaultTableModel(new Object[]{"Loan ID", "Principal", "Term", "Owed", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable loansTable = new JTable(loansModel);
        UI.styleTable(loansTable);
        JScrollPane scroll = new JScrollPane(loansTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xE5E9EF)));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        JButton apply = UI.primaryButton("Apply For Loan");
        JButton repay = UI.secondaryButton("Repay Selected Loan");
        buttons.add(apply); buttons.add(repay);

        apply.addActionListener(e -> applyLoanDialog());
        repay.addActionListener(e -> {
            int row = loansTable.getSelectedRow();
            if (row < 0) { UI.showError(this, "Select a loan first."); return; }
            repayLoanDialog((String) loansModel.getValueAt(row, 0));
        });

        p.add(buttons, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private void applyLoanDialog() {
        String accNo = pickOwnAccount("Disburse To");
        if (accNo == null) return;
        JTextField amount = UI.textField();
        JTextField term = UI.textField();
        term.setText("12");
        JPanel form = formPanel(new String[]{"Loan Amount", "Term (months)"}, new JComponent[]{amount, term});
        if (JOptionPane.showConfirmDialog(this, form, "Apply For Loan",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            Loan loan = bank.applyForLoan(customer.getUsername(), accNo,
                    Double.parseDouble(amount.getText().trim()), Integer.parseInt(term.getText().trim()));
            app.persist();
            refreshAll();
            UI.showInfo(this, "Application submitted: " + loan.getLoanId() + "\nAwaiting admin approval.");
        } catch (Exception ex) {
            UI.showError(this, ex.getMessage());
        }
    }

    private void repayLoanDialog(String loanId) {
        String accNo = pickOwnAccount("Repay From");
        if (accNo == null) return;
        String amtStr = JOptionPane.showInputDialog(this, "Amount to repay:");
        if (amtStr == null) return;
        try {
            bank.repayLoan(loanId, accNo, Double.parseDouble(amtStr.trim()));
            app.persist();
            refreshAll();
            UI.showInfo(this, "Repayment successful.");
        } catch (Exception ex) {
            UI.showError(this, ex.getMessage());
        }
    }

    // ---------------------------------------------------------- Profile tab

    private JPanel buildProfileTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(8, 8, 8, 8); gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField fullName = UI.textField(); fullName.setText(customer.getFullName());
        JTextField email = UI.textField(); email.setText(customer.getEmail());
        JTextField phone = UI.textField(); phone.setText(customer.getPhone());
        JPasswordField newPin = UI.passwordField();

        JPanel card = UI.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.gridx = 0; cc.gridy = 0; cc.anchor = GridBagConstraints.WEST;
        cc.insets = new Insets(6, 6, 6, 6); cc.fill = GridBagConstraints.HORIZONTAL;

        addLabeled(card, cc, "Full Name", fullName);
        addLabeled(card, cc, "Email", email);
        addLabeled(card, cc, "Phone", phone);
        addLabeled(card, cc, "New PIN (leave blank to keep current)", newPin);

        JButton save = UI.primaryButton("Save Changes");
        cc.gridx = 0; cc.gridy++; cc.gridwidth = 2;
        card.add(save, cc);

        save.addActionListener(e -> {
            customer.setFullName(fullName.getText().trim());
            customer.setEmail(email.getText().trim());
            customer.setPhone(phone.getText().trim());
            if (newPin.getPassword().length > 0) customer.changePin(new String(newPin.getPassword()));
            app.persist();
            UI.showInfo(this, "Profile updated.");
        });

        gc.gridx = 0; gc.gridy = 0;
        p.add(card, gc);
        return p;
    }

    private void addLabeled(JPanel p, GridBagConstraints gc, String label, JComponent field) {
        gc.gridx = 0; gc.gridwidth = 1;
        p.add(new JLabel(label), gc);
        gc.gridx = 1;
        p.add(field, gc);
        gc.gridy++;
    }

    // -------------------------------------------------------------- helpers

    private String pickOwnAccount(String title) {
        List<String> accs = customer.getAccountNumbers();
        if (accs.isEmpty()) {
            UI.showError(this, "You don't have any accounts yet. Open one first.");
            return null;
        }
        String choice = (String) JOptionPane.showInputDialog(this, "Choose an account:", title,
                JOptionPane.PLAIN_MESSAGE, null, accs.toArray(), accs.get(0));
        return choice;
    }

    private JPanel formPanel(String[] labels, JComponent[] fields) {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(6, 6, 6, 6); gc.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0;
            form.add(new JLabel(labels[i]), gc);
            gc.gridx = 1; gc.weightx = 1;
            form.add(fields[i], gc);
        }
        return form;
    }

    private void refreshAll() {
        accountsModel.setRowCount(0);
        accountPicker.removeAllItems();
        double total = 0;
        for (String accNo : customer.getAccountNumbers()) {
            try {
                Account acc = bank.getAccount(accNo);
                accountsModel.addRow(new Object[]{
                        acc.getAccountNumber(), acc.getAccountType(),
                        String.format("%.2f", acc.getBalance()),
                        acc.isFrozen() ? "FROZEN" : "ACTIVE"});
                accountPicker.addItem(accNo);
                total += acc.getBalance();
            } catch (AccountNotFoundException ignored) {
            }
        }
        netWorthLabel.setText("Total balance across accounts: " + String.format("%.2f", total));

        loansModel.setRowCount(0);
        for (String loanId : customer.getLoanIds()) {
            Loan l = bank.getLoan(loanId);
            if (l == null) continue;
            loansModel.addRow(new Object[]{
                    l.getLoanId(), String.format("%.2f", l.getPrincipal()),
                    l.getTermMonths() + " mo", String.format("%.2f", l.getOutstandingBalance()),
                    l.getStatus()});
        }
        refreshTransactions();
    }
}
