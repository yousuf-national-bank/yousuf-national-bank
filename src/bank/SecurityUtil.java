package bank;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Simple one-way hashing so PINs/passwords are never stored in plain text. */
public final class SecurityUtil {
    private SecurityUtil() {}

    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
