package bank;

import java.util.Map;
import java.util.Scanner;

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static Bank bank;

    public static void main(String[] args) {
        Bank loaded = FileStorage.load();
        bank = (loaded != null) ? loaded : new Bank("Yousuf National Bank");
        System.out.println("=================================================");
        System.out.println("   Welcome to " + bank.getBankName());
        System.out.println("=================================================");
        if (loaded == null) {
            System.out.println("(No previous data found — starting a fresh bank. Default admin login: admin / admin123)");
        }

        boolean running = true;
        while (running) {
            System.out.println("\n1) Customer Login\n2) Register New Customer\n3) Admin Login\n4) Exit");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": customerLogin(); break;
                case "2": registerCustomer(); break;
                case "3": adminLogin(); break;
                case "4":
                    FileStorage.save(bank);
                    System.out.println("Data saved. Goodbye!");
                    running = false;
                    break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    // ---------------- Registration / Login ----------------

    private static void registerCustomer() {
        System.out.print("Choose a username: ");
        String username = sc.nextLine().trim();
        if (bank.getCustomer(username) != null) {
            System.out.println("That username is already taken.");
            return;
        }
        System.out.print("Choose a 4-6 digit PIN: ");
        String pin = sc.nextLine().trim();
        System.out.print("Full name: ");
        String name = sc.nextLine().trim();
        System.out.print("Email: ");
        String email = sc.nextLine().trim();
        System.out.print("Phone: ");
        String phone = sc.nextLine().trim();

        bank.registerCustomer(username, pin, name, email, phone);
        System.out.println("Account registered! You can now log in and open a bank account.");
        FileStorage.save(bank);
    }

    private static void customerLogin() {
        System.out.print("Username: ");
        String username = sc.nextLine().trim();
        System.out.print("PIN: ");
        String pin = sc.nextLine().trim();
        try {
            Customer c = bank.login(username, pin);
            customerMenu(c);
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void adminLogin() {
        System.out.print("Admin username: ");
        String username = sc.nextLine().trim();
        System.out.print("Password: ");
        String password = sc.nextLine().trim();
        try {
            Admin a = bank.loginAdmin(username, password);
            adminMenu(a);
        } catch (AuthenticationException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    // ---------------- Customer menu ----------------

    private static void customerMenu(Customer c) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Welcome, " + c.getFullName() + " ---");
            System.out.println("1) Open Account (Savings/Checking/Fixed Deposit)");
            System.out.println("2) View My Accounts");
            System.out.println("3) Deposit");
            System.out.println("4) Withdraw");
            System.out.println("5) Transfer");
            System.out.println("6) Transaction History");
            System.out.println("7) Apply for Loan");
            System.out.println("8) View My Loans / Repay Loan");
            System.out.println("9) Update Profile");
            System.out.println("10) Change PIN");
            System.out.println("11) Log Out");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": openAccount(c); break;
                    case "2": viewAccounts(c); break;
                    case "3": doDeposit(c); break;
                    case "4": doWithdraw(c); break;
                    case "5": doTransfer(c); break;
                    case "6": viewHistory(c); break;
                    case "7": applyLoan(c); break;
                    case "8": viewLoans(c); break;
                    case "9": updateProfile(c); break;
                    case "10": changePin(c); break;
                    case "11":
                        FileStorage.save(bank);
                        inMenu = false;
                        break;
                    default: System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void openAccount(Customer c) {
        System.out.println("Account type: 1) Savings  2) Checking  3) Fixed Deposit");
        String type = sc.nextLine().trim();
        System.out.print("Opening deposit amount: ");
        double amt = readDouble();
        Account acc;
        switch (type) {
            case "1": acc = bank.openSavingsAccount(c.getUsername(), amt); break;
            case "2": acc = bank.openCheckingAccount(c.getUsername(), amt); break;
            case "3":
                System.out.print("Term in months (e.g. 6, 12, 24): ");
                int term = readInt();
                acc = bank.openFixedDeposit(c.getUsername(), amt, term);
                break;
            default:
                System.out.println("Invalid type.");
                return;
        }
        System.out.println("Account opened: " + acc);
        FileStorage.save(bank);
    }

    private static void viewAccounts(Customer c) throws AccountNotFoundException {
        System.out.println("--- Your Accounts ---");
        for (String accNo : c.getAccountNumbers()) {
            System.out.println(bank.getAccount(accNo));
        }
    }

    private static void doDeposit(Customer c) throws Exception {
        String accNo = pickOwnAccount(c);
        if (accNo == null) return;
        System.out.print("Amount to deposit: ");
        double amt = readDouble();
        Account acc = bank.getAccount(accNo);
        acc.deposit(amt, "Customer deposit");
        System.out.println("New balance: " + acc.getBalance());
        FileStorage.save(bank);
    }

    private static void doWithdraw(Customer c) throws Exception {
        String accNo = pickOwnAccount(c);
        if (accNo == null) return;
        System.out.print("Amount to withdraw: ");
        double amt = readDouble();
        Account acc = bank.getAccount(accNo);
        acc.withdraw(amt, "Customer withdrawal");
        System.out.println("New balance: " + acc.getBalance());
        FileStorage.save(bank);
    }

    private static void doTransfer(Customer c) throws Exception {
        String fromAcc = pickOwnAccount(c);
        if (fromAcc == null) return;
        System.out.print("Destination account number: ");
        String toAcc = sc.nextLine().trim();
        System.out.print("Amount: ");
        double amt = readDouble();
        System.out.print("Note (optional): ");
        String note = sc.nextLine().trim();
        bank.transfer(fromAcc, toAcc, amt, note);
        System.out.println("Transfer complete.");
        FileStorage.save(bank);
    }

    private static void viewHistory(Customer c) throws AccountNotFoundException {
        String accNo = pickOwnAccount(c);
        if (accNo == null) return;
        Account acc = bank.getAccount(accNo);
        System.out.println("--- Transaction History for " + accNo + " ---");
        if (acc.getTransactions().isEmpty()) {
            System.out.println("No transactions yet.");
        }
        for (Transaction t : acc.getTransactions()) {
            System.out.println(t);
        }
    }

    private static void applyLoan(Customer c) {
        String accNo = pickOwnAccount(c);
        if (accNo == null) return;
        System.out.print("Loan amount requested: ");
        double amt = readDouble();
        System.out.print("Term in months: ");
        int term = readInt();
        Loan loan = bank.applyForLoan(c.getUsername(), accNo, amt, term);
        System.out.println("Loan application submitted: " + loan);
        System.out.println("An admin must approve it before funds are disbursed.");
        FileStorage.save(bank);
    }

    private static void viewLoans(Customer c) throws Exception {
        System.out.println("--- Your Loans ---");
        if (c.getLoanIds().isEmpty()) {
            System.out.println("You have no loans.");
            return;
        }
        for (String id : c.getLoanIds()) {
            System.out.println(bank.getLoan(id));
        }
        System.out.print("Repay a loan? Enter loan ID or blank to skip: ");
        String id = sc.nextLine().trim();
        if (id.isEmpty()) return;
        Loan loan = bank.getLoan(id);
        if (loan == null || !loan.getCustomerUsername().equals(c.getUsername())) {
            System.out.println("That loan doesn't belong to you.");
            return;
        }
        if (loan.getStatus() != LoanStatus.APPROVED) {
            System.out.println("This loan isn't in an active/approved state.");
            return;
        }
        System.out.print("Repay from which account? ");
        String accNo = sc.nextLine().trim();
        System.out.print("Amount to repay: ");
        double amt = readDouble();
        bank.repayLoan(id, accNo, amt);
        System.out.println("Repayment successful. Remaining balance: " + loan.getOutstandingBalance());
        FileStorage.save(bank);
    }

    private static void updateProfile(Customer c) {
        System.out.print("New full name (blank to keep \"" + c.getFullName() + "\"): ");
        String name = sc.nextLine().trim();
        if (!name.isEmpty()) c.setFullName(name);
        System.out.print("New email (blank to keep \"" + c.getEmail() + "\"): ");
        String email = sc.nextLine().trim();
        if (!email.isEmpty()) c.setEmail(email);
        System.out.print("New phone (blank to keep \"" + c.getPhone() + "\"): ");
        String phone = sc.nextLine().trim();
        if (!phone.isEmpty()) c.setPhone(phone);
        System.out.println("Profile updated.");
        FileStorage.save(bank);
    }

    private static void changePin(Customer c) {
        System.out.print("New PIN: ");
        String pin = sc.nextLine().trim();
        c.changePin(pin);
        System.out.println("PIN changed.");
        FileStorage.save(bank);
    }

    private static String pickOwnAccount(Customer c) {
        if (c.getAccountNumbers().isEmpty()) {
            System.out.println("You don't have any accounts yet. Open one first.");
            return null;
        }
        System.out.println("Your accounts: " + c.getAccountNumbers());
        System.out.print("Enter account number: ");
        String accNo = sc.nextLine().trim();
        if (!c.getAccountNumbers().contains(accNo)) {
            System.out.println("That account doesn't belong to you.");
            return null;
        }
        return accNo;
    }

    // ---------------- Admin menu ----------------

    private static void adminMenu(Admin a) {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println("\n--- Admin Panel (" + a.getUsername() + ") ---");
            System.out.println("1) View All Customers");
            System.out.println("2) View All Accounts");
            System.out.println("3) Freeze / Unfreeze Account");
            System.out.println("4) Lock / Unlock Customer");
            System.out.println("5) View Pending Loans");
            System.out.println("6) Approve Loan");
            System.out.println("7) Reject Loan");
            System.out.println("8) Apply Interest to All Accounts");
            System.out.println("9) Bank Summary Report");
            System.out.println("10) Log Out");
            System.out.print("Choose an option: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": listCustomers(); break;
                    case "2": listAccounts(); break;
                    case "3": toggleFreeze(); break;
                    case "4": toggleLock(); break;
                    case "5": listPendingLoans(); break;
                    case "6": approveLoan(); break;
                    case "7": rejectLoan(); break;
                    case "8":
                        bank.applyInterestToAll();
                        System.out.println("Interest applied to all eligible accounts.");
                        FileStorage.save(bank);
                        break;
                    case "9": bankSummary(); break;
                    case "10":
                        FileStorage.save(bank);
                        inMenu = false;
                        break;
                    default: System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void listCustomers() {
        System.out.println("--- All Customers ---");
        for (Customer c : bank.getAllCustomers().values()) {
            System.out.println(c);
        }
    }

    private static void listAccounts() {
        System.out.println("--- All Accounts ---");
        for (Account acc : bank.getAllAccounts().values()) {
            System.out.println(acc);
        }
    }

    private static void toggleFreeze() throws AccountNotFoundException {
        System.out.print("Account number: ");
        String accNo = sc.nextLine().trim();
        Account acc = bank.getAccount(accNo);
        acc.setFrozen(!acc.isFrozen());
        System.out.println("Account " + accNo + " is now " + (acc.isFrozen() ? "FROZEN" : "ACTIVE"));
        FileStorage.save(bank);
    }

    private static void toggleLock() {
        System.out.print("Customer username: ");
        String username = sc.nextLine().trim();
        Customer c = bank.getCustomer(username);
        if (c == null) {
            System.out.println("No such customer.");
            return;
        }
        c.setLocked(!c.isLocked());
        System.out.println("Customer " + username + " is now " + (c.isLocked() ? "LOCKED" : "ACTIVE"));
        FileStorage.save(bank);
    }

    private static void listPendingLoans() {
        System.out.println("--- Pending Loans ---");
        boolean any = false;
        for (Loan l : bank.getAllLoans().values()) {
            if (l.getStatus() == LoanStatus.PENDING) {
                System.out.println(l);
                any = true;
            }
        }
        if (!any) System.out.println("No pending loans.");
    }

    private static void approveLoan() throws AccountNotFoundException, InvalidAmountException {
        System.out.print("Loan ID to approve: ");
        String id = sc.nextLine().trim();
        bank.approveLoan(id);
        System.out.println("Loan " + id + " approved and disbursed.");
        FileStorage.save(bank);
    }

    private static void rejectLoan() {
        System.out.print("Loan ID to reject: ");
        String id = sc.nextLine().trim();
        bank.rejectLoan(id);
        System.out.println("Loan " + id + " rejected.");
        FileStorage.save(bank);
    }

    private static void bankSummary() {
        System.out.println("--- Bank Summary: " + bank.getBankName() + " ---");
        System.out.println("Total customers: " + bank.getAllCustomers().size());
        System.out.println("Total accounts:  " + bank.getAllAccounts().size());
        System.out.printf("Total deposits:  %.2f%n", bank.getTotalDeposits());
        System.out.println("Total loans issued: " + bank.getAllLoans().size());
        System.out.printf("Total outstanding loan balance: %.2f%n", bank.getTotalOutstandingLoans());
    }

    // ---------------- Input helpers ----------------

    private static double readDouble() {
        while (true) {
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid number: ");
            }
        }
    }

    private static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Please enter a valid whole number: ");
            }
        }
    }
}
