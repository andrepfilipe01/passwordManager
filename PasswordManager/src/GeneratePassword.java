import java.security.SecureRandom;

public class GeneratePassword{

    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final int DEFAULT_LENGTH = 12;

    public static void main(String[] args) {
        System.out.println(generatePassword(12));
    }

    public static String generatePassword(int length) {
    
        if(length <= 5) {
            length = DEFAULT_LENGTH;
        }

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();  

        for(int i = 0; i < length; i++){
            int index = random.nextInt(CHAR_POOL.length());
            password.append(CHAR_POOL.charAt(index));
        } 
        return password.toString();
    }

}