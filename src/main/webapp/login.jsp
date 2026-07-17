<%@ page contentType="text/html;charset=UTF-8" %>

<html>
<head>
    <title>Se connecter</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<div class="form-container">

    <h2>Se connecter</h2>

    <% if ("1".equals(request.getParameter("error"))) { %>
        <div class="error-message">Email ou mot de passe incorrect.</div>
    <% } %>

    <form action="login" method="post">

        <input type="email" name="email" placeholder="Email">

        <input type="password" name="password" placeholder="Mot de passe">

        <button class="btn" type="submit">Connection</button>

    </form>

</div>
</body>
</html>