package bank.web;

import bank.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {

    private static AppState state;
    private static final Map<String, WebSession> sessions = new ConcurrentHashMap<>();
    private static final Path WEBROOT = Paths.get("webroot").toAbsolutePath().normalize();

    interface ApiHandler {
        Map<String, Object> handle(HttpExchange ex, Map<String, String> params, WebSession session) throws Exception;
    }

    enum Auth { NONE, CUSTOMER, ADMIN, ANY }

    static class Route {
        final Auth auth;
        final ApiHandler handler;
        Route(Auth auth, ApiHandler handler) { this.auth = auth; this.handler = handler; }
    }

    private static final Map<String, Route> routes = new HashMap<>();

    public static void main(String[] args) throws Exception {
        state = AppState.start("Yousuf National Bank");
        registerRoutes();

        int port = 8080;
        String envPort = System.getenv("PORT"); // most cloud hosts (Render, Railway, etc.) set this
        if (envPort != null && !envPort.isEmpty()) {
            try { port = Integer.parseInt(envPort.trim()); } catch (NumberFormatException ignored) {}
        }
        // Binding to a plain port number (no specific host) listens on ALL network
        // interfaces, not just localhost — so other computers on the same
        // Wi-Fi/LAN (or the internet, once deployed) can reach this on this port too.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", WebServer::handleApi);
        server.createContext("/", WebServer::handleStatic);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("=================================================");
        System.out.println(" Yousuf National Bank web server is running!");
        System.out.println();
        System.out.println(" On this computer:      http://localhost:" + port);
        for (String ip : localNetworkAddresses()) {
            System.out.println(" On your network (LAN): http://" + ip + ":" + port);
        }
        System.out.println();
        System.out.println(" Other PCs on the same Wi-Fi/network can use the LAN address above.");
        System.out.println(" (If they can't connect, check this machine's firewall allows port " + port + ".)");
        System.out.println();
        System.out.println(" Press Ctrl+C to stop.");
        System.out.println("=================================================");
    }

    /** Every non-loopback IPv4 address this machine has — usually just one, the LAN IP. */
    private static java.util.List<String> localNetworkAddresses() {
        java.util.List<String> result = new ArrayList<>();
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                java.net.NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        result.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    // ------------------------------------------------------------- routing

    private static void registerRoutes() {
        routes.put("POST /api/register", new Route(Auth.NONE, WebServer::handleRegister));
        routes.put("POST /api/login", new Route(Auth.NONE, WebServer::handleLogin));
        routes.put("POST /api/admin-login", new Route(Auth.NONE, WebServer::handleAdminLogin));
        routes.put("POST /api/logout", new Route(Auth.ANY, WebServer::handleLogout));
        routes.put("GET /api/me", new Route(Auth.ANY, WebServer::handleMe));

        routes.put("GET /api/accounts", new Route(Auth.CUSTOMER, WebServer::handleListAccounts));
        routes.put("POST /api/accounts/open", new Route(Auth.CUSTOMER, WebServer::handleOpenAccount));
        routes.put("POST /api/accounts/deposit", new Route(Auth.CUSTOMER, WebServer::handleDeposit));
        routes.put("POST /api/accounts/withdraw", new Route(Auth.CUSTOMER, WebServer::handleWithdraw));
        routes.put("POST /api/accounts/transfer", new Route(Auth.CUSTOMER, WebServer::handleTransfer));
        routes.put("GET /api/transactions", new Route(Auth.CUSTOMER, WebServer::handleTransactions));

        routes.put("GET /api/loans", new Route(Auth.CUSTOMER, WebServer::handleListLoans));
        routes.put("POST /api/loans/apply", new Route(Auth.CUSTOMER, WebServer::handleApplyLoan));
        routes.put("POST /api/loans/repay", new Route(Auth.CUSTOMER, WebServer::handleRepayLoan));

        routes.put("POST /api/profile/update", new Route(Auth.CUSTOMER, WebServer::handleUpdateProfile));
        routes.put("POST /api/profile/change-pin", new Route(Auth.CUSTOMER, WebServer::handleChangePin));

        routes.put("GET /api/admin/customers", new Route(Auth.ADMIN, WebServer::handleAdminCustomers));
        routes.put("GET /api/admin/accounts", new Route(Auth.ADMIN, WebServer::handleAdminAccounts));
        routes.put("GET /api/admin/loans", new Route(Auth.ADMIN, WebServer::handleAdminLoans));
        routes.put("GET /api/admin/summary", new Route(Auth.ADMIN, WebServer::handleAdminSummary));
        routes.put("POST /api/admin/customers/toggle-lock", new Route(Auth.ADMIN, WebServer::handleToggleLock));
        routes.put("POST /api/admin/accounts/toggle-freeze", new Route(Auth.ADMIN, WebServer::handleToggleFreeze));
        routes.put("POST /api/admin/accounts/apply-interest", new Route(Auth.ADMIN, WebServer::handleApplyInterest));
        routes.put("POST /api/admin/loans/approve", new Route(Auth.ADMIN, WebServer::handleApproveLoan));
        routes.put("POST /api/admin/loans/reject", new Route(Auth.ADMIN, WebServer::handleRejectLoan));

        routes.put("GET /api/admin/payroll/employees", new Route(Auth.ADMIN, WebServer::handlePayrollList));
        routes.put("POST /api/admin/payroll/employees/add", new Route(Auth.ADMIN, WebServer::handlePayrollAdd));
        routes.put("POST /api/admin/payroll/employees/remove", new Route(Auth.ADMIN, WebServer::handlePayrollRemove));
        routes.put("POST /api/admin/payroll/employees/toggle", new Route(Auth.ADMIN, WebServer::handlePayrollToggle));
        routes.put("POST /api/admin/payroll/run", new Route(Auth.ADMIN, WebServer::handlePayrollRun));
    }

    private static void handleApi(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        Route route = routes.get(method + " " + path);
        if (route == null) {
            sendJson(ex, 404, Json.map("ok", false, "error", "No such endpoint."));
            return;
        }
        try {
            Map<String, String> params = new HashMap<>(parseQuery(ex.getRequestURI().getRawQuery()));
            if ("POST".equals(method)) params.putAll(parseFormBody(ex));

            WebSession session = getSession(ex);
            if (route.auth == Auth.CUSTOMER && (session == null || session.role != WebSession.Role.CUSTOMER)) {
                sendJson(ex, 401, Json.map("ok", false, "error", "Please log in."));
                return;
            }
            if (route.auth == Auth.ADMIN && (session == null || session.role != WebSession.Role.ADMIN)) {
                sendJson(ex, 401, Json.map("ok", false, "error", "Admin login required."));
                return;
            }

            Map<String, Object> result = route.handler.handle(ex, params, session);
            sendJson(ex, 200, result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendJson(ex, 400, Json.map("ok", false, "error", e.getMessage()));
        } catch (Exception e) {
            sendJson(ex, 500, Json.map("ok", false, "error", e.getMessage() == null ? e.toString() : e.getMessage()));
        }
    }

    // -------------------------------------------------------- auth handlers

    private static Map<String, Object> handleRegister(HttpExchange ex, Map<String, String> p, WebSession s) {
        String username = require(p, "username");
        String pin = require(p, "pin");
        if (state.bank.getCustomer(username) != null) throw new IllegalArgumentException("That username is already taken.");
        state.bank.registerCustomer(username, pin, p.getOrDefault("fullName", ""),
                p.getOrDefault("email", ""), p.getOrDefault("phone", ""));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleLogin(HttpExchange ex, Map<String, String> p, WebSession s) throws AuthenticationException {
        Customer c = state.bank.login(require(p, "username"), require(p, "pin"));
        String token = newToken();
        sessions.put(token, new WebSession(token, WebSession.Role.CUSTOMER, c.getUsername()));
        setSessionCookie(ex, token);
        return Json.map("ok", true, "fullName", c.getFullName());
    }

    private static Map<String, Object> handleAdminLogin(HttpExchange ex, Map<String, String> p, WebSession s) throws AuthenticationException {
        Admin a = state.bank.loginAdmin(require(p, "username"), require(p, "password"));
        String token = newToken();
        sessions.put(token, new WebSession(token, WebSession.Role.ADMIN, a.getUsername()));
        setSessionCookie(ex, token);
        return Json.map("ok", true, "username", a.getUsername());
    }

    private static Map<String, Object> handleLogout(HttpExchange ex, Map<String, String> p, WebSession s) {
        if (s != null) sessions.remove(s.token);
        clearSessionCookie(ex);
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleMe(HttpExchange ex, Map<String, String> p, WebSession s) {
        if (s == null) return Json.map("ok", true, "loggedIn", false);
        if (s.role == WebSession.Role.ADMIN) {
            return Json.map("ok", true, "loggedIn", true, "role", "ADMIN", "username", s.username);
        }
        Customer c = state.bank.getCustomer(s.username);
        return Json.map("ok", true, "loggedIn", true, "role", "CUSTOMER", "username", s.username,
                "fullName", c != null ? c.getFullName() : s.username,
                "email", c != null ? c.getEmail() : "",
                "phone", c != null ? c.getPhone() : "");
    }

    // --------------------------------------------------- customer handlers

    private static Customer currentCustomer(WebSession s) {
        Customer c = state.bank.getCustomer(s.username);
        if (c == null) throw new IllegalStateException("Session user no longer exists.");
        return c;
    }

    private static void requireOwnAccount(Customer c, String accNo) {
        if (!c.getAccountNumbers().contains(accNo)) throw new IllegalArgumentException("That account doesn't belong to you.");
    }

    private static Map<String, Object> handleListAccounts(HttpExchange ex, Map<String, String> p, WebSession s) throws AccountNotFoundException {
        Customer c = currentCustomer(s);
        List<Object> list = new ArrayList<>();
        double total = 0;
        for (String accNo : c.getAccountNumbers()) {
            Account a = state.bank.getAccount(accNo);
            list.add(accountJson(a));
            total += a.getBalance();
        }
        return Json.map("ok", true, "accounts", list, "totalBalance", total);
    }

    private static Map<String, Object> handleOpenAccount(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = currentCustomer(s);
        double amount = parseDouble(require(p, "amount"));
        String type = require(p, "type").toUpperCase();
        Account acc;
        switch (type) {
            case "SAVINGS": acc = state.bank.openSavingsAccount(c.getUsername(), amount); break;
            case "CHECKING": acc = state.bank.openCheckingAccount(c.getUsername(), amount); break;
            case "FIXED":
                int term = (int) parseDouble(p.getOrDefault("term", "12"));
                acc = state.bank.openFixedDeposit(c.getUsername(), amount, term);
                break;
            default: throw new IllegalArgumentException("Unknown account type: " + type);
        }
        persistQuietly();
        return Json.map("ok", true, "account", accountJson(acc));
    }

    private static Map<String, Object> handleDeposit(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException, InvalidAmountException {
        Customer c = currentCustomer(s);
        String accNo = require(p, "accountNumber");
        requireOwnAccount(c, accNo);
        Account acc = state.bank.getAccount(accNo);
        acc.deposit(parseDouble(require(p, "amount")), "Deposit via web");
        persistQuietly();
        return Json.map("ok", true, "newBalance", acc.getBalance());
    }

    private static Map<String, Object> handleWithdraw(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Customer c = currentCustomer(s);
        String accNo = require(p, "accountNumber");
        requireOwnAccount(c, accNo);
        Account acc = state.bank.getAccount(accNo);
        acc.withdraw(parseDouble(require(p, "amount")), "Withdrawal via web");
        persistQuietly();
        return Json.map("ok", true, "newBalance", acc.getBalance());
    }

    private static Map<String, Object> handleTransfer(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Customer c = currentCustomer(s);
        String fromAcc = require(p, "fromAccount");
        requireOwnAccount(c, fromAcc);
        state.bank.transfer(fromAcc, require(p, "toAccount"), parseDouble(require(p, "amount")),
                p.getOrDefault("note", ""));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleTransactions(HttpExchange ex, Map<String, String> p, WebSession s) throws AccountNotFoundException {
        Customer c = currentCustomer(s);
        String accNo = require(p, "accountNumber");
        requireOwnAccount(c, accNo);
        Account acc = state.bank.getAccount(accNo);
        List<Object> list = new ArrayList<>();
        for (Transaction t : acc.getTransactions()) list.add(transactionJson(t));
        return Json.map("ok", true, "transactions", list);
    }

    private static Map<String, Object> handleListLoans(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = currentCustomer(s);
        List<Object> list = new ArrayList<>();
        for (String id : c.getLoanIds()) {
            Loan l = state.bank.getLoan(id);
            if (l != null) list.add(loanJson(l));
        }
        return Json.map("ok", true, "loans", list);
    }

    private static Map<String, Object> handleApplyLoan(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = currentCustomer(s);
        String accNo = require(p, "accountNumber");
        requireOwnAccount(c, accNo);
        double amount = parseDouble(require(p, "amount"));
        int term = (int) parseDouble(require(p, "term"));
        Loan loan = state.bank.applyForLoan(c.getUsername(), accNo, amount, term);
        persistQuietly();
        return Json.map("ok", true, "loan", loanJson(loan));
    }

    private static Map<String, Object> handleRepayLoan(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Customer c = currentCustomer(s);
        String loanId = require(p, "loanId");
        Loan loan = state.bank.getLoan(loanId);
        if (loan == null || !loan.getCustomerUsername().equals(c.getUsername()))
            throw new IllegalArgumentException("That loan doesn't belong to you.");
        String accNo = require(p, "accountNumber");
        requireOwnAccount(c, accNo);
        state.bank.repayLoan(loanId, accNo, parseDouble(require(p, "amount")));
        persistQuietly();
        return Json.map("ok", true, "loan", loanJson(loan));
    }

    private static Map<String, Object> handleUpdateProfile(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = currentCustomer(s);
        if (p.containsKey("fullName")) c.setFullName(p.get("fullName"));
        if (p.containsKey("email")) c.setEmail(p.get("email"));
        if (p.containsKey("phone")) c.setPhone(p.get("phone"));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleChangePin(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = currentCustomer(s);
        String newPin = require(p, "newPin");
        c.changePin(newPin);
        persistQuietly();
        return Json.map("ok", true);
    }

    // ------------------------------------------------------- admin handlers

    private static Map<String, Object> handleAdminCustomers(HttpExchange ex, Map<String, String> p, WebSession s) {
        List<Object> list = new ArrayList<>();
        for (Customer c : state.bank.getAllCustomers().values()) list.add(customerJson(c));
        return Json.map("ok", true, "customers", list);
    }

    private static Map<String, Object> handleAdminAccounts(HttpExchange ex, Map<String, String> p, WebSession s) {
        List<Object> list = new ArrayList<>();
        for (Account a : state.bank.getAllAccounts().values()) list.add(accountJson(a));
        return Json.map("ok", true, "accounts", list);
    }

    private static Map<String, Object> handleAdminLoans(HttpExchange ex, Map<String, String> p, WebSession s) {
        List<Object> list = new ArrayList<>();
        for (Loan l : state.bank.getAllLoans().values()) list.add(loanJson(l));
        return Json.map("ok", true, "loans", list);
    }

    private static Map<String, Object> handleAdminSummary(HttpExchange ex, Map<String, String> p, WebSession s) {
        return Json.map("ok", true,
                "totalCustomers", state.bank.getAllCustomers().size(),
                "totalAccounts", state.bank.getAllAccounts().size(),
                "totalDeposits", state.bank.getTotalDeposits(),
                "totalLoans", state.bank.getAllLoans().size(),
                "totalOutstandingLoans", state.bank.getTotalOutstandingLoans());
    }

    private static Map<String, Object> handleToggleLock(HttpExchange ex, Map<String, String> p, WebSession s) {
        Customer c = state.bank.getCustomer(require(p, "username"));
        if (c == null) throw new IllegalArgumentException("No such customer.");
        c.setLocked(!c.isLocked());
        persistQuietly();
        return Json.map("ok", true, "locked", c.isLocked());
    }

    private static Map<String, Object> handleToggleFreeze(HttpExchange ex, Map<String, String> p, WebSession s) throws AccountNotFoundException {
        Account a = state.bank.getAccount(require(p, "accountNumber"));
        a.setFrozen(!a.isFrozen());
        persistQuietly();
        return Json.map("ok", true, "frozen", a.isFrozen());
    }

    private static Map<String, Object> handleApplyInterest(HttpExchange ex, Map<String, String> p, WebSession s) {
        state.bank.applyInterestToAll();
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleApproveLoan(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException, InvalidAmountException {
        state.bank.approveLoan(require(p, "loanId"));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handleRejectLoan(HttpExchange ex, Map<String, String> p, WebSession s) {
        state.bank.rejectLoan(require(p, "loanId"));
        persistQuietly();
        return Json.map("ok", true);
    }

    // ------------------------------------------------------ payroll handlers

    private static Map<String, Object> handlePayrollList(HttpExchange ex, Map<String, String> p, WebSession s) {
        List<Object> list = new ArrayList<>();
        for (Employee e : state.bank.getAllEmployees().values()) list.add(employeeJson(e));
        return Json.map("ok", true, "employees", list);
    }

    private static Map<String, Object> handlePayrollAdd(HttpExchange ex, Map<String, String> p, WebSession s)
            throws AccountNotFoundException {
        Employee emp = state.bank.addEmployee(
                require(p, "accountNumber"),
                require(p, "employerName"),
                require(p, "position"),
                parseDouble(require(p, "monthlySalary")));
        persistQuietly();
        return Json.map("ok", true, "employee", employeeJson(emp));
    }

    private static Map<String, Object> handlePayrollRemove(HttpExchange ex, Map<String, String> p, WebSession s) {
        state.bank.removeEmployee(require(p, "employeeId"));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handlePayrollToggle(HttpExchange ex, Map<String, String> p, WebSession s) {
        state.bank.toggleEmployeeActive(require(p, "employeeId"));
        persistQuietly();
        return Json.map("ok", true);
    }

    private static Map<String, Object> handlePayrollRun(HttpExchange ex, Map<String, String> p, WebSession s) {
        List<Payslip> results = state.bank.runPayroll();
        persistQuietly();
        List<Object> list = new ArrayList<>();
        for (Payslip ps : results) list.add(payslipJson(ps));
        return Json.map("ok", true, "count", results.size(), "payslips", list);
    }

    // ------------------------------------------------------------- helpers

    private static Map<String, Object> accountJson(Account a) {
        Map<String, Object> m = Json.map(
                "accountNumber", a.getAccountNumber(),
                "type", a.getAccountType(),
                "owner", a.getOwnerUsername(),
                "balance", a.getBalance(),
                "frozen", a.isFrozen(),
                "openedOn", a.getOpenedOn().toString());
        if (a instanceof FixedDepositAccount) {
            m.put("maturityDate", ((FixedDepositAccount) a).getMaturityDate().toString());
        }
        return m;
    }

    private static Map<String, Object> transactionJson(Transaction t) {
        return Json.map(
                "id", t.getTransactionId(),
                "type", t.getType().name(),
                "amount", t.getAmount(),
                "balanceAfter", t.getBalanceAfter(),
                "timestamp", t.getTimestamp().toString(),
                "description", t.getDescription());
    }

    private static Map<String, Object> loanJson(Loan l) {
        return Json.map(
                "loanId", l.getLoanId(),
                "customer", l.getCustomerUsername(),
                "principal", l.getPrincipal(),
                "termMonths", l.getTermMonths(),
                "outstandingBalance", l.getOutstandingBalance(),
                "status", l.getStatus().name());
    }

    private static Map<String, Object> customerJson(Customer c) {
        return Json.map(
                "username", c.getUsername(),
                "fullName", c.getFullName(),
                "email", c.getEmail(),
                "phone", c.getPhone(),
                "locked", c.isLocked());
    }

    private static Map<String, Object> employeeJson(Employee e) {
        Customer c = state.bank.getCustomer(e.getCustomerUsername());
        return Json.map(
                "employeeId", e.getEmployeeId(),
                "accountNumber", e.getAccountNumber(),
                "customerUsername", e.getCustomerUsername(),
                "employeeName", c != null ? c.getFullName() : e.getCustomerUsername(),
                "employerName", e.getEmployerName(),
                "position", e.getPosition(),
                "monthlySalary", e.getMonthlySalary(),
                "active", e.isActive(),
                "lastPaidOn", e.getLastPaidOn() == null ? "" : e.getLastPaidOn().toString());
    }

    private static Map<String, Object> payslipJson(Payslip ps) {
        return Json.map(
                "voucherId", ps.getVoucherId(),
                "employeeId", ps.getEmployeeId(),
                "employeeName", ps.getEmployeeName(),
                "accountNumber", ps.getAccountNumber(),
                "employerName", ps.getEmployerName(),
                "position", ps.getPosition(),
                "amount", ps.getAmount(),
                "newBalance", ps.getNewBalance(),
                "payDate", ps.getPayDate().toString());
    }

    private static void persistQuietly() {
        try {
            state.persist();
        } catch (Exception e) {
            System.err.println("Warning: failed to save to database: " + e.getMessage());
        }
    }

    private static String require(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("Missing required field: " + key);
        return v.trim();
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + s + "\" is not a valid number.");
        }
    }

    private static String newToken() {
        return java.util.UUID.randomUUID().toString();
    }

    private static WebSession getSession(HttpExchange ex) {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) return null;
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("SID")) return sessions.get(kv[1]);
        }
        return null;
    }

    private static void setSessionCookie(HttpExchange ex, String token) {
        ex.getResponseHeaders().add("Set-Cookie", "SID=" + token + "; Path=/; HttpOnly; SameSite=Lax");
    }

    private static void clearSessionCookie(HttpExchange ex) {
        ex.getResponseHeaders().add("Set-Cookie", "SID=; Path=/; Max-Age=0");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            try {
                String k = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                String v = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                map.put(k, v);
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private static Map<String, String> parseFormBody(HttpExchange ex) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        InputStream is = ex.getRequestBody();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = is.read(chunk)) != -1) buffer.write(chunk, 0, read);
        String body = buffer.toString("UTF-8");
        return parseQuery(body);
    }

    private static void sendJson(HttpExchange ex, int status, Map<String, Object> data) throws IOException {
        byte[] bytes = Json.write(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // -------------------------------------------------------- static files

    private static void handleStatic(HttpExchange ex) throws IOException {
        String reqPath = ex.getRequestURI().getPath();
        if (reqPath.equals("/")) reqPath = "/index.html";

        Path file = WEBROOT.resolve("." + reqPath).normalize();
        if (!file.startsWith(WEBROOT) || !Files.exists(file) || Files.isDirectory(file)) {
            file = WEBROOT.resolve("index.html"); // SPA fallback
        }

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().add("Content-Type", contentType(file.toString()));
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".webmanifest") || path.endsWith(".json")) return "application/manifest+json";
        if (path.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (path.endsWith(".xml")) return "application/xml; charset=utf-8";
        return "application/octet-stream";
    }
}
