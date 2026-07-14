package bank;

import java.time.LocalDate;

/** A read-only snapshot of one salary payment, handed back after a payroll run so the caller can display/print it. */
public class Payslip {
    private final String voucherId;
    private final String employeeId;
    private final String employeeName;
    private final String accountNumber;
    private final String employerName;
    private final String position;
    private final double amount;
    private final double newBalance;
    private final LocalDate payDate;

    public Payslip(String voucherId, String employeeId, String employeeName, String accountNumber,
                    String employerName, String position, double amount, double newBalance, LocalDate payDate) {
        this.voucherId = voucherId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.accountNumber = accountNumber;
        this.employerName = employerName;
        this.position = position;
        this.amount = amount;
        this.newBalance = newBalance;
        this.payDate = payDate;
    }

    public String getVoucherId() { return voucherId; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getAccountNumber() { return accountNumber; }
    public String getEmployerName() { return employerName; }
    public String getPosition() { return position; }
    public double getAmount() { return amount; }
    public double getNewBalance() { return newBalance; }
    public LocalDate getPayDate() { return payDate; }
}
