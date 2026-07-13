package bank.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Central place for colors, fonts and small reusable widget builders so every screen looks consistent. */
final class UI {
    static final Color NAVY = new Color(0x0B2545);
    static final Color NAVY_LIGHT = new Color(0x13315C);
    static final Color ACCENT = new Color(0x2EC4B6);
    static final Color ACCENT_DARK = new Color(0x1B9C90);
    static final Color BG = new Color(0xF4F6F8);
    static final Color CARD_BG = Color.WHITE;
    static final Color TEXT = new Color(0x1C1C1E);
    static final Color MUTED = new Color(0x6B7280);
    static final Color DANGER = new Color(0xD64545);
    static final Color WARNING = new Color(0xE0A11D);
    static final Color SUCCESS = new Color(0x2E9E5B);

    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 16);
    static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    static final Font FONT_NUMBER = new Font("Segoe UI", Font.BOLD, 26);

    private UI() {}

    static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, ACCENT, Color.WHITE);
        return b;
    }

    static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, new Color(0xE5E9EF), TEXT);
        return b;
    }

    static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        styleButton(b, DANGER, Color.WHITE);
        return b;
    }

    private static void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorderPainted(false);
    }

    static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT);
        return l;
    }

    static JLabel subtitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SUBTITLE);
        l.setForeground(MUTED);
        return l;
    }

    static JLabel header(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADER);
        l.setForeground(TEXT);
        return l;
    }

    static JTextField textField() {
        JTextField f = new JTextField();
        stylize(f);
        return f;
    }

    static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        stylize(f);
        return f;
    }

    private static void stylize(JTextField f) {
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD5DAE1), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        f.setPreferredSize(new Dimension(220, 36));
    }

    /** A rounded white "card" panel used throughout the dashboards. */
    static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));
        return p;
    }

    static void styleTable(JTable t) {
        t.setFont(FONT_BODY);
        t.setRowHeight(28);
        t.setForeground(TEXT);
        t.setGridColor(new Color(0xE5E9EF));
        t.setSelectionBackground(new Color(0xDCEFEC));
        t.setSelectionForeground(TEXT);
        t.getTableHeader().setFont(FONT_BOLD);
        t.getTableHeader().setBackground(new Color(0xEFF2F5));
        t.getTableHeader().setForeground(TEXT);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
    }

    static void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void showInfo(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
