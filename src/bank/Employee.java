package bank;

import java.io.Serializable;
import java.time.LocalDate;

/** A payroll record linking a customer's account to an employer and a recurring salary. */
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String employeeId;
    private final String accountNumber;
    private final String customerUsername;
    private String employerName;
    private String position;
    private double monthlySalary;
    private boolean active;
    private final LocalDate addedOn;
    private LocalDate lastPaidOn; // null until the first payroll run includes them

    public Employee(String employeeId, String accountNumber, String customerUsername,
                     String employerName, String position, double monthlySalary) {
        this.employeeId = employeeId;
        this.accountNumber = accountNumber;
        this.customerUsername = customerUsername;
        this.employerName = employerName;
        this.position = position;
        this.monthlySalary = monthlySalary;
        this.active = true;
        this.addedOn = LocalDate.now();
    }

    public String getEmployeeId() { return employeeId; }
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerUsername() { return customerUsername; }
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
        return String.format("%-8s | %-10s | Employer: %-15s | Position: %-12s | Salary: %10.2f | %s",
                employeeId, accountNumber, employerName, position, monthlySalary, active ? "ACTIVE" : "INACTIVE");
    }
}
