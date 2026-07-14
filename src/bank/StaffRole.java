package bank;

/**
 * TELLER — day-to-day front-desk operations: view customers/accounts, lock/unlock
 *          customers, freeze/unfreeze accounts.
 * ADMIN  — everything a teller can do, plus financial/managerial decisions: applying
 *          interest, approving/rejecting loans, running payroll, and managing staff logins.
 */
public enum StaffRole {
    TELLER,
    ADMIN
}
