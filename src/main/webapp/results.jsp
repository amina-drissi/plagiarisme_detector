<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.json.JSONObject,org.json.JSONArray" %>
<%@ page import="util.DBConnection,util.AppConfig" %>

<html>
<head>
    <title>Resultat d'analyse</title>
    <style>
        body {
            font-family: Arial;
            background-color: #f4f6f9;
            margin: 0;
            padding: 20px;
        }

        h2 {
            color: #2c3e50;
        }

        .container {
            width: 90%;
            margin: auto;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 40px;
            background: white;
        }

        th, td {
            padding: 12px;
            border: 1px solid #ddd;
            text-align: center;
        }

        th {
            background-color: #34495e;
            color: white;
        }

        .badge-high {
            background-color: #e74c3c;
            color: white;
            padding: 5px;
            border-radius: 5px;
        }

        .badge-medium {
            background-color: #f39c12;
            color: white;
            padding: 5px;
            border-radius: 5px;
        }

        .badge-low {
            background-color: #2ecc71;
            color: white;
            padding: 5px;
            border-radius: 5px;
        }
    </style>
    
</head>
<body>

<div class="container">

    <%
        // ✅ Isolation par utilisateur : on ne peut voir que ses propres résultats
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr == null) {
            response.sendRedirect("login.jsp");
            return;
        }
        int userId = (Integer) userIdAttr;

        String errorMsg = (String) request.getAttribute("error");
        String jsonStr  = (String) request.getAttribute("results");

        // ✅ Cas "bouton Résultats" : la page est appelée directement avec ?idSubmission=...
        //    (pas d'attribute "results" transmis par un forward d'AnalyseServlet)
        if (jsonStr == null && errorMsg == null) {
            String idParam = request.getParameter("idSubmission");
            if (idParam == null) {
                errorMsg = "Aucune soumission sélectionnée.";
            } else {
                try {
                    int submissionId = Integer.parseInt(idParam);

                    Connection conn = DBConnection.getConnection();
                    PreparedStatement psSub = conn.prepareStatement(
                        "SELECT id FROM submissions WHERE id=? AND user_id=?");
                    psSub.setInt(1, submissionId);
                    psSub.setInt(2, userId);
                    ResultSet rsSub = psSub.executeQuery();

                    if (!rsSub.next()) {
                        errorMsg = "Soumission introuvable ou accès refusé.";
                    } else {
                        File resultFile = new File(AppConfig.getResultsDir(), "result_" + submissionId + ".json");
                        if (!resultFile.exists()) {
                            errorMsg = "Aucune analyse n'a encore été lancée pour cette soumission. "
                                     + "Cliquez sur \"Analyser\" depuis le tableau de bord.";
                        } else {
                            StringBuilder sb = new StringBuilder();
                            try (BufferedReader br = new BufferedReader(new FileReader(resultFile))) {
                                String l;
                                while ((l = br.readLine()) != null) sb.append(l);
                            }
                            jsonStr = sb.toString();
                        }
                    }
                } catch (NumberFormatException nfe) {
                    errorMsg = "Identifiant de soumission invalide.";
                } catch (Exception ex) {
                    ex.printStackTrace();
                    errorMsg = "Erreur lors du chargement du résultat.";
                }
            }
        }

        if (errorMsg != null) {
    %>
        <div class="error"><%= errorMsg %></div>
    <% } %>

    <%
        JSONObject json = null;
        if (jsonStr != null) {
            json = new JSONObject(jsonStr);
        }
    %>




    <h2>📊 Similarité de code Java entre étudiants</h2>

    <table>
        <tr>
            <th>Étudiant 1</th>
            <th>Étudiant 2</th>
            <th>Similarité (%)</th>
            <th>Statut</th>
        </tr>

        <%
            if (json != null) {
                JSONArray codeArr = json.getJSONArray("student_code_results"); 
                for (int i = 0; i < codeArr.length(); i++) {
                    JSONObject obj = codeArr.getJSONObject(i);
                    String s1  = obj.getString("student1");
                    String s2  = obj.getString("student2");
                    double sim = obj.getDouble("similarity");

                    String status, badge;
                    if (sim >= 70)      { status = "Élevé";  badge = "badge-high"; }
                    else if (sim >= 40) { status = "Moyen";  badge = "badge-medium"; }
                    else                { status = "Faible"; badge = "badge-low"; }
        %>
        <tr>
            <td><%= s1 %></td>
            <td><%= s2 %></td>
            <td><%= sim %> %</td>
            <td><span class="<%= badge %>"><%= status %></span></td>
        </tr>
        <%
                }
                if (codeArr.length() == 0) {
        %>
        <tr><td colspan="4">Aucun code Java extrait des PDFs.</td></tr>
        <%
                }
            }
        %>
    </table>






    <h2>🌐 Similarité avec GitHub</h2>

    <table>
        <tr>
            <th>Étudiant</th>
            <th>Repository trouvé</th>
        </tr>

        <%
            if (json != null) {
                JSONArray ghArr = json.getJSONArray("github_results");
                for (int i = 0; i < ghArr.length(); i++) {
                    JSONObject obj    = ghArr.getJSONObject(i);
                    String student    = obj.getString("student");
                    String repo       = obj.getString("repository");
                    boolean noMatch   = repo.equals("No match") || repo.startsWith("N/A");
        %>
        <tr>
            <td><%= student %></td>
            <td>
                <% if (!noMatch) { %>
                    <a href="https://github.com/<%= repo %>" target="_blank"><%= repo %></a>
                <% } else { %>
                    <%= repo %>
                <% } %>
            </td>
        </tr>
        <%
                }
                if (ghArr.length() == 0) {
        %>
        <tr><td colspan="2">Aucun résultat GitHub.</td></tr>
        <%
                }
            }
        %>
    </table>






    <h2>📄 Plagiat textuel — Sources Internet</h2>

    <table>
        <tr>
            <th>Étudiant / Rapport</th>
            <th>Sources trouvées</th>
        </tr>

        <%
            if (json != null) {
                JSONArray textArr = json.getJSONArray("text_plagiarism_results");
                for (int i = 0; i < textArr.length(); i++) {
                    JSONObject obj   = textArr.getJSONObject(i);
                    String student   = obj.getString("student");
                    JSONArray sources = obj.optJSONArray("sources");
                    String note      = obj.optString("note", "");
        %>
        <tr>
            <td><%= student %></td>
            <td>
                <% if (note != null && !note.isEmpty()) { %>
                    <%= note %>
                <% } else if (sources == null || sources.length() == 0) { %>
                    Aucune source trouvée
                <% } else { %>
                    <ul>
                    <% for (int k = 0; k < sources.length(); k++) {
                           JSONObject src = sources.getJSONObject(k);
                           String title   = src.optString("title", "Source");
                           String link    = src.optString("link", "#");
                    %>
                        <li><a href="<%= link %>" target="_blank"><%= title %></a></li>
                    <% } %>
                    </ul>
                <% } %>
            </td>
        </tr>
        <%
                }
                if (textArr.length() == 0) {
        %>
        <tr><td colspan="2">Aucune analyse textuelle disponible.</td></tr>
        <%
                }
            }
        %>
    </table>

</div>

</body>
</html>