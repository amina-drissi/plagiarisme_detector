package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * ✅ Centralise la configuration de chemins du projet afin de supprimer
 * tous les chemins absolus codés en dur (ex: "D:/Dev/detecteur_de_plagiarisme/...")
 * et de rendre le projet exécutable sur n'importe quelle machine.
 *
 * Résolution de la racine du projet (dans l'ordre de priorité) :
 *   1. Variable d'environnement / propriété système "PLAGIARISM_HOME"
 *   2. Déduction automatique à partir de l'emplacement des classes compilées
 *      (structure Eclipse Dynamic Web Project : build/classes est à la racine du projet,
 *      au même niveau que "uploads", "python" et "results")
 *
 * Un fichier "config.properties" optionnel (non versionné, voir .gitignore) placé
 * à la racine du projet permet de surcharger les valeurs par défaut si besoin.
 */
public class AppConfig {

    private static final String ENV_HOME = "PLAGIARISM_HOME";

    private static final File projectRoot;
    private static final Properties props = new Properties();

    static {
        String override = System.getProperty(ENV_HOME);
        if (override == null || override.isBlank()) {
            override = System.getenv(ENV_HOME);
        }

        File root;
        if (override != null && !override.isBlank()) {
            root = new File(override);
        } else {
            root = deduceProjectRoot();
        }
        projectRoot = root;

        File cfgFile = new File(projectRoot, "config.properties");
        if (cfgFile.exists()) {
            try (FileInputStream in = new FileInputStream(cfgFile)) {
                props.load(in);
            } catch (IOException ignored) {
                // Config optionnelle : on continue avec les valeurs par défaut
            }
        }
    }

    private static File deduceProjectRoot() {
        try {
            File classesDir = new File(
                AppConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // classesDir pointe vers ".../build/classes" -> on remonte de 2 niveaux
            File root = classesDir.getParentFile() != null
                    ? classesDir.getParentFile().getParentFile()
                    : null;
            return (root != null) ? root : new File(".").getAbsoluteFile();
        } catch (Exception e) {
            return new File(".").getAbsoluteFile();
        }
    }

    public static File getProjectRoot() {
        return projectRoot;
    }

    public static File getUploadDir() {
        return resolve(props.getProperty("upload.dir", "uploads"));
    }

    public static File getResultsDir() {
        return resolve(props.getProperty("results.dir", "results"));
    }

    public static File getPythonScript() {
        return resolve(props.getProperty("python.script", "python/plagiarism_detector.py"));
    }

    public static String getPythonExecutable() {
        return props.getProperty("python.executable", "python");
    }

    // ✅ Identifiants de connexion à la base de données : plus de mot de passe en dur
    //    dans DBConnection.java. Priorité : variable d'environnement > config.properties
    //    > valeur par défaut (pratique pour un environnement de dev local).
    public static String getDbUrl() {
        return firstNonBlank(
            System.getenv("DB_URL"),
            props.getProperty("db.url"),
            "jdbc:mysql://localhost:3306/plagiarism_db"
        );
    }

    public static String getDbUser() {
        return firstNonBlank(
            System.getenv("DB_USER"),
            props.getProperty("db.user"),
            "root"
        );
    }

    public static String getDbPassword() {
        return firstNonBlank(
            System.getenv("DB_PASSWORD"),
            props.getProperty("db.password"),
            ""
        );
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static File resolve(String path) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(projectRoot, path);
    }
}
