package bank;

import java.io.Serializable;
import java.time.LocalDate;

public class Loan implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final double ANNUAL_INTEREST_RATE = 0.10; // 10% simple interest per year

    private final String loanId;
    private final String customerUsername;
    private final String linkedAccountNumber;
    private final double principal;
    private final int termMonths;
    private double outstandingBalance;
    private LoanStatus status;
    private final LocalDate appliedOn;

    public Loan(String loanId, String customerUsername, String linkedAccountNumber, double principal, int termMonths) {
        this.loanId = loanId;
        this.customerUsername = customerUsername;
        this.linkedAccountNumber = linkedAccountNumber;
        this.principal = principal;
        this.termMonths = termMonths;
        this.status = LoanStatus.PENDING;
        this.appliedOn = LocalDate.now();
        double interest = principal * ANNUAL_INTEREST_RATE * (termMonths / 12.0);
        this.outstandingBalance = principal + interest;
    }

    /** Rehydration constructor — preserves exact stored state instead of recalculating it. */
    public Loan(String loanId, String customerUsername, String linkedAccountNumber, double principal,
                int termMonths, double outstandingBalance, LoanStatus status, LocalDate appliedOn) {
        this.loanId = loanId;
        this.customerUsername = customerUsername;
        this.linkedAccountNumber = linkedAccountNumber;
        this.principal = principal;
        this.termMonths = termMonths;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.appliedOn = appliedOn;
    }

    public void approve() { this.status = LoanStatus.APPROVED; }
    public void reject() { this.status = LoanStatus.REJECTED; }

    public void repay(double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Repayment amount must be positive.");
        outstandingBalance -= amount;
        if (outstandingBalance <= 0) {
            outstandingBalance = 0;
            status = LoanStatus.PAID_OFF;
        }
    }

    public String getLoanId() { return loanId; }
    public String getCustomerUsername() { return customerUsername; }
    public String getLinkedAccountNumber() { return linkedAccountNumber; }
    public double getPrincipal() { return principal; }
    public int getTermMonths() { return termMonths; }
    public double getOutstandingBalance() { return outstandingBalance; }
    public LoanStatus getStatus() { return status; }
    public LocalDate getAppliedOn() { return appliedOn; }

    @Override
    public String toString() {
        return String.format("%-10s | Customer: %-10s | Principal: %10.2f | Term: %2d mo | Owed: %10.2f | %s",
                loanId, customerUsername, principal, termMonths, outstandingBalance, status);
    }
}
