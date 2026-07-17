package util;

import java.util.regex.Pattern;

import org.mindrot.BCrypt;

/**
 * ✅ Utilitaire centralisant :
 *   - le hachage / la vérification des mots de passe avec BCrypt
 *   - la politique de mot de passe fort à l'inscription
 */
public class PasswordUtil {

    // Au moins 8 caractères, 1 majuscule, 1 minuscule, 1 chiffre, 1 caractère spécial
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$"
    );

    private PasswordUtil() {}

    public static boolean isStrong(String password) {
        return password != null && STRONG_PASSWORD.matcher(password).matches();
    }

    public static String getPolicyMessage() {
        return "Le mot de passe doit contenir au moins 8 caractères, "
             + "une majuscule, une minuscule, un chiffre et un caractère spécial.";
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Le hash stocké n'est pas un hash BCrypt valide (ex: ancien mot de passe en clair)
            return false;
        }
    }
}
