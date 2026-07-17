<%@ page contentType="text/html;charset=UTF-8" %>

<html>
<head>
    <title>Upload</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>

<div class="form-container">

    <h2>Upload TP</h2>

    <%
        String error = (String) request.getAttribute("error");
        if (error != null) {
    %>
        <div class="error-message"><%= error %></div>
    <%
        }
    %>

    <form action="UploadServlet" method="post" enctype="multipart/form-data">

        <input type="text" name="tp_name" placeholder="Nom du TP" required>

        <%-- ✅ accept=".zip" : guide l'étudiant à choisir un ZIP --%>
        <input type="file" name="file" accept=".zip" required>

        <p class="hint">Uploadez un fichier <strong>.zip</strong> contenant les rapports <strong>.pdf</strong> des étudiants</p><br><br>

        <button class="btn" type="submit">Lancer l'upload</button>

    </form>

</div>

</body>
</html>