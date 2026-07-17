<%@ page contentType="text/html;charset=UTF-8" %>

<html>
<body>
<link rel="stylesheet" href="style.css">
<div class="form-container">

    <h2>S'inscrire</h2>

    <%
        String error = (String) request.getAttribute("error");
        if (error != null) {
    %>
        <div class="error-message"><%= error %></div>
    <%
        }
        String prevName  = (String) request.getAttribute("name");
        String prevEmail = (String) request.getAttribute("email");
    %>

    <form action="register" method="post">

        <input type="text" name="name" placeholder="Nom complet"
               value="<%= prevName != null ? prevName : "" %>">

        <input type="email" name="email" placeholder="Email"
               value="<%= prevEmail != null ? prevEmail : "" %>">

        <input type="password" name="password"
               placeholder="Mot de passe (8+ car., Maj, min, chiffre, caractère spécial)">

        <button class="btn" type="submit">Inscription</button>

    </form>

</div>

</body>
</html>
