package controller;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import util.AppConfig;
import util.DBConnection;

public class UploadServlet extends HttpServlet {

    // ✅ Doit rester cohérent avec les limites déclarées dans web.xml (multipart-config)
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 Mo

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("=== [UploadServlet] doPost() appelé ===");

        // ✅ Isolation par utilisateur : impossible d'uploader sans être connecté
        HttpSession session = request.getSession(false);
        Object userIdAttr = (session != null) ? session.getAttribute("userId") : null;
        System.out.println("[UploadServlet] session=" + session + " userIdAttr=" + userIdAttr);
        if (userIdAttr == null) {
            System.out.println("[UploadServlet] Pas d'userId en session -> redirection login.jsp");
            response.sendRedirect("login.jsp");
            return;
        }
        int userId = (Integer) userIdAttr;

        String tpName = request.getParameter("tp_name");
        System.out.println("[UploadServlet] tpName=" + tpName);

        Part filePart;
        try {
            filePart = request.getPart("file");
            System.out.println("[UploadServlet] filePart=" + filePart);
        } catch (IllegalStateException e) {
            // ✅ Levée par le conteneur quand le fichier dépasse max-file-size/max-request-size
            System.out.println("[UploadServlet] IllegalStateException (fichier trop volumineux) : " + e.getMessage());
            request.setAttribute("error", "Le fichier dépasse la taille maximale autorisée (50 Mo).");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        // ✅ getPart() peut renvoyer null si aucun fichier n'a été sélectionné/transmis
        //    (au lieu de lancer une exception) : on l'affiche comme une erreur claire, pas un 500.
        if (filePart == null || filePart.getSize() <= 0) {
            System.out.println("[UploadServlet] filePart null ou vide (size="
                + (filePart != null ? filePart.getSize() : "null") + ")");
            request.setAttribute("error", "Veuillez sélectionner un fichier ZIP avant de valider.");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        System.out.println("[UploadServlet] fileName=" + fileName + " size=" + filePart.getSize());

        // ✅ Validation : seul un .zip est accepté
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase().endsWith(".zip")) {
            request.setAttribute("error", "Veuillez uploader un fichier ZIP contenant des PDFs.");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        // ✅ Validation de la taille (sécurité applicative en plus de web.xml)
        if (filePart.getSize() > MAX_FILE_SIZE) {
            request.setAttribute("error", "Fichier trop volumineux (50 Mo max).");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        // ✅ Chemin portable : plus de "D:/Dev/...", résolu via AppConfig
        File uploadDir = AppConfig.getUploadDir();
        System.out.println("[UploadServlet] uploadDir=" + uploadDir.getAbsolutePath()
            + " exists=" + uploadDir.exists());
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        Connection conn;
        int submissionId;
        try {
            conn = DBConnection.getConnection();
            System.out.println("[UploadServlet] Connexion BDD obtenue: " + conn);

            // 1) On crée d'abord la soumission pour obtenir un identifiant unique
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO submissions(user_id, tp_name, file_path) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setString(2, tpName);
            ps.setString(3, "");
            int rows = ps.executeUpdate();
            System.out.println("[UploadServlet] INSERT submissions -> " + rows + " ligne(s) affectée(s)");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                submissionId = keys.getInt(1);
            }
            System.out.println("[UploadServlet] submissionId généré = " + submissionId);
        } catch (Exception e) {
            System.out.println("[UploadServlet] ERREUR lors de l'INSERT: " + e);
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de l'enregistrement de la soumission.");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        // 2) ✅ Un ZIP par soumission : nom de fichier unique basé sur l'id de soumission
        //    (isolation entre utilisateurs/soumissions, plus de collisions de noms)
        File destination = new File(uploadDir, "submission_" + submissionId + ".zip");
        filePart.write(destination.getAbsolutePath());
        filePart.delete(); // ✅ nettoyage du fichier temporaire du conteneur
        System.out.println("[UploadServlet] ZIP écrit -> " + destination.getAbsolutePath()
            + " exists=" + destination.exists() + " size=" + destination.length());

        // ✅ Vérification que le ZIP contient bien au moins un PDF
        boolean containsPdf = false;
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(destination)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName().toLowerCase();
                if (entryName.endsWith(".pdf")) {
                    containsPdf = true;
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("[UploadServlet] ZIP invalide: " + e);
            destination.delete();
            deleteSubmission(conn, submissionId);
            request.setAttribute("error", "Le fichier n'est pas une archive ZIP valide.");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }
        System.out.println("[UploadServlet] containsPdf=" + containsPdf);

        if (!containsPdf) {
            // ✅ Nettoyage : on ne garde pas de fichier/soumission invalide (évite l'accumulation inutile)
            destination.delete();
            deleteSubmission(conn, submissionId);
            request.setAttribute("error", "Le ZIP ne contient aucun fichier PDF.");
            request.getRequestDispatcher("upload.jsp").forward(request, response);
            return;
        }

        // 3) Mise à jour du chemin définitif en base
        try {
            PreparedStatement update = conn.prepareStatement(
                "UPDATE submissions SET file_path=? WHERE id=?");
            update.setString(1, destination.getAbsolutePath());
            update.setInt(2, submissionId);
            int rows = update.executeUpdate();
            System.out.println("[UploadServlet] UPDATE file_path -> " + rows + " ligne(s) affectée(s)");
        } catch (Exception e) {
            System.out.println("[UploadServlet] ERREUR lors de l'UPDATE file_path: " + e);
            e.printStackTrace();
        }

        System.out.println("[UploadServlet] Succès -> redirection dashboard.jsp");
        response.sendRedirect("dashboard.jsp");
    }

    private void deleteSubmission(Connection conn, int submissionId) {
        try {
            PreparedStatement del = conn.prepareStatement("DELETE FROM submissions WHERE id=?");
            del.setInt(1, submissionId);
            del.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
