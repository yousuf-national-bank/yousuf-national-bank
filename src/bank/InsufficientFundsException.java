package bank;

/** Thrown when a withdrawal/transfer exceeds available balance (incl. overdraft rules). */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
