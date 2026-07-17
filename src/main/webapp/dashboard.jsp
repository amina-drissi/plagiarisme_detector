<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.sql.*" %>
<%@ page import="util.DBConnection" %>

<html>
<head>
    <title>Accueil</title>
    <link rel="stylesheet" href="style.css">
</head>

<body>

<%
    // ✅ Isolation par utilisateur : redirection si non connecté
    Object userIdAttr = session.getAttribute("userId");
    if (userIdAttr == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    int userId = (Integer) userIdAttr;
%>

<div class="navbar">
    <h2>Système de détection de plagiat</h2>
    <a href="LogoutServlet">Déconnecter</a>
</div>

<div class="container">

    <div class="card">
        <h3>📂 Soumettre TP</h3>
        <a class="btn" href="upload.jsp">Ouvrir</a>
    </div>

    <h3>📄 Mes soumissions</h3>

    <table>
        <tr>
            <th>Nom du TP</th>
            <th>Date</th>
            <th>Analyse</th>
            <th>Résultats</th>
        </tr>
        <%
            try {
                PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                    "SELECT id, tp_name, upload_date FROM submissions WHERE user_id=? ORDER BY upload_date DESC");
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int subId = rs.getInt("id");
        %>
        <tr>
            <td><%= rs.getString("tp_name") %></td>
            <td><%= rs.getTimestamp("upload_date") %></td>
            <td><a class="btn" href="AnalyseServlet?idSubmission=<%= subId %>">Analyser</a></td>
            <td><a class="btn" href="results.jsp?idSubmission=<%= subId %>">Résultats</a></td>
        </tr>
        <%
                }
                if (!any) {
        %>
        <tr><td colspan="4">Aucune soumission pour le moment.</td></tr>
        <%
                }
            } catch (Exception e) {
                e.printStackTrace();
        %>
        <tr><td colspan="4">Erreur lors du chargement des soumissions.</td></tr>
        <%
            }
        %>
    </table>

</div>

</body>
</html>
