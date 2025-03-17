import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    // Hash a password
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12)); // 12 rounds for security
    }

    // Verify password
    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
