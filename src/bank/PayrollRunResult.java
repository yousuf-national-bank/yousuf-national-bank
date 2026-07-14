package bank;

import java.util.ArrayList;
import java.util.List;

/** Everything that happened during one payroll run — successes and skipped/failed entries with reasons. */
public class PayrollRunResult {
    public static class Skipped {
        public final String employeeId;
        public final String employeeName;
        public final String reason;
        public Skipped(String employeeId, String employeeName, String reason) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.reason = reason;
        }
    }

    private final List<Payslip> paid = new ArrayList<>();
    private final List<Skipped> skipped = new ArrayList<>();

    public void addPaid(Payslip p) { paid.add(p); }
    public void addSkipped(String employeeId, String employeeName, String reason) {
        skipped.add(new Skipped(employeeId, employeeName, reason));
    }

    public List<Payslip> getPaid() { return paid; }
    public List<Skipped> getSkipped() { return skipped; }
}
