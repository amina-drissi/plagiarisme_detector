package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import util.AppConfig;
import util.DBConnection;

public class AnalyseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ✅ Isolation par utilisateur : impossible de lancer une analyse sans être connecté
        HttpSession session = request.getSession(false);
        Object userIdAttr = (session != null) ? session.getAttribute("userId") : null;
        if (userIdAttr == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        int userId = (Integer) userIdAttr;

        // ✅ On analyse désormais UNE soumission précise, plus "tous les ZIP"
        String idParam = request.getParameter("idSubmission");
        Integer submissionId = parseId(idParam);
        if (submissionId == null) {
            request.setAttribute("error", "Aucune soumission valide sélectionnée.");
            request.getRequestDispatcher("results.jsp").forward(request, response);
            return;
        }

        try {
            Connection conn = DBConnection.getConnection();

            // ✅ Vérifie que la soumission appartient bien à l'utilisateur connecté
            PreparedStatement psSub = conn.prepareStatement(
                "SELECT file_path FROM submissions WHERE id=? AND user_id=?");
            psSub.setInt(1, submissionId);
            psSub.setInt(2, userId);
            ResultSet rsSub = psSub.executeQuery();

            if (!rsSub.next()) {
                request.setAttribute("error", "Soumission introuvable ou accès refusé.");
                request.getRequestDispatcher("results.jsp").forward(request, response);
                return;
            }

            String zipPath = rsSub.getString("file_path");

            // ✅ Résultat indépendant par soumission : results/result_<id>.json
            File resultsDir = AppConfig.getResultsDir();
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }
            File resultFile = new File(resultsDir, "result_" + submissionId + ".json");

            // 1. ✅ Lancer plagiarism_detector.py sur le ZIP sélectionné uniquement,
            //    en lui passant le chemin d'entrée ET le chemin de sortie
            ProcessBuilder pb = new ProcessBuilder(
                AppConfig.getPythonExecutable(),
                AppConfig.getPythonScript().getAbsolutePath(),
                zipPath,
                resultFile.getAbsolutePath()
            );
            pb.redirectErrorStream(false);
            Process p = pb.start();

            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));
            BufferedReader outputReader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = errorReader.readLine()) != null) {
                System.out.println("PYTHON ERROR: " + line);
            }
            while ((line = outputReader.readLine()) != null) {
                System.out.println("PYTHON OUTPUT: " + line);
            }

            p.waitFor();
            System.out.println("Python exit code: " + p.exitValue());

            // 2. Lire le JSON produit par Python pour CETTE soumission
            StringBuilder json = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(resultFile))) {
                String jsonLine;
                while ((jsonLine = br.readLine()) != null) {
                    json.append(jsonLine);
                }
            }

            // 3. Sauvegarder l'analyse en base, liée à la bonne soumission
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO analyses(submission_id) VALUES(?)");
            ps.setInt(1, submissionId);
            ps.executeUpdate();

            request.setAttribute("results", json.toString());
            request.setAttribute("idSubmission", submissionId);
            request.getRequestDispatcher("results.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de l'analyse : " + e.getMessage());
            request.getRequestDispatcher("results.jsp").forward(request, response);
        }
    }

    private Integer parseId(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return null;
        }
    }
}
