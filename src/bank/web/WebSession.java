package bank.web;

final class WebSession {
    enum Role { CUSTOMER, ADMIN }

    final String token;
    final Role role;
    final String username;

    WebSession(String token, Role role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }
}
