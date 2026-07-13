package bank.gui;

import bank.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class LoginPanel extends JPanel {
    private final BankApp app;

    LoginPanel(BankApp app) {
        this.app = app;
        setLayout(new GridBagLayout());
        setBackground(UI.NAVY);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel bankTitle = new JLabel("🏦  Yousuf National Bank");
        bankTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        bankTitle.setForeground(Color.WHITE);
        bankTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Your money, managed simply.");
        tagline.setFont(UI.FONT_SUBTITLE);
        tagline.setForeground(new Color(0xB9C6DA));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UI.CARD_BG);
        card.setBorder(new EmptyBorder(0, 0, 0, 0));
        card.setMaximumSize(new Dimension(440, 480));
        card.setPreferredSize(new Dimension(440, 460));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UI.FONT_BOLD);
        tabs.addTab("Customer Login", buildCustomerLoginTab());
        tabs.addTab("Register", buildRegisterTab());
        tabs.addTab("Admin Login", buildAdminLoginTab());
        card.add(tabs, BorderLayout.CENTER);

        center.add(bankTitle);
        center.add(Box.createVerticalStrut(6));
        center.add(tagline);
        center.add(Box.createVerticalStrut(24));
        center.add(card);

        add(center);
    }

    private JPanel buildCustomerLoginTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(24, 24, 24, 24));
        p.setBackground(UI.CARD_BG);
        GridBagConstraints gc = gc();

        JTextField username = UI.textField();
        JPasswordField pin = UI.passwordField();

        addRow(p, gc, "Username", username);
        addRow(p, gc, "PIN", pin);

        JButton login = UI.primaryButton("Log In");
        JLabel status = statusLabel();

        login.addActionListener(e -> {
            status.setText(" ");
            Bank bank = app.bank();
            if (bank == null) { status.setText("No database connection."); return; }
            try {
                Customer c = bank.login(username.getText().trim(), new String(pin.getPassword()));
                app.showCustomerDashboard(c);
            } catch (AuthenticationException ex) {
                status.setText(ex.getMessage());
            }
        });

        gc.gridx = 0; gc.gridy++; gc.gridwidth = 2; gc.insets = new Insets(18, 4, 6, 4);
        p.add(login, gc);
        gc.gridy++;
        p.add(status, gc);
        return p;
    }

    private JPanel buildRegisterTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        p.setBackground(UI.CARD_BG);
        GridBagConstraints gc = gc();

        JTextField username = UI.textField();
        JPasswordField pin = UI.passwordField();
        JTextField fullName = UI.textField();
        JTextField email = UI.textField();
        JTextField phone = UI.textField();

        addRow(p, gc, "Username", username);
        addRow(p, gc, "Choose PIN", pin);
        addRow(p, gc, "Full Name", fullName);
        addRow(p, gc, "Email", email);
        addRow(p, gc, "Phone", phone);

        JButton register = UI.primaryButton("Create Account");
        JLabel status = statusLabel();

        register.addActionListener(e -> {
            status.setText(" ");
            Bank bank = app.bank();
            if (bank == null) { status.setText("No database connection."); return; }
            String u = username.getText().trim();
            if (u.isEmpty() || pin.getPassword().length == 0) {
                status.setText("Username and PIN are required.");
                return;
            }
            if (bank.getCustomer(u) != null) {
                status.setText("That username is already taken.");
                return;
            }
            bank.registerCustomer(u, new String(pin.getPassword()), fullName.getText().trim(),
                    email.getText().trim(), phone.getText().trim());
            app.persist();
            status.setForeground(UI.SUCCESS);
            status.setText("Account created! Switch to Customer Login to sign in.");
        });

        gc.gridx = 0; gc.gridy++; gc.gridwidth = 2; gc.insets = new Insets(14, 4, 6, 4);
        p.add(register, gc);
        gc.gridy++;
        p.add(status, gc);
        return p;
    }

    private JPanel buildAdminLoginTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(24, 24, 24, 24));
        p.setBackground(UI.CARD_BG);
        GridBagConstraints gc = gc();

        JTextField username = UI.textField();
        username.setText("admin");
        JPasswordField password = UI.passwordField();

        addRow(p, gc, "Admin Username", username);
        addRow(p, gc, "Password", password);

        JButton login = UI.primaryButton("Log In as Admin");
        JLabel status = statusLabel();
        JLabel hint = new JLabel("Default: admin / admin123");
        hint.setFont(UI.FONT_SUBTITLE);
        hint.setForeground(UI.MUTED);

        login.addActionListener(e -> {
            status.setText(" ");
            Bank bank = app.bank();
            if (bank == null) { status.setText("No database connection."); return; }
            try {
                Admin a = bank.loginAdmin(username.getText().trim(), new String(password.getPassword()));
                app.showAdminDashboard(a);
            } catch (AuthenticationException ex) {
                status.setText(ex.getMessage());
            }
        });

        gc.gridx = 0; gc.gridy++; gc.gridwidth = 2; gc.insets = new Insets(18, 4, 4, 4);
        p.add(login, gc);
        gc.gridy++; gc.insets = new Insets(4, 4, 4, 4);
        p.add(hint, gc);
        gc.gridy++;
        p.add(status, gc);
        return p;
    }

    private GridBagConstraints gc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(6, 4, 6, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;
        return gc;
    }

    private void addRow(JPanel p, GridBagConstraints gc, String label, JComponent field) {
        gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(UI.FONT_BODY);
        p.add(l, gc);
        gc.gridx = 1; gc.weightx = 1;
        p.add(field, gc);
        gc.gridy++;
    }

    private JLabel statusLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(UI.FONT_SUBTITLE);
        l.setForeground(UI.DANGER);
        return l;
    }
}
