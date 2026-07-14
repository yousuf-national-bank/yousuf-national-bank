package bank;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A payroll record linking a customer's account (who gets paid) to an employer's
 * own funded account (who pays) and a recurring salary amount. Running payroll
 * moves real money out of the employer's account into the employee's account —
 * it does not create money from nothing.
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String employeeId;
    private final String accountNumber;          // where the salary is credited
    private final String customerUsername;        // the employee, as a bank customer
    private final String employerAccountNumber;   // where the salary is debited from
    private String employerName;                  // display name, derived from the employer account's owner
    private String position;
    private double monthlySalary;
    private boolean active;
    private final LocalDate addedOn;
    private LocalDate lastPaidOn;

    public Employee(String employeeId, String accountNumber, String customerUsername,
                     String employerAccountNumber, String employerName, String position, double monthlySalary) {
        this.employeeId = employeeId;
        this.accountNumber = accountNumber;
        this.customerUsername = customerUsername;
        this.employerAccountNumber = employerAccountNumber;
        this.employerName = employerName;
        this.position = position;
        this.monthlySalary = monthlySalary;
        this.active = true;
        this.addedOn = LocalDate.now();
    }

    public String getEmployeeId() { return employeeId; }
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerUsername() { return customerUsername; }
    public String getEmployerAccountNumber() { return employerAccountNumber; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getAddedOn() { return addedOn; }
    public LocalDate getLastPaidOn() { return lastPaidOn; }
    public void markPaid(LocalDate date) { this.lastPaidOn = date; }

    @Override
    public String toString() {
        return String.format("%-8s | Pays: %-10s | From: %-10s | Position: %-12s | Salary: %10.2f | %s",
                employeeId, accountNumber, employerAccountNumber, position, monthlySalary, active ? "ACTIVE" : "INACTIVE");
    }
}
