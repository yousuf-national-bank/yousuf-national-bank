package bank.web;

import bank.StaffRole;

final class WebSession {
    enum Role { CUSTOMER, ADMIN }

    final String token;
    final Role role;
    final String username;
    final StaffRole staffRole; // null for customer sessions; TELLER or ADMIN for staff sessions

    WebSession(String token, Role role, String username) {
        this(token, role, username, null);
    }

    WebSession(String token, Role role, String username, StaffRole staffRole) {
        this.token = token;
        this.role = role;
        this.username = username;
        this.staffRole = staffRole;
    }
}
